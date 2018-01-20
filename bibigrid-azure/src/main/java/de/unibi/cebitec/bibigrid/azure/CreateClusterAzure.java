package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * Implementation of the general CreateCluster interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class CreateClusterAzure extends CreateCluster {
    private static final Logger LOG = LoggerFactory.getLogger(CreateClusterAzure.class);
    private final ConfigurationAzure config;
    private Azure compute;
    private String masterStartupScript;
    private CreateClusterEnvironmentAzure environment;

    CreateClusterAzure(final ConfigurationAzure config, final ProviderModule providerModule) {
        super(config, providerModule);
        this.config = config;
    }

    @Override
    public CreateClusterEnvironmentAzure createClusterEnvironment() throws ConfigurationException {
        compute = AzureUtils.getComputeService(config);
        return environment = new CreateClusterEnvironmentAzure(this);
    }

    @Override
    public CreateClusterAzure configureClusterMasterInstance() {
        // preparing block device mappings for master
        // Map<String, String> masterSnapshotToMountPointMap = config.getMasterMounts();
        // int ephemerals = config.getMasterInstanceType().getSpec().getEphemerals();
        // DeviceMapper masterDeviceMapper = new DeviceMapper(providerModule, masterSnapshotToMountPointMap, ephemerals);

        masterStartupScript = ShellScriptCreator.getMasterUserData(config, environment.getKeypair(), true);
        return this;
    }

    @Override
    public CreateClusterAzure configureClusterSlaveInstance() {
        //now defining Slave Volumes
        // Map<String, String> snapShotToSlaveMounts = config.getSlaveMounts();
        // int ephemerals = config.getSlaveInstanceType().getSpec().getEphemerals();
        // slaveDeviceMapper = new DeviceMapper(providerModule, snapShotToSlaveMounts, ephemerals);
        return this;
    }

    @Override
    public boolean launchClusterInstances() {
        VirtualMachine masterInstance = launchClusterMasterInstance();
        List<VirtualMachine> slaveInstances = launchClusterSlaveInstances();
        // just to be sure, everything is present, wait 5 seconds
        sleep(5);
        // post configure master
        List<String> slaveIps = new ArrayList<>();
        List<String> slaveHostnames = new ArrayList<>();
        if (slaveInstances != null) {
            for (VirtualMachine slave : slaveInstances) {
                slaveIps.add(slave.getPrimaryPublicIPAddress().ipAddress());
                slaveHostnames.add(slave.getPrimaryPublicIPAddress().fqdn());
            }
        }
        String masterPublicIp = masterInstance.getPrimaryPublicIPAddress().ipAddress();
        configureMaster(masterInstance.getPrimaryNetworkInterface().primaryPrivateIP(), masterPublicIp,
                masterInstance.getPrimaryPublicIPAddress().fqdn(), slaveIps, slaveHostnames,
                environment.getSubnetCidr());
        logFinishedInfoMessage(masterPublicIp);
        saveGridPropertiesFile(masterPublicIp);
        return true;
    }

    private VirtualMachine launchClusterMasterInstance() {
        LOG.info("Requesting master instance...");
        String masterInstanceName = PREFIX + "master-" + clusterId;
        //compute.publicIPAddresses().define("").withRegion("").withExistingResourceGroup("").withStaticIP().
        VirtualMachine masterInstance = compute.virtualMachines()
                .define(masterInstanceName)
                .withRegion(config.getRegion())
                .withExistingResourceGroup(environment.getResourceGroup())
                .withExistingPrimaryNetwork(environment.getVpc())
                .withSubnet(environment.getSubnet().name())
                .withPrimaryPrivateIPAddressDynamic()
                .withNewPrimaryPublicIPAddress(environment.getMasterIP())
                .withSpecificLinuxImageVersion(AzureUtils.getImage(compute, config, config.getMasterImage()))
                .withRootUsername(config.getUser())
                .withSsh("") // TODO
                .withCustomData(masterStartupScript)
                .withNewDataDisk(50)
                .withSize(config.getMasterInstanceType().getValue())
                .withTag("bibigrid-id", clusterId)
                .withTag("user", config.getUser())
                .withTag("name", masterInstanceName)
                .withTag("creation", "" + new Date().getTime())
                .create();
        // TODO .attachDisks(config.getMasterMounts())
        // TODO .setInstanceSchedulingOptions(config.isUseSpotInstances())
        // Waiting for master instance to run
        LOG.info("Waiting for master instance to finish booting...");
        // TODO
        LOG.info(I, "Master instance is now running!");
        waitForInstancesStatusCheck(Collections.singletonList(masterInstance));
        return masterInstance;
    }

    private List<VirtualMachine> launchClusterSlaveInstances() {
        List<VirtualMachine> slaveInstances = new ArrayList<>();
        int instanceCount = config.getSlaveInstanceCount();
        if (instanceCount > 0) {
            LOG.info("Requesting slave instances...");
            String base64SlaveUserData = ShellScriptCreator.getSlaveUserData(config, environment.getKeypair(), true);
            String slaveInstanceNameTag = PREFIX + "slave-" + clusterId;
            for (int i = 0; i < instanceCount; i++) {
                String slaveInstanceId = PREFIX + "slave" + i + "-" + clusterId;
                VirtualMachine slaveInstance = compute.virtualMachines()
                        .define(slaveInstanceId)
                        .withRegion(config.getRegion())
                        .withExistingResourceGroup(environment.getResourceGroup())
                        .withExistingPrimaryNetwork(environment.getVpc())
                        .withSubnet(environment.getSubnet().name())
                        .withPrimaryPrivateIPAddressDynamic()
                        .withNewPrimaryPublicIPAddress(environment.getMasterIP()) // TODO
                        .withSpecificLinuxImageVersion(AzureUtils.getImage(compute, config, config.getSlaveImage()))
                        .withRootUsername(config.getUser())
                        .withSsh("") // TODO
                        .withCustomData(base64SlaveUserData)
                        .withNewDataDisk(50)
                        .withSize(config.getSlaveInstanceType().getValue())
                        .withTag("bibigrid-id", clusterId)
                        .withTag("user", config.getUser())
                        .withTag("name", slaveInstanceNameTag)
                        .withTag("creation", "" + new Date().getTime())
                        .create();
                // TODO .attachDisks(config.getSlaveMounts())
                // TODO .setInstanceSchedulingOptions(config.isUseSpotInstances())
                slaveInstances.add(slaveInstance);
            }
            LOG.info("Waiting for slave instance(s) to finish booting...");
            // TODO
            LOG.info(I, "Slave instance(s) is now running!");
            waitForInstancesStatusCheck(slaveInstances);
        } else {
            LOG.info("No Slave instance(s) requested!");
        }
        return slaveInstances;
    }

    private void waitForInstancesStatusCheck(List<VirtualMachine> instances) {
        LOG.info("Waiting for Status Checks on instances...");
        for (VirtualMachine instance : instances) {
            do {
                PowerState status = instance.powerState();
                LOG.info(V, "Status of " + instance.computerName() + " instance: " + status);
                if (status == PowerState.RUNNING) {
                    break;
                } else {
                    LOG.info(V, "...");
                    sleep(10);
                }
            } while (true);
        }
        LOG.info(I, "Status checks successful.");
    }

    ConfigurationAzure getConfig() {
        return config;
    }

    Azure getCompute() {
        return compute;
    }
}
package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * Implementation of the general CreateCluster interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class CreateClusterAzure extends CreateCluster {
    private static final Logger LOG = LoggerFactory.getLogger(CreateClusterAzure.class);
    private final Azure compute;
    private String masterStartupScript;

    CreateClusterAzure(final ProviderModule providerModule, Client client, final Configuration config) {
        super(providerModule, client, config);
        compute = ((ClientAzure) client).getInternal();
    }

    @Override
    public CreateCluster configureClusterMasterInstance() {
        masterStartupScript = ShellScriptCreator.getUserData(config, true);
        return super.configureClusterMasterInstance();
    }

    @Override
    protected List<Configuration.MountPoint> resolveMountSources(List<Configuration.MountPoint> mountPoints) {
        // TODO: possibly check if snapshot or volume like openstack
        return mountPoints;
    }

    @Override
    protected InstanceAzure launchClusterMasterInstance(String masterNameTag) {
        LOG.info("Requesting master instance ...");
        //compute.publicIPAddresses().define("").withRegion("").withExistingResourceGroup("").withStaticIP().
        InstanceImageAzure image = (InstanceImageAzure) client.getImageById(config.getMasterInstance().getImage());
        VirtualMachine masterInstance = setMasterPublicIpMode(compute.virtualMachines()
                .define(masterNameTag)
                .withRegion(config.getRegion())
                .withExistingResourceGroup(((CreateClusterEnvironmentAzure) environment).getResourceGroup())
                .withExistingPrimaryNetwork(((NetworkAzure) environment.getNetwork()).getInternal())
                .withSubnet(environment.getSubnet().getName())
                .withPrimaryPrivateIPAddressDynamic(), masterNameTag)
                .withSpecificLinuxImageVersion(image.getInternal())
                .withRootUsername(config.getSshUser())
                .withSsh("") // TODO
                .withCustomData(masterStartupScript)
                .withNewDataDisk(50)
                .withSize(config.getMasterInstance().getProviderType().getValue())
                .withTag(Instance.TAG_BIBIGRID_ID, clusterId)
                .withTag(Instance.TAG_USER, config.getUser())
                .withTag(Instance.TAG_NAME, masterNameTag)
                .withTag(InstanceAzure.TAG_CREATION, getCurrentTime())
                .create();
        // TODO .attachDisks(config.getMasterMounts())
        // TODO .setInstanceSchedulingOptions(config.isUseSpotInstances())
        // Waiting for master instance to run
        LOG.info("Waiting for master instance to finish booting ...");
        waitForInstancesStatusCheck(Collections.singletonList(masterInstance));
        LOG.info(I, "Master instance is now running!");
        return new InstanceAzure(config.getMasterInstance(), masterInstance);
    }

    private VirtualMachine.DefinitionStages.WithOS setMasterPublicIpMode(
            VirtualMachine.DefinitionStages.WithPublicIPAddress builder, String masterNameTag) {
        return config.isUseMasterWithPublicIp() ? builder.withNewPrimaryPublicIPAddress(masterNameTag) :
                builder.withoutPrimaryPublicIPAddress();
    }

    private String getCurrentTime() {
        return ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    protected List<Instance> launchClusterWorkerInstances(
            int batchIndex, Configuration.WorkerInstanceConfiguration instanceConfiguration, String workerNameTag) {
        List<VirtualMachine> workerInstances = new ArrayList<>();
        String base64WorkerUserData = ShellScriptCreator.getUserData(config, true);
        InstanceImageAzure image = (InstanceImageAzure) client.getImageById(instanceConfiguration.getImage());
        for (int i = 0; i < instanceConfiguration.getCount(); i++) {
            VirtualMachine workerInstance = compute.virtualMachines()
                    .define(buildWorkerInstanceName(batchIndex, i))
                    .withRegion(config.getRegion())
                    .withExistingResourceGroup(((CreateClusterEnvironmentAzure) environment).getResourceGroup())
                    .withExistingPrimaryNetwork(((NetworkAzure) environment.getNetwork()).getInternal())
                    .withSubnet(environment.getSubnet().getName())
                    .withPrimaryPrivateIPAddressDynamic()
                    .withoutPrimaryPublicIPAddress()
                    .withSpecificLinuxImageVersion(image.getInternal())
                    .withRootUsername(config.getSshUser())
                    .withSsh("") // TODO
                    .withCustomData(base64WorkerUserData)
                    .withNewDataDisk(50)
                    .withSize(instanceConfiguration.getProviderType().getValue())
                    .withTag(Instance.TAG_BIBIGRID_ID, clusterId)
                    .withTag(Instance.TAG_USER, config.getUser())
                    .withTag(Instance.TAG_NAME, workerNameTag)
                    .withTag(InstanceAzure.TAG_CREATION, getCurrentTime())
                    .create();
            // TODO .setInstanceSchedulingOptions(config.isUseSpotInstances())
            workerInstances.add(workerInstance);
        }
        LOG.info("Waiting for worker instance(s) to finish booting ...");
        waitForInstancesStatusCheck(workerInstances);
        LOG.info(I, "Worker instance(s) is now running!");
        return workerInstances.stream().map(i -> new InstanceAzure(instanceConfiguration, i)).collect(Collectors.toList());
    }

    private void waitForInstancesStatusCheck(List<VirtualMachine> instances) {
        LOG.info("Waiting for Status Checks on instances ...");
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
}
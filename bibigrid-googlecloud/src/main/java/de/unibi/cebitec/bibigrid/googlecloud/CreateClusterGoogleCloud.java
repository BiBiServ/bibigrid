package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.cloud.compute.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * Implementation of the general CreateCluster interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class CreateClusterGoogleCloud extends CreateCluster {
    private static final Logger LOG = LoggerFactory.getLogger(CreateClusterGoogleCloud.class);
    private final ConfigurationGoogleCloud config;
    private Compute compute;
    private Metadata masterStartupScript;
    private NetworkInterface masterNetworkInterface;
    private CreateClusterEnvironmentGoogleCloud environment;
    private String bibigridId;

    CreateClusterGoogleCloud(final ConfigurationGoogleCloud config, final ProviderModule providerModule) {
        super(config, providerModule);
        this.config = config;
    }

    @Override
    public CreateClusterEnvironmentGoogleCloud createClusterEnvironment() throws ConfigurationException {
        compute = GoogleCloudUtils.getComputeService(config);
        bibigridId = "bibigrid-id" + GoogleCloudUtils.TAG_SEPARATOR + clusterId;
        // username = "user" + GoogleCloudUtils.TAG_SEPARATOR + config.getUser();
        return environment = new CreateClusterEnvironmentGoogleCloud(this);
    }

    @Override
    public CreateClusterGoogleCloud configureClusterMasterInstance() {
        // preparing block device mappings for master
        // Map<String, String> masterSnapshotToMountPointMap = config.getMasterMounts();
        // int ephemerals = config.getMasterInstanceType().getSpec().getEphemerals();
        // DeviceMapper masterDeviceMapper = new DeviceMapper(providerModule, masterSnapshotToMountPointMap, ephemerals);

        String startupScript = ShellScriptCreator.getMasterUserData(config, environment.getKeypair(), false);
        masterStartupScript = Metadata.newBuilder().add("startup-script", startupScript).build();
        buildMasterNetworkInterface();
        return this;
    }

    private void buildMasterNetworkInterface() {
        // create NetworkInterfaceSpecification for MASTER instance with FIXED internal IP and public ip
        NetworkInterface.Builder networkInterfaceBuilder = buildExternalNetworkInterface();
        // Currently only accessible through reflection. Should be public according to docs...
        try {
            Method m = NetworkInterface.Builder.class.getDeclaredMethod("setNetworkIp", String.class);
            m.setAccessible(true);
            m.invoke(networkInterfaceBuilder, environment.getMasterIP());
        } catch (Exception e) {
            LOG.error("Failed to assign fixed IP to master instance! {}", e.getMessage());
        }
        masterNetworkInterface = networkInterfaceBuilder.build();
    }

    private NetworkInterface.Builder buildExternalNetworkInterface() {
        NetworkInterface.AccessConfig accessConfig = NetworkInterface.AccessConfig.newBuilder()
                .setType(NetworkInterface.AccessConfig.Type.ONE_TO_ONE_NAT)
                .setName("external-nat")
                .build();
        return NetworkInterface.newBuilder(environment.getSubnet().getNetwork())
                .setSubnetwork(environment.getSubnet().getSubnetworkId())
                .setAccessConfigurations(accessConfig);
    }

    @Override
    public CreateClusterGoogleCloud configureClusterSlaveInstance() {
        //now defining Slave Volumes
        // Map<String, String> snapShotToSlaveMounts = config.getSlaveMounts();
        // int ephemerals = config.getSlaveInstanceType().getSpec().getEphemerals();
        // slaveDeviceMapper = new DeviceMapper(providerModule, snapShotToSlaveMounts, ephemerals);
        return this;
    }

    @Override
    public boolean launchClusterInstances() {
        Instance masterInstance = launchClusterMasterInstance();
        List<Instance> slaveInstances = launchClusterSlaveInstances();
        // just to be sure, everything is present, wait 5 seconds
        sleep(5);
        // post configure master
        List<String> slaveIps = new ArrayList<>();
        List<String> slaveHostnames = new ArrayList<>();
        if (slaveInstances != null) {
            for (Instance slave : slaveInstances) {
                slaveIps.add(GoogleCloudUtils.getInstancePrivateIp(slave));
                slaveHostnames.add(GoogleCloudUtils.getInstanceFQDN(slave));
            }
        }
        String masterPublicIp = GoogleCloudUtils.getInstancePublicIp(masterInstance);
        configureMaster(GoogleCloudUtils.getInstancePrivateIp(masterInstance), masterPublicIp,
                GoogleCloudUtils.getInstanceFQDN(masterInstance), slaveIps, slaveHostnames, environment.getSubnetCidr());
        logFinishedInfoMessage(masterPublicIp);
        saveGridPropertiesFile(masterPublicIp);
        return true;
    }

    private Instance launchClusterMasterInstance() {
        LOG.info("Requesting master instance...");
        String zone = config.getAvailabilityZone();
        String masterInstanceName = PREFIX + "master-" + clusterId;
        InstanceInfo.Builder masterBuilder = GoogleCloudUtils.getInstanceBuilder(zone, masterInstanceName,
                config.getMasterInstanceType().getValue())
                .setNetworkInterfaces(masterNetworkInterface)
                .setTags(Tags.of(bibigridId, "name" + GoogleCloudUtils.TAG_SEPARATOR + masterInstanceName))
                .setMetadata(masterStartupScript);
        GoogleCloudUtils.attachDisks(compute, masterBuilder, config.getMasterImage(), zone, config.getMasterMounts(),
                clusterId, config.getGoogleProjectId());
        GoogleCloudUtils.setInstanceSchedulingOptions(masterBuilder, config.isUseSpotInstances());
        // Waiting for master instance to run
        LOG.info("Waiting for master instance to finish booting...");
        Operation createMasterOperation = compute.create(masterBuilder.build());
        Instance masterInstance = waitForInstances(new InstanceId[]{InstanceId.of(zone, masterInstanceName)},
                new Operation[]{createMasterOperation}).get(0);
        LOG.info(I, "Master instance is now running!");
        waitForInstancesStatusCheck(Collections.singletonList(masterInstance));
        return masterInstance;
    }

    private List<Instance> launchClusterSlaveInstances() {
        List<Instance> slaveInstances = null;
        int instanceCount = config.getSlaveInstanceCount();
        if (instanceCount > 0) {
            LOG.info("Requesting slave instances...");
            String zone = config.getAvailabilityZone();
            String base64SlaveUserData = ShellScriptCreator.getSlaveUserData(config, environment.getKeypair(), false);
            String slaveInstanceNameTag = "name" + GoogleCloudUtils.TAG_SEPARATOR + PREFIX + "slave-" + clusterId;
            InstanceId[] slaveInstanceIds = new InstanceId[instanceCount];
            Operation[] slaveInstanceOperations = new Operation[instanceCount];
            for (int i = 0; i < instanceCount; i++) {
                String slaveInstanceId = PREFIX + "slave" + i + "-" + clusterId;
                InstanceInfo.Builder slaveBuilder = GoogleCloudUtils.getInstanceBuilder(zone, slaveInstanceId,
                        config.getSlaveInstanceType().getValue())
                        .setNetworkInterfaces(buildExternalNetworkInterface().build())
                        .setTags(Tags.of(bibigridId, slaveInstanceNameTag))
                        .setMetadata(Metadata.newBuilder().add("startup-script", base64SlaveUserData).build());
                GoogleCloudUtils.attachDisks(compute, slaveBuilder, config.getSlaveImage(), zone,
                        config.getSlaveMounts(), clusterId, config.getGoogleProjectId());
                GoogleCloudUtils.setInstanceSchedulingOptions(slaveBuilder, config.isUseSpotInstances());
                slaveInstanceOperations[i] = compute.create(slaveBuilder.build());
                slaveInstanceIds[i] = InstanceId.of(zone, slaveInstanceId);
            }
            LOG.info("Waiting for slave instance(s) to finish booting...");
            slaveInstances = waitForInstances(slaveInstanceIds, slaveInstanceOperations);
            LOG.info(I, "Slave instance(s) is now running!");
            waitForInstancesStatusCheck(slaveInstances);
        } else {
            LOG.info("No Slave instance(s) requested!");
        }
        return slaveInstances;
    }

    private void waitForInstancesStatusCheck(List<Instance> instances) {
        LOG.info("Waiting for Status Checks on instances...");
        for (Instance instance : instances) {
            do {
                InstanceInfo.Status status = instance.getStatus();
                LOG.info(V, "Status of " + instance.getInstanceId().getInstance() + " instance: " + status);
                if (status == InstanceInfo.Status.RUNNING) {
                    break;
                } else {
                    LOG.info(V, "...");
                    sleep(10);
                }
            } while (true);
        }
        LOG.info(I, "Status checks successful.");
    }

    private List<Instance> waitForInstances(InstanceId[] instanceIds, Operation[] operations) {
        if (instanceIds.length == 0 || operations.length == 0 || instanceIds.length != operations.length) {
            LOG.error("No instances found");
            return new ArrayList<>();
        }
        List<Instance> returnList = new ArrayList<>();
        for (int i = 0; i < instanceIds.length; i++) {
            Operation operation = operations[i];
            while (!operation.isDone()) {
                LOG.info(V, "...");
                sleep(1);
            }
            operation = operation.reload();
            if (operation.getErrors() == null) {
                returnList.add(compute.getInstance(instanceIds[i]));
            } else {
                LOG.error("Creation of instance failed: {}", operation.getErrors());
                break;
            }
        }
        return returnList;
    }

    ConfigurationGoogleCloud getConfig() {
        return config;
    }

    Compute getCompute() {
        return compute;
    }
}
package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.cloud.compute.*;
import org.apache.commons.codec.binary.Base64;
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
    static final String SUBNET_PREFIX = PREFIX + "subnet-";
    static final String SECURITY_GROUP_PREFIX = PREFIX + "sg-";
    private final ConfigurationGoogleCloud config;

    private Compute compute;
    private String base64MasterUserData;
    private List<NetworkInterface> masterNetworkInterfaces, slaveNetworkInterfaces;
    private CreateClusterEnvironmentGoogleCloud environment;
    private String bibigridId, username;
    private DeviceMapper slaveDeviceMapper;

    CreateClusterGoogleCloud(final ConfigurationGoogleCloud config, final ProviderModule providerModule) {
        super(providerModule);
        this.config = config;
    }

    public CreateClusterEnvironmentGoogleCloud createClusterEnvironment() {
        compute = GoogleCloudUtils.getComputeService(config);

        clusterId = generateClusterId();
        config.setClusterId(clusterId);
        bibigridId = "bibigrid-id" + GoogleCloudUtils.TAG_SEPARATOR + clusterId;
        username = "user" + GoogleCloudUtils.TAG_SEPARATOR + config.getUser();

        return environment = new CreateClusterEnvironmentGoogleCloud(this);
    }

    public CreateClusterGoogleCloud configureClusterMasterInstance() {
        // done for master. More volume description later when master is running

        // preparing block device mappings for master
        Map<String, String> masterSnapshotToMountPointMap = config.getMasterMounts();
        int ephemerals = config.getMasterInstanceType().getSpec().getEphemerals();
        DeviceMapper masterDeviceMapper = new DeviceMapper(providerModule, masterSnapshotToMountPointMap, ephemerals);

        base64MasterUserData = UserDataCreator.masterUserData(masterDeviceMapper, config, environment.getKeypair());
        // Google doesn't use base64 encoded startup scripts. Just use plain text
        base64MasterUserData = new String(Base64.decodeBase64(base64MasterUserData));

        LOG.info(V, "Master UserData:\n {}", base64MasterUserData);

        // create NetworkInterfaceSpecification for MASTER instance with FIXED internal IP and public ip
        masterNetworkInterfaces = new ArrayList<>();

        NetworkInterface.Builder inis = NetworkInterface.newBuilder(environment.getSubnet().getNetwork());
        inis.setSubnetwork(environment.getSubnet().getSubnetworkId())
                .setAccessConfigurations(NetworkInterface.AccessConfig.newBuilder()
                        .setType(NetworkInterface.AccessConfig.Type.ONE_TO_ONE_NAT)
                        .setName("external-nat")
                        .build());
        // Currently only accessible through reflection. Should be public according to docs...
        try {
            Method m = NetworkInterface.Builder.class.getDeclaredMethod("setNetworkIp", String.class);
            m.setAccessible(true);
            m.invoke(inis, environment.getMasterIP());
        } catch (Exception e) {
            e.printStackTrace();
        }

        masterNetworkInterfaces.add(inis.build()); // add eth0

        slaveNetworkInterfaces = new ArrayList<>();
        inis = NetworkInterface.newBuilder(environment.getSubnet().getNetwork());
        inis.setSubnetwork(environment.getSubnet().getSubnetworkId())
                .setAccessConfigurations(NetworkInterface.AccessConfig.newBuilder()
                        .setType(NetworkInterface.AccessConfig.Type.ONE_TO_ONE_NAT)
                        .setName("external-nat")
                        .build());
        for (int i = 0; i < config.getSlaveInstanceCount(); i++) {
            slaveNetworkInterfaces.add(inis.build());
        }

        return this;
    }

    public CreateClusterGoogleCloud configureClusterSlaveInstance() {
        //now defining Slave Volumes
        Map<String, String> snapShotToSlaveMounts = config.getSlaveMounts();
        int ephemerals = config.getSlaveInstanceType().getSpec().getEphemerals();
        slaveDeviceMapper = new DeviceMapper(providerModule, snapShotToSlaveMounts, ephemerals);
        return this;
    }

    public boolean launchClusterInstances() {
        LOG.info("Requesting master instance ...");

        String zone = config.getAvailabilityZone();
        String masterInstanceName = PREFIX + "master-" + clusterId;
        InstanceInfo.Builder masterBuilder = GoogleCloudUtils.getInstanceBuilder(zone, masterInstanceName,
                config.getMasterInstanceType().getValue())
                .setNetworkInterfaces(masterNetworkInterfaces)
                .setTags(Tags.of(bibigridId, username, "name" + GoogleCloudUtils.TAG_SEPARATOR + masterInstanceName))
                .setMetadata(Metadata.newBuilder().add("startup-script", base64MasterUserData).build());
        GoogleCloudUtils.attachDisks(compute, masterBuilder, config.getMasterImage(), zone, config.getMasterMounts(), clusterId);
        GoogleCloudUtils.setInstanceSchedulingOptions(masterBuilder, config.isUseSpotInstances());

        // Waiting for master instance to run
        LOG.info("Waiting for master instance to finish booting ...");
        Operation createMasterOperation = compute.create(masterBuilder.build());
        Instance masterInstance = waitForInstances(Collections.singletonList(InstanceId.of(zone, masterInstanceName)),
                Collections.singletonList(createMasterOperation)).get(0);
        LOG.info(I, "Master instance is now running!");

        waitForInstancesStatusCheck(Collections.singletonList(masterInstance));

        String masterPrivateIp = GoogleCloudUtils.getInstancePrivateIp(masterInstance);
        String masterPublicIp = GoogleCloudUtils.getInstancePublicIp(masterInstance);
        String masterDnsName = GoogleCloudUtils.getInstanceFQDN(masterInstance);

        // run slave instances and supply userdata
        List<Instance> slaveInstances = null;
        if (config.getSlaveInstanceCount() > 0) {
            String base64SlaveUserData = UserDataCreator.forSlave(masterPrivateIp, masterDnsName, slaveDeviceMapper,
                    config, environment.getKeypair());
            // Google doesn't use base64 encoded startup scripts. Just use plain text
            base64SlaveUserData = new String(Base64.decodeBase64(base64SlaveUserData));
            LOG.info(V, "Slave Userdata:\n{}", base64SlaveUserData);

            List<InstanceId> slaveInstanceIds = new ArrayList<>();
            List<Operation> slaveInstanceOperations = new ArrayList<>();
            for (int i = 0; i < config.getSlaveInstanceCount(); i++) {
                String slaveInstanceName = PREFIX + "slave-" + clusterId;
                String slaveInstanceId = PREFIX + "slave" + i + "-" + clusterId;
                InstanceInfo.Builder slaveBuilder = GoogleCloudUtils.getInstanceBuilder(zone, slaveInstanceId,
                        config.getSlaveInstanceType().getValue())
                        .setNetworkInterfaces(slaveNetworkInterfaces.get(i))
                        .setTags(Tags.of(bibigridId, username, "name" + GoogleCloudUtils.TAG_SEPARATOR + slaveInstanceName))
                        .setMetadata(Metadata.newBuilder().add("startup-script", base64SlaveUserData).build());
                GoogleCloudUtils.attachDisks(compute, slaveBuilder, config.getSlaveImage(), zone, config.getSlaveMounts(), clusterId);
                GoogleCloudUtils.setInstanceSchedulingOptions(masterBuilder, config.isUseSpotInstances());

                Operation createSlaveOperation = compute.create(slaveBuilder.build());
                slaveInstanceIds.add(InstanceId.of(zone, slaveInstanceId));
                slaveInstanceOperations.add(createSlaveOperation);
            }
            LOG.info("Waiting for slave instance(s) to finish booting ...");
            slaveInstances = waitForInstances(slaveInstanceIds, slaveInstanceOperations);
            LOG.info(I, "Slave instance(s) is now running!");

            waitForInstancesStatusCheck(slaveInstances);
        } else {
            LOG.info("No Slave instance(s) requested!");
        }

        // just to be sure, everything is present, wait 5 seconds
        sleep(5);

        // post configure master
        List<String> slaveIps = new ArrayList<>();
        if (slaveInstances != null) {
            for (com.google.cloud.compute.Instance slave : slaveInstances) {
                slaveIps.add(GoogleCloudUtils.getInstancePrivateIp(slave));
            }
        }
        configureMaster(GoogleCloudUtils.getInstancePrivateIp(masterInstance),
                GoogleCloudUtils.getInstancePublicIp(masterInstance), slaveIps, config);

        logFinishedInfoMessage(masterPublicIp, config, clusterId);
        saveGridPropertiesFile(masterPublicIp, config, clusterId);
        return true;
    }

    private void waitForInstancesStatusCheck(List<Instance> instances) {
        LOG.info("Waiting for Status Checks on instances ...");
        for (Instance instance : instances) {
            do {
                InstanceInfo.Status status = instance.getStatus();
                LOG.debug("Status of " + instance.getInstanceId().getInstance() + " instance: " + status);
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

    private List<Instance> waitForInstances(List<InstanceId> instanceIds, List<Operation> operations) {
        if (instanceIds.isEmpty() || operations.isEmpty() || instanceIds.size() != operations.size()) {
            LOG.error("No instances found");
            return new ArrayList<>();
        }
        List<Instance> returnList = new ArrayList<>();
        for (int i = 0; i < instanceIds.size(); i++) {
            Operation operation = operations.get(i);
            while (!operation.isDone()) {
                LOG.info(V, "...");
                sleep(1);
            }
            operation = operation.reload();
            if (operation.getErrors() == null) {
                returnList.add(compute.getInstance(instanceIds.get(i)));
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
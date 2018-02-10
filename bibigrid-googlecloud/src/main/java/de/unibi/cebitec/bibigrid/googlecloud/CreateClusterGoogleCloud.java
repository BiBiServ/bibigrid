package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import com.google.api.services.compute.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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
    private Metadata.Items masterStartupScript;
    private Metadata.Items slaveStartupScript;
    private NetworkInterface masterNetworkInterface;
    private CreateClusterEnvironmentGoogleCloud environment;

    CreateClusterGoogleCloud(final ConfigurationGoogleCloud config, final ProviderModule providerModule) {
        super(config, providerModule);
        this.config = config;
    }

    @Override
    public CreateClusterEnvironmentGoogleCloud createClusterEnvironment() throws ConfigurationException {
        compute = GoogleCloudUtils.getComputeService(config);
        return environment = new CreateClusterEnvironmentGoogleCloud(this);
    }

    @Override
    public CreateClusterGoogleCloud configureClusterMasterInstance() {
        // preparing block device mappings for master
        // Map<String, String> masterSnapshotToMountPointMap = config.getMasterMounts();
        // int ephemerals = config.getMasterInstanceType().getSpec().getEphemerals();
        // DeviceMapper masterDeviceMapper = new DeviceMapper(providerModule, masterSnapshotToMountPointMap, ephemerals);

        String startupScript = ShellScriptCreator.getUserData(config, environment.getKeypair(), false, true);
        masterStartupScript = new Metadata.Items().setKey("startup-script").setValue(startupScript);
        buildMasterNetworkInterface();
        return this;
    }

    private void buildMasterNetworkInterface() {
        // create NetworkInterfaceSpecification for MASTER instance with FIXED internal IP and public ip
        masterNetworkInterface = buildExternalNetworkInterface();
        masterNetworkInterface.setNetworkIP(environment.getMasterIP());
    }

    private NetworkInterface buildExternalNetworkInterface() {
        AccessConfig accessConfig = new AccessConfig().setType("ONE_TO_ONE_NAT").setName("External NAT");
        return new NetworkInterface().setNetwork(environment.getSubnet().getNetwork())
                .setSubnetwork(environment.getSubnet().getSelfLink())
                .setAccessConfigs(Collections.singletonList(accessConfig));
    }

    @Override
    public CreateClusterGoogleCloud configureClusterSlaveInstance() {
        //now defining Slave Volumes
        // Map<String, String> snapShotToSlaveMounts = config.getSlaveMounts();
        // int ephemerals = config.getSlaveInstanceType().getEphemerals();
        // slaveDeviceMapper = new DeviceMapper(providerModule, snapShotToSlaveMounts, ephemerals);

        String startupScript = ShellScriptCreator.getUserData(config, environment.getKeypair(), false, false);
        slaveStartupScript = new Metadata.Items().setKey("startup-script").setValue(startupScript);
        return this;
    }

    @Override
    protected InstanceGoogleCloud launchClusterMasterInstance(String masterNameTag) {
        LOG.info("Requesting master instance...");
        final Map<String, String> labels = new HashMap<>();
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_NAME, masterNameTag);
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_BIBIGRID_ID, clusterId);
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_USER, config.getUser());
        // Create instance
        Instance masterInstance = GoogleCloudUtils.getInstanceBuilder(compute, config, masterNameTag,
                config.getMasterInstance().getProviderType().getValue())
                .setNetworkInterfaces(Collections.singletonList(masterNetworkInterface))
                .setLabels(labels)
                .setMetadata(buildMetadata(masterStartupScript));
        GoogleCloudUtils.attachDisks(compute, masterInstance, config.getMasterInstance().getImage(), config,
                config.getMasterMounts(), config.getGoogleImageProjectId());
        GoogleCloudUtils.setInstanceSchedulingOptions(masterInstance, config.isUseSpotInstances());
        // Waiting for master instance to run
        LOG.info("Waiting for master instance to finish booting...");
        try {
            String zone = config.getAvailabilityZone();
            Operation createMasterOperation = compute.instances()
                    .insert(config.getGoogleProjectId(), zone, masterInstance).execute();
            masterInstance = waitForInstances(new Instance[]{masterInstance}, new Operation[]{createMasterOperation}).get(0);
        } catch (Exception e) {
            LOG.error("Failed to start master instance. {}", e);
            return null;
        }
        LOG.info(I, "Master instance is now running!");
        List<Instance> instances = new ArrayList<>();
        instances.add(masterInstance);
        waitForInstancesStatusCheck(instances);
        return new InstanceGoogleCloud(masterInstance);
    }

    private Metadata buildMetadata(Metadata.Items... items) {
        return new Metadata().setItems(Arrays.asList(items));
    }

    @Override
    protected List<de.unibi.cebitec.bibigrid.core.model.Instance> launchClusterSlaveInstances(
            int batchIndex, Configuration.SlaveInstanceConfiguration instanceConfiguration, String slaveNameTag) {
        final int instanceCount = instanceConfiguration.getCount();
        final String zone = config.getAvailabilityZone();
        final Map<String, String> labels = new HashMap<>();
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_NAME, slaveNameTag);
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_BIBIGRID_ID, clusterId);
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_USER, config.getUser());
        // Create instances
        Instance[] slaveInstanceBuilders = new Instance[instanceCount];
        Operation[] slaveInstanceOperations = new Operation[instanceCount];
        for (int i = 0; i < instanceCount; i++) {
            String slaveInstanceId = buildSlaveInstanceName(batchIndex, i);
            Instance slaveBuilder = GoogleCloudUtils.getInstanceBuilder(compute, config, slaveInstanceId,
                    instanceConfiguration.getProviderType().getValue())
                    .setNetworkInterfaces(Collections.singletonList(buildExternalNetworkInterface()))
                    .setLabels(labels)
                    .setMetadata(buildMetadata(slaveStartupScript));
            GoogleCloudUtils.attachDisks(compute, slaveBuilder, instanceConfiguration.getImage(), config,
                    config.getSlaveMounts(), config.getGoogleImageProjectId());
            GoogleCloudUtils.setInstanceSchedulingOptions(slaveBuilder, config.isUseSpotInstances());
            try {
                slaveInstanceOperations[i] = compute.instances()
                        .insert(config.getGoogleProjectId(), zone, slaveBuilder).execute();
                slaveInstanceBuilders[i] = slaveBuilder;
            } catch (Exception e) {
                LOG.error("Failed to start slave instance. {}", e);
                return null;
            }
        }
        LOG.info("Waiting for slave instance(s) to finish booting...");
        List<Instance> slaveInstances = waitForInstances(slaveInstanceBuilders, slaveInstanceOperations);
        LOG.info(I, "Slave instance(s) is now running!");
        waitForInstancesStatusCheck(slaveInstances);
        return slaveInstances.stream().map(InstanceGoogleCloud::new).collect(Collectors.toList());
    }

    private void waitForInstancesStatusCheck(List<Instance> instances) {
        LOG.info("Waiting for Status Checks on instances...");
        for (int i = 0; i < instances.size(); i++) {
            Instance instance = instances.get(i);
            do {
                instance = GoogleCloudUtils.reload(compute, config, instance);
                String status = instance.getStatus();
                LOG.info(V, "Status of " + instance.getName() + " instance: " + status);
                if (status.equals("RUNNING")) {
                    break;
                } else {
                    LOG.info(V, "...");
                    sleep(10);
                }
            } while (true);
            instances.set(i, instance);
        }
        LOG.info(I, "Status checks successful.");
    }

    private List<Instance> waitForInstances(Instance[] instances, Operation[] operations) {
        if (instances.length == 0 || operations.length == 0 || instances.length != operations.length) {
            LOG.error("No instances found");
            return new ArrayList<>();
        }
        List<Instance> returnList = new ArrayList<>();
        for (int i = 0; i < operations.length; i++) {
            Operation operation = operations[i];
            try {
                GoogleCloudUtils.waitForOperation(compute, config, operation);
            } catch (InterruptedException e) {
                LOG.error("Creation of instance {} failed. {}", instances[i], e);
                break;
            }
            returnList.add(GoogleCloudUtils.reload(compute, config, instances[i]));
        }
        return returnList;
    }

    Compute getCompute() {
        return compute;
    }

    @Override
    protected String getSubnetCidr() {
        return environment.getSubnetCidr();
    }
}
package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import com.google.api.services.compute.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.*;
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
    private final Compute compute;
    private Metadata.Items instanceStartupScript;
    private NetworkInterface masterNetworkInterface;

    CreateClusterGoogleCloud(final ProviderModule providerModule, final ConfigurationGoogleCloud config, final String clusterId) {
        super(providerModule, config, clusterId);
        this.config = config;
        compute = ((ClientGoogleCloud) providerModule.getClient()).getInternal();
    }

    @Override
    public void configureClusterMasterInstance() {
        String startupScript = ShellScriptCreator.getUserData(config, false);
        instanceStartupScript = new Metadata.Items().setKey("startup-script").setValue(startupScript);
        masterNetworkInterface = buildExternalNetworkInterface();
        super.configureClusterMasterInstance();
    }

    private NetworkInterface buildExternalNetworkInterface() {
        AccessConfig accessConfig = new AccessConfig().setType("ONE_TO_ONE_NAT").setName("External NAT");
        return new NetworkInterface()
                .setNetwork(((SubnetGoogleCloud) environment.getSubnet()).getInternal().getNetwork())
                .setSubnetwork(environment.getSubnet().getId())
                .setAccessConfigs(Collections.singletonList(accessConfig));
    }

    @Override
    protected List<Configuration.MountPoint> resolveMountSources(List<Configuration.MountPoint> mountPoints) {
        // TODO: possibly check if snapshot or volume like openstack
        return mountPoints;
    }

    @Override
    protected InstanceGoogleCloud launchClusterMasterInstance(String masterNameTag) {
        LOG.info("Requesting master instance ...");
        final Map<String, String> labels = new HashMap<>();
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_NAME, masterNameTag);
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_BIBIGRID_ID, clusterId);
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_USER, config.getUser().replace(".", "_"));
        // Create instance
        Instance masterInstance = GoogleCloudUtils.getInstanceBuilder(compute, config, masterNameTag,
                config.getMasterInstance().getProviderType().getValue())
                .setNetworkInterfaces(Collections.singletonList(masterNetworkInterface))
                .setLabels(labels)
                .setMetadata(buildMetadata(instanceStartupScript));
        GoogleCloudUtils.attachDisks(compute, masterInstance, config.getMasterInstance().getImage(), config,
                config.getMasterMounts(), config.getGoogleImageProjectId());
        GoogleCloudUtils.setInstanceSchedulingOptions(masterInstance, config.isUseSpotInstances());
        // Waiting for master instance to run
        LOG.info("Waiting for master instance to finish booting ...");
        try {
            String zone = config.getAvailabilityZone();
            Operation createMasterOperation = compute.instances()
                    .insert(config.getGoogleProjectId(), zone, masterInstance).execute();
            masterInstance = waitForInstances(new Instance[]{masterInstance}, new Operation[]{createMasterOperation}).get(0);
        } catch (Exception e) {
            LOG.error("Failed to start master instance.", e);
            return null;
        }
        LOG.info(I, "Master instance is now running!");
        List<Instance> instances = new ArrayList<>();
        instances.add(masterInstance);
        waitForInstancesStatusCheck(instances);
        return new InstanceGoogleCloud(config.getMasterInstance(), masterInstance);
    }

    private Metadata buildMetadata(Metadata.Items... items) {
        return new Metadata().setItems(Arrays.asList(items));
    }

    @Override
    protected List<de.unibi.cebitec.bibigrid.core.model.Instance> launchClusterWorkerInstances(
            int batchIndex, Configuration.WorkerInstanceConfiguration instanceConfiguration, String workerNameTag) {
        final int instanceCount = instanceConfiguration.getCount();
        final String zone = config.getAvailabilityZone();
        final Map<String, String> labels = new HashMap<>();
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_NAME, workerNameTag);
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_BIBIGRID_ID, clusterId);
        labels.put(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_USER, config.getUser().replace(".", "_"));
        // Create instances
        Instance[] workerInstanceBuilders = new Instance[instanceCount];
        Operation[] workerInstanceOperations = new Operation[instanceCount];
        for (int i = 0; i < instanceCount; i++) {
            String workerInstanceId = buildWorkerInstanceName(batchIndex + 1, i + 1);
            Instance workerBuilder = GoogleCloudUtils.getInstanceBuilder(compute, config, workerInstanceId,
                    instanceConfiguration.getProviderType().getValue())
                    .setNetworkInterfaces(Collections.singletonList(buildExternalNetworkInterface()))
                    .setLabels(labels)
                    .setMetadata(buildMetadata(instanceStartupScript));
            GoogleCloudUtils.attachDisks(compute, workerBuilder, instanceConfiguration.getImage(), config, null,
                    config.getGoogleImageProjectId());
            GoogleCloudUtils.setInstanceSchedulingOptions(workerBuilder, config.isUseSpotInstances());
            try {
                // Start the instance
                workerInstanceOperations[i] = compute.instances()
                        .insert(config.getGoogleProjectId(), zone, workerBuilder).execute();
                workerInstanceBuilders[i] = workerBuilder;
            } catch (Exception e) {
                LOG.error("Failed to start worker instance.", e);
                return null;
            }
        }
        LOG.info("Waiting for worker instance(s) to finish booting ...");
        List<Instance> workerInstances = waitForInstances(workerInstanceBuilders, workerInstanceOperations);
        LOG.info(I, "Worker instance(s) is now running!");
        waitForInstancesStatusCheck(workerInstances);
        return workerInstances.stream().map(i -> new InstanceGoogleCloud(instanceConfiguration, i)).collect(Collectors.toList());
    }

    @Override
    protected List<de.unibi.cebitec.bibigrid.core.model.Instance> launchAdditionalClusterWorkerInstances(Cluster cluster, int batchIndex, int workerIndex, Configuration.WorkerInstanceConfiguration instanceConfiguration, String workerNameTag) {
        return null;
    }

    private void waitForInstancesStatusCheck(List<Instance> instances) {
        LOG.info("Waiting for status checks on instances ...");
        for (int i = 0; i < instances.size(); i++) {
            Instance instance = instances.get(i);
            do {
                instance = GoogleCloudUtils.reload(compute, config, instance);
                String status = instance.getStatus();
                LOG.info(V, "Status of instance '{}': {}", instance.getName(), status);
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
        if (operations.length == 0 || instances.length != operations.length) {
            LOG.error("No instances found");
            return new ArrayList<>();
        }
        List<Instance> returnList = new ArrayList<>();
        for (int i = 0; i < operations.length; i++) {
            Operation operation = operations[i];
            try {
                GoogleCloudUtils.waitForOperation(compute, config, operation);
            } catch (InterruptedException e) {
                LOG.error("Creation of instance '{}' failed. {}", instances[i], e);
                break;
            }
            returnList.add(GoogleCloudUtils.reload(compute, config, instances[i]));
        }
        return returnList;
    }

    Compute getCompute() {
        return compute;
    }
}
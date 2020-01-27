package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.intents.LoadClusterConfigurationIntent;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Uses Openstack API to load assign values from cluster servers to internal config.
 * Largely adopted from formerly ListIntentOpenstack
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 * @author Tim Dilger - tdilger(at)techfak.uni-bielefeld.de
 */
public class LoadClusterConfigurationIntentOpenstack extends LoadClusterConfigurationIntent {
    private static final Logger LOG = LoggerFactory.getLogger(LoadClusterConfigurationIntentOpenstack.class);
    private final OSClient os;

    LoadClusterConfigurationIntentOpenstack(ProviderModule providerModule, Client client, Configuration config) {
        super(providerModule, client, config);
        os = ((ClientOpenstack) client).getInternal();

        clusterMap = new HashMap<>();
        assignClusterConfigValues();
        if (clusterMap.isEmpty()) {
            LOG.error("No BiBiGrid cluster found!\n");
        } else {
            for (Cluster cluster : clusterMap.values()) {
                loadInstanceConfiguration(cluster.getMasterInstance());
                for (Instance worker : cluster.getWorkerInstances()) {
                    loadInstanceConfiguration(worker);
                }
            }
        }
    }

    @Override
    public void assignClusterConfigValues() {
        Map<String, List<InstanceOpenstack>> instanceList = createInstanceMap();
        for (String clusterId : instanceList.keySet()) {
            createCluster(clusterId, instanceList.get(clusterId));
        }
    }

    /**
     * Initializes map of clusterIds and corresponding instances.
     * @return instanceMap
     */
    private Map<String, List<InstanceOpenstack>> createInstanceMap() {
        Map<String, List<InstanceOpenstack>> instanceList = new HashMap<>();
        for (Server server : os.compute().servers().list()) {
            String clusterId = server.getMetadata().get(Instance.TAG_BIBIGRID_ID);
            InstanceOpenstack instance = new InstanceOpenstack(null, server);
            if (!server.getMetadata().containsKey(Instance.TAG_BATCH)) {
                instance.setMaster(true);
            }
            if (instanceList.containsKey(clusterId)) {
                instanceList.get(clusterId).add(instance);
            } else {
                List<InstanceOpenstack> clusterInstances = new ArrayList<>();
                clusterInstances.add(instance);
                instanceList.put(clusterId, clusterInstances);
            }
        }
        return instanceList;
    }

    /**
     * Creates a new cluster from specified clusterId and extends clusterMap.
     * @param clusterId Id of cluster
     * @param instances list of master and worker instance(s)
     */
    private void createCluster(String clusterId, List<InstanceOpenstack> instances) {
        Cluster cluster = new Cluster(clusterId);
        for (InstanceOpenstack instance : instances) {
            if (instance.isMaster()) {
                cluster.setMasterInstance(instance);
            } else {
                cluster.addWorkerInstance(instance);
            }
        }
        clusterMap.put(clusterId, cluster);
    }

    private void searchSecurityGroups() {
        for (SecGroupExtension sg : os.compute().securityGroups().list()) {
            String name = sg.getName();
            if (name != null && name.startsWith(CreateClusterEnvironment.SECURITY_GROUP_PREFIX)) {
                // getOrCreateCluster(getClusterIdFromName(name)).setSecurityGroup(sg.getId());
            }
        }
    }

    @Override
    public List<Instance> getInstances() {
        List<? extends Server> serverList = os.compute().servers().list();
        List<Instance> list = serverList.stream().map(i -> new InstanceOpenstack(null, i)).collect(Collectors.toList());
        Collections.reverse(list);
        return list;
    }

    @Override
    public void loadInstanceConfiguration(Instance instance) {
        Configuration.InstanceConfiguration instanceConfiguration =
                instance.isMaster() ? new Configuration.InstanceConfiguration()
                        : new Configuration.WorkerInstanceConfiguration();
        Server server = os.compute().servers().get(instance.getId());
        Flavor flavor = server.getFlavor();
        if (flavor != null) {
            instanceConfiguration.setType(flavor.getName());
            try {
                instanceConfiguration.setProviderType(providerModule.getInstanceType(client, config, flavor.getName()));
            } catch (InstanceTypeNotFoundException ignored) {
            }
        }
        server.getAvailabilityZone();
        Map<String, String> metadata = server.getMetadata();
        String wb = metadata.get(Instance.TAG_BATCH);
        int workerBatch;
        if (wb != null) {
            workerBatch = Integer.parseInt(server.getMetadata().get(Instance.TAG_BATCH));
            instance.setBatchIndex(workerBatch);
        } else {
            if (instance.isMaster()) {
                LOG.warn("{} - Could not set worker batch. Continuing ...", instance.getId());
            }
        }
        Image image = server.getImage();
        if (image != null) {
            instanceConfiguration.setImage(image.getName());
        }
        instance.setConfiguration(instanceConfiguration);
    }

}

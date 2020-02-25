package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handling of cluster internal configuration loading from server and provider API.
 *
 * @author Tim Dilger - tdilger@techfak.uni-bielefeld.de
 */
public abstract class LoadClusterConfigurationIntent extends Intent {
    public static final Logger LOG = LoggerFactory.getLogger(LoadClusterConfigurationIntent.class);
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");

    protected final ProviderModule providerModule;
    protected final Client client;
    protected final Configuration config;
    private Map<String, Cluster> clusterMap;

    protected LoadClusterConfigurationIntent(ProviderModule providerModule, Client client, Configuration config) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
    }

    /**
     * Loads cluster and instance configuration(s) from internal config.
     */
    public void loadClusterConfiguration() {
        LOG.info("Load Cluster Configurations ...");
        Map<String, List<Instance>> instanceMap = createInstanceMap();
        clusterMap = new HashMap<>();
        if (instanceMap.isEmpty()) {
            LOG.warn("No BiBiGrid cluster found!\n");
        } else {
            for (String clusterId : instanceMap.keySet()) {
                List<Instance> clusterInstances = instanceMap.get(clusterId);
                for (Instance instance : clusterInstances) {
                    loadInstanceConfiguration(instance);
                }
                initCluster(clusterId, clusterInstances);
            }
        }
    }

    /**
     * Initializes map of clusterIds and corresponding instances.
     * @return instanceMap
     */
    public abstract Map<String, List<Instance>> createInstanceMap();

    /**
     * Initializes a new cluster from specified clusterId and extends clusterMap.
     * @param clusterId Id of cluster
     * @param clusterInstances list of master and worker instance(s)
     */
    private void initCluster(String clusterId, List<Instance> clusterInstances) {
        Cluster cluster = new Cluster(clusterId);
        for (Instance instance : clusterInstances) {
            if (instance.isMaster()) {
                if (cluster.getMasterInstance() == null) {
                    cluster.setMasterInstance(instance);
                    cluster.setPublicIp(instance.getPublicIp());
                    cluster.setPrivateIp(instance.getPrivateIp());
                    cluster.setKeyName(instance.getKeyName());
                    cluster.setAvailabilityZone(config.getAvailabilityZone());
                    // TODO add Security Group / Server Group / Subnet ?
                    cluster.setStarted(instance.getCreationTimestamp()
                            .format(DATE_TIME_FORMATTER));
                } else {
                    LOG.error("Detected two master instances ({},{}) for cluster '{}'.",
                            cluster.getMasterInstance().getName(),
                            instance.getName(), clusterId);
                }
            } else {
                cluster.addWorkerInstance(instance);
            }
            checkInstanceKeyName(cluster, instance);
            checkInstanceUserTag(cluster, instance);
        }
        clusterMap.put(clusterId, cluster);
    }

    /**
     * Determines clusterMap if not available and gets cluster with specified clusterId.
     * @param clusterId Id of cluster
     * @return Cluster initialized or already available in clusterMap
     */
    public Cluster getCluster(String clusterId) {
        if (clusterMap.isEmpty()) {
            LOG.error("No BiBiGrid cluster found!\n");
        }
        if (!clusterMap.containsKey(clusterId)) {
            LOG.error("No BiBiGrid cluster with id '" + clusterId + "' found!\n");
        }
        return clusterMap.get(clusterId);
    }

    /**
     * Return a Map of Cluster objects within current configuration.
     */
    public final Map<String, Cluster> getClusterMap() {
        return clusterMap;
    }

    public abstract List<Instance> getInstances();

    /**
     * Checks, if key name is always the same for all instances of one cluster as should be.
     * @param cluster cluster of specific instance
     * @param instance master or worker
     */
    private void checkInstanceKeyName(Cluster cluster, Instance instance) {
        if (cluster.getKeyName() != null) {
            if (!cluster.getKeyName().equals(instance.getKeyName())) {
                LOG.error("Detected two different keynames ({},{}) for cluster '{}'.", cluster.getKeyName(),
                        instance.getKeyName(), cluster.getClusterId());
            }
        } else {
            cluster.setKeyName(instance.getKeyName());
        }
    }

    /**
     * Checks, if user is always the same for cluster.
     * @param cluster cluster of specific instance
     * @param instance master or worker
     */
    private void checkInstanceUserTag(Cluster cluster, Instance instance) {
        String user = instance.getTag(Instance.TAG_USER);
        if (user != null) {
            if (cluster.getUser() == null) {
                cluster.setUser(user);
            } else if (!cluster.getUser().equals(user)) {
                LOG.error("Detected two different users ({},{}) for cluster '{}'.", cluster.getUser(), user,
                        cluster.getClusterId());
            }
        }
    }

    /**
     * Loads config of an instance from server.
     * @param instance master or worker instance
     */
    protected abstract void loadInstanceConfiguration(Instance instance);
}

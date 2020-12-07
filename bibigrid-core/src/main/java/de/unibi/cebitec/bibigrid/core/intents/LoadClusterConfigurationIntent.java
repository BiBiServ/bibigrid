package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.*;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * Handling of cluster internal configuration loading from server and provider API.
 *
 * @author Tim Dilger - tdilger@techfak.uni-bielefeld.de
 */
public abstract class LoadClusterConfigurationIntent extends Intent {
    public static final Logger LOG = LoggerFactory.getLogger(LoadClusterConfigurationIntent.class);
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");

    protected final ProviderModule providerModule;
    protected final Configuration config;
    private final Map<String, Cluster> clusterMap;

    protected LoadClusterConfigurationIntent(ProviderModule providerModule, Configuration config) {
        this.providerModule = providerModule;
        this.config = config;
        clusterMap = new HashMap<>();
    }

    /**
     * Loads cluster and instance configuration(s) from internal config.
     * @param clusterId parameter given
     */
    public void loadClusterConfiguration(String clusterId) {
        LOG.info("Load Cluster Configurations ...");
        Map<String, List<Instance>> instanceMap = createInstanceMap(clusterId);
        if (instanceMap.isEmpty()) {
            LOG.info("No BiBiGrid cluster found!\n");
            return;
        } else {
            if (clusterId == null || instanceMap.get(clusterId) == null) {
                LOG.info("Loading Configuration for all clusters ...\n");
                for (String cid : instanceMap.keySet()) {
                    loadSingleClusterConfiguration(instanceMap.get(cid), cid);
                }
            } else {
                // Only load necessary cluster configuration
                loadSingleClusterConfiguration(instanceMap.get(clusterId), clusterId);
            }

        }
        LOG.info("Cluster Configuration loaded successfully.");
    }

    /**
     * Loads configuration of a cluster and initializes Cluster object.
     * @param clusterInstances instance list of cluster
     * @param clusterId id of cluster
     */
    private void loadSingleClusterConfiguration(List<Instance> clusterInstances, String clusterId) {
        LOG.info("Loading Configuration for cluster with id {} ...", clusterId);
        for (Instance instance : clusterInstances) {
            LOG.info(V, "Loading Configuration for instance {} ...", instance.getName());
            loadInstanceConfiguration(instance);
            LOG.info(V, "Configuration for instance {} loaded successfully.", instance.getName());
        }
        LOG.info(V, "Initialize cluster with id {} ...", clusterId);
        initCluster(clusterId, clusterInstances);
        LOG.info("Cluster with id {} initialized successfully.\n", clusterId);
    }

    /**
     * Initializes map of clusterIds and corresponding instances.
     * @return instanceMap
     */
    public abstract Map<String, List<Instance>> createInstanceMap(String clusterId);

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
                    // TODO AvailabilityZone not from config file, since it might be different from expectation
                    cluster.setAvailabilityZone(config.getAvailabilityZone());
                    // TODO add Server Group / Subnet ?
                    cluster.setSecurityGroup(instance.getConfiguration().getSecurityGroup());
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
        Collections.sort(cluster.getWorkerInstances());
        clusterMap.put(clusterId, cluster);
    }

    /**
     * Determines clusterMap if not available and gets cluster with specified clusterId.
     * @param clusterId Id of cluster
     * @return Cluster initialized or already available in clusterMap, null if no cluster found
     */
    Cluster getCluster(String clusterId) {
        if (clusterMap.isEmpty()) {
            LOG.error("No BiBiGrid cluster found!\n");
            return null;
        }
        if (!clusterMap.containsKey(clusterId)) {
            LOG.error("No BiBiGrid cluster with id '" + clusterId + "' found!\n");
            return null;
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

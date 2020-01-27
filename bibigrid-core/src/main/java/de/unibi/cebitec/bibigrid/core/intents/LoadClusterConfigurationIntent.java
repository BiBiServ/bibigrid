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
    private static final Logger LOG = LoggerFactory.getLogger(LoadClusterConfigurationIntent.class);
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");

    protected final ProviderModule providerModule;
    protected final Client client;
    protected final Configuration config;
    protected Map<String, Cluster> clusterMap;

    protected LoadClusterConfigurationIntent(ProviderModule providerModule, Client client, Configuration config) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
    }

    /**
     * Assigns cluster values from single servers to internal config, initializes clusterMap.
     */
    public abstract void assignClusterConfigValues();

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

    /**
     * Extracts cluster Id from name, e.g. bibigrid-worker1-2-jbxoecbvtfu3n5f.
     * TODO Probably an error source, if names change -> search in clusterMap and getId ?
     * @param name instance name
     * @return clusterId
     */
    private static String getClusterIdFromName(String name) {
        String[] parts = name.split("-");
        return parts[parts.length - 1];
    }

    public abstract List<Instance> getInstances();

    /**
     * Checks if instance is a BiBiGrid instance and extract clusterId from it
     * @param instance master or worker
     */
    protected void initCluster(Instance instance) {
        String clusterId = getClusterIdForInstance(instance);
        if (clusterId == null)
            return;
        Cluster cluster = getCluster(clusterId);
        // Check whether master or worker instance
        String name = instance.getTag(Instance.TAG_NAME);
        if (name == null) {
            name = instance.getName();
        }
        if (name != null && name.startsWith(CreateCluster.MASTER_NAME_PREFIX)) {
            if (cluster.getMasterInstance() == null) {
                cluster.setMasterInstance(instance);
                cluster.setPublicIp(instance.getPublicIp());
                cluster.setPrivateIp(instance.getPrivateIp());
                cluster.setKeyName(instance.getKeyName());
                cluster.setStarted(instance.getCreationTimestamp().format(DATE_TIME_FORMATTER));
            } else {
                LOG.error("Detected two master instances ({},{}) for cluster '{}'.", cluster.getMasterInstance().getName(),
                        instance.getName(), clusterId);
            }
        } else {
            cluster.addWorkerInstance(instance);
        }
        checkInstanceKeyName(instance, cluster);
        checkInstanceUserTag(instance, cluster);
    }

    /**
     * Checks, if key name is always the same for all instances of one cluster as should be.
     * @param instance master or worker
     * @param cluster
     */
    private void checkInstanceKeyName(Instance instance, Cluster cluster) {
        if (cluster.getKeyName() != null) {
            if (!cluster.getKeyName().equals(instance.getKeyName())) {
                LOG.error("Detected two different keynames ({},{}) for cluster '{}'.", cluster.getKeyName(),
                        instance.getKeyName(), cluster.getClusterId());
            }
        } else {
            cluster.setKeyName(instance.getKeyName());
        }
    }

    private void checkInstanceUserTag(Instance instance, Cluster cluster) {
        // user should be always the same for all instances of one cluster
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

    private String getClusterIdForInstance(Instance instance) {
        String clusterIdTag = instance.getTag(Instance.TAG_BIBIGRID_ID);
        if (clusterIdTag != null) {
            return clusterIdTag;
        }
        String name = instance.getName();
        if (name != null && (name.startsWith(CreateCluster.MASTER_NAME_PREFIX) ||
                name.startsWith(CreateCluster.WORKER_NAME_PREFIX))) {
            return getClusterIdFromName(name);
        }
        return null;
    }

    /**
     * Loads config of an instance from server.
     * @param instance master or worker instance
     */
    protected abstract void loadInstanceConfiguration(Instance instance);
}

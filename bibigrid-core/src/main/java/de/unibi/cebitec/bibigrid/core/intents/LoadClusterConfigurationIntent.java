package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class LoadClusterConfigurationIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(ListIntent.class);
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");

    protected final ProviderModule providerModule;
    protected final Client client;
    protected final Configuration config;
    private Map<String, Cluster> clusterMap;

    protected LoadClusterConfigurationIntent(ProviderModule providerModule, Client client, Configuration config) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;

        if (clusterMap == null) {
            clusterMap = new HashMap<>();
            assignClusterConfigValues();
        }
        if (clusterMap.isEmpty()) {
            LOG.error("No BiBiGrid cluster found!\n");
        }
    }

    /**
     * Determines clusterMap if not available and gets cluster with specified clusterId.
     * @param clusterId Id of cluster
     * @return Cluster initialized or already available in clusterMap
     */
    public Cluster getCluster(String clusterId) {
        if (clusterMap == null) {
            clusterMap = new HashMap<>();
            assignClusterConfigValues();
        }
        if (clusterMap.isEmpty()) {
            LOG.error("No BiBiGrid cluster found!\n");
        }
        if (!clusterMap.containsKey(clusterId)) {
            LOG.error("No BiBiGrid cluster with id '" + clusterId + "' found!\n");
        }
        return clusterMap.get(clusterId);
    }

    /**
     * Extract cluster from clusterMap or create a new one if not yet existent.
     * TODO little confusing since the class CreateCluster is not used here.
     *      Instead ListIntent uses only Cluster as representation
     * @param clusterId Id of cluster
     * @return cluster
     */
    protected final Cluster createCluster(String clusterId) {
        Cluster cluster = new Cluster(clusterId);
        clusterMap.put(clusterId, cluster);
        return cluster;
    }

    /**
     * Return a Map of Cluster objects within current configuration.
     */
    public final Map<String, Cluster> getList() {
        if (clusterMap == null) {
            clusterMap = new HashMap<>();
            assignClusterConfigValues();
        }
        return clusterMap;
    }

    /**
     * Extracts cluster Id from name, e.g. bibigrid-worker1-2-jbxoecbvtfu3n5f.
     * TODO Probably an error source, if names change -> search in clusterMap and getId ?
     * @param name instance name
     * @return clusterId
     */
    protected static String getClusterIdFromName(String name) {
        String[] parts = name.split("-");
        return parts[parts.length - 1];
    }

    /**
     * Assigns cluster values from single servers to internal config.
     */
    protected void assignClusterConfigValues() {
        List<Instance> instances = getInstances();
        if (instances != null) {
            for (Instance instance : instances) {
                checkInstance(instance);
            }
        }
        List<Network> networks = client.getNetworks();
        if (networks != null) {
            for (Network network : networks) {
                String name = network.getName();
                if (name != null && name.startsWith(CreateClusterEnvironment.NETWORK_PREFIX)) {

                    getOrCreateCluster(getClusterIdFromName(name)).setNetwork(network);
                }
            }
        }
        List<Subnet> subnets = client.getSubnets();
        if (subnets != null) {
            for (Subnet subnet : subnets) {
                String name = subnet.getName();
                if (name != null && name.startsWith(CreateClusterEnvironment.SUBNET_PREFIX)) {
                    getOrCreateCluster(getClusterIdFromName(name)).setSubnet(subnet);
                }
            }
        }
        List<String> keypairs = client.getKeypairNames();
        if (keypairs != null) {
            for (String name : keypairs) {
                if (name != null && name.startsWith(CreateCluster.PREFIX)) {
                    getOrCreateCluster(getClusterIdFromName(name)).setKeyName(name);
                }
            }
        }

    }

    protected abstract List<Instance> getInstances();

    /**
     * Loads instance configurations from internal and sets cluster and instance config.
     * @param instance master or worker
     */
    protected void checkInstance(Instance instance) {
        // check if instance is a BiBiGrid instance and extract clusterId from it
        String clusterId = getClusterIdForInstance(instance);
        if (clusterId == null)
            return;
        Cluster cluster = getOrCreateCluster(clusterId);
        loadInstanceConfiguration(instance);
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

    private void checkInstanceKeyName(Instance instance, Cluster cluster) {
        //key name should be always the same for all instances of one cluster
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

    protected abstract void loadInstanceConfiguration(Instance instance);
}

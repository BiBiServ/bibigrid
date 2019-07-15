package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Creates a Map of BiBiGrid cluster instances
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public abstract class ListIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(ListIntent.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");

    protected final ProviderModule providerModule;
    protected final Client client;
    protected final Configuration config;
    private Map<String, Cluster> clusterMap;

    protected ListIntent(ProviderModule providerModule, Client client, Configuration config) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
    }

    protected final Cluster getOrCreateCluster(String clusterId) {
        Cluster cluster;
        // check if entry already available
        if (clusterMap.containsKey(clusterId)) {
            cluster = clusterMap.get(clusterId);
        } else {
            cluster = new Cluster(clusterId);
            clusterMap.put(clusterId, cluster);
        }
        return cluster;
    }

    /**
     * Return a Map of Cluster objects within current configuration.
     */
    public final Map<String, Cluster> getList() {
        if (clusterMap == null) {
            clusterMap = new HashMap<>();
            searchClusterIfNecessary();
        }
        return clusterMap;
    }

    protected static String getClusterIdFromName(String name) {
        String[] parts = name.split("-");
        return parts[parts.length - 1];
    }

    protected void searchClusterIfNecessary() {
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
    }

    protected abstract List<Instance> getInstances();

    protected void checkInstance(Instance instance) {
        // check if instance is a BiBiGrid instance and extract clusterId from it
        String clusterId = getClusterIdForInstance(instance);
        if (clusterId == null)
            return;
        Cluster cluster = getOrCreateCluster(clusterId);
        loadInstanceConfiguration(instance);
        // Check whether master or slave instance
        String name = instance.getTag(Instance.TAG_NAME);
        if (name == null) {
            name = instance.getName();
        }
        if (name != null && name.startsWith(CreateCluster.MASTER_NAME_PREFIX)) {
            if (cluster.getMasterInstance() == null) {
                cluster.setMasterInstance(instance);
                cluster.setPublicIp(instance.getPublicIp());
                cluster.setKeyName(instance.getKeyName());
                cluster.setStarted(instance.getCreationTimestamp().format(dateTimeFormatter));
            } else {
                LOG.error("Detected two master instances ({},{}) for cluster '{}'.", cluster.getMasterInstance().getName(),
                        instance.getName(), clusterId);
            }
        } else {
            cluster.addSlaveInstance(instance);
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
                name.startsWith(CreateCluster.SLAVE_NAME_PREFIX))) {
            return getClusterIdFromName(name);
        }
        return null;
    }

    protected abstract void loadInstanceConfiguration(Instance instance);

    /**
     * Return a String representation of found cluster objects map.
     */
    @Override
    public final String toString() {
        if (clusterMap == null) {
            clusterMap = new HashMap<>();
            searchClusterIfNecessary();
        }
        if (clusterMap.isEmpty()) {
            return "No BiBiGrid cluster found!\n";
        }
        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        display.append("\n");
        formatter.format("%15s | %10s | %19s | %15s | %15s | %7s | %15s | %11s | %11s%n",
                "cluster-id", "user", "launch date", "key name", "public-ip", "# inst", "group-id", "subnet-id",
                "network-id");
        display.append(new String(new char[142]).replace('\0', '-')).append("\n");
        for (Map.Entry<String, Cluster> entry : clusterMap.entrySet()) {
            Cluster v = entry.getValue();
            formatter.format("%15s | %10s | %19s | %15s | %15s | %7d | %15s | %11s | %11s%n",
                    entry.getKey(),
                    (v.getUser() == null) ? "-" : v.getUser(),
                    (v.getStarted() == null) ? "-" : v.getStarted(),
                    (v.getKeyName() == null ? "-" : v.getKeyName()),
                    (v.getPublicIp() == null ? "-" : v.getPublicIp()),
                    ((v.getMasterInstance() != null ? 1 : 0) + v.getSlaveInstances().size()),
                    (v.getSecurityGroup() == null ? "-" : cutStringIfNecessary(v.getSecurityGroup())),
                    (v.getSubnet() == null ? "-" : cutStringIfNecessary(v.getSubnet().getId())),
                    (v.getNetwork() == null ? "-" : cutStringIfNecessary(v.getNetwork().getId())));
        }
        return display.toString();
    }

    public final String toDetailString(String clusterId) {
        if (clusterMap == null) {
            clusterMap = new HashMap<>();
            searchClusterIfNecessary();
        }
        if (clusterMap.isEmpty()) {
            return "No BiBiGrid cluster found!\n";
        }
        if (!clusterMap.containsKey(clusterId)) {
            return "No BiBiGrid cluster with id '" + clusterId + "' found!\n";
        }
        Cluster cluster = clusterMap.get(clusterId);
        StringBuilder display = new StringBuilder();
        display.append("cluster-id: ").append(cluster.getClusterId()).append("\n");
        display.append("user: ").append(cluster.getUser()).append("\n");
        if (cluster.getNetwork() != null) {
            display.append("\nnetwork:\n");
            display.append("  id: ").append(cluster.getNetwork().getId()).append("\n");
            display.append("  name: ").append(cluster.getNetwork().getName()).append("\n");
            display.append("  cidr: ").append(cluster.getNetwork().getCidr()).append("\n");
        }
        if (cluster.getSubnet() != null) {
            display.append("\nsubnet:\n");
            display.append("  id: ").append(cluster.getSubnet().getId()).append("\n");
            display.append("  name: ").append(cluster.getSubnet().getName()).append("\n");
            display.append("  cidr: ").append(cluster.getSubnet().getCidr()).append("\n");
        }
        if (cluster.getMasterInstance() != null) {
            display.append("\nmaster-instance:\n");
            addInstanceToDetailString(display, cluster.getMasterInstance());
        }
        if (cluster.getSlaveInstances() != null) {
            for (Instance slaveInstance : cluster.getSlaveInstances()) {
                display.append("\nslave-instance:\n");
                addInstanceToDetailString(display, slaveInstance);
            }
        }
        return display.toString();
    }

    private void addInstanceToDetailString(StringBuilder display, Instance instance) {
        display.append("  id: ").append(instance.getId()).append("\n");
        display.append("  name: ").append(instance.getName()).append("\n");
        display.append("  hostname: ").append(instance.getHostname()).append("\n");
        display.append("  private-ip: ").append(instance.getPrivateIp()).append("\n");
        display.append("  public-ip: ").append(instance.getPublicIp()).append("\n");
        display.append("  key-name: ").append(instance.getKeyName()).append("\n");
        Configuration.InstanceConfiguration instanceConfig = instance.getConfiguration();
        if (instanceConfig != null) {
            display.append("  type: ").append(instanceConfig.getType()).append("\n");
            display.append("  image: ").append(instanceConfig.getImage()).append("\n");
        }
        display.append("  launch-date: ").append(instance.getCreationTimestamp().format(dateTimeFormatter)).append("\n");
    }

    private String cutStringIfNecessary(String s) {
        return s.length() > 11 ? s.substring(0, 9) + ".." : s;
    }
}

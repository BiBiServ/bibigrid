package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Creates a Map of BiBiGrid cluster instances
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public abstract class ListIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(ListIntent.class);

    private static final int LEN_ID_CLUSTER = 20;
    private static final int LEN_USER = 13;
    private static final int LEN_LAUNCH = 19;
    private static final int LEN_KEY = 14;
    private static final int LEN_IP_PUB = 15;
    private static final int LEN_IP_PRIV = 15;
    private static final int LEN_COUNT = 7;
    private static final int LEN_ID_GROUP = 18;
    private static final int LEN_ID_SUBNET = 11;
    private static final int LEN_ID_NET = 11;
    private static final int LEN_TOTAL = LEN_ID_CLUSTER + LEN_USER + LEN_LAUNCH + LEN_KEY +
            LEN_IP_PUB + LEN_IP_PRIV + LEN_COUNT + LEN_ID_GROUP + LEN_ID_SUBNET + LEN_ID_NET;

    private Map<String, Cluster> clusterMap;

    protected ListIntent(Map<String, Cluster> clusterMap) {
        this.clusterMap = clusterMap;
    }

    /**
     * Return a String representation of found cluster objects map.
     * ToDo magic numbers in single ints and new char[ sum ]
     */
    @Override
    public final String toString() {
        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        display.append("\n");
        String lineFormat = "%" + LEN_ID_CLUSTER + "s" +
                            " | %" + LEN_USER + "s" +
                            " | %" + LEN_LAUNCH + "s" +
                            " | %" + LEN_KEY + "s" +
                            " | %" + LEN_IP_PUB + "s" +
                            " | %" + LEN_IP_PRIV + "s" +
                            " | %" + LEN_COUNT + "s" +
                            " | %" + LEN_ID_GROUP + "s" +
                            " | %" + LEN_ID_SUBNET + "s" +
                            " | %" + LEN_ID_NET +"s%n";
        formatter.format(lineFormat,
                "cluster-id", "user", "launch date", "key name", "public-ip", "private-ip",
                "# inst", "group-id", "subnet-id", "network-id");
        display.append(new String(new char[(3 * 9) + LEN_TOTAL]).replace('\0', '-')).append("\n");
        for (Map.Entry<String, Cluster> entry : clusterMap.entrySet()) {
            Cluster v = entry.getValue();
            formatter.format(lineFormat,
                    ellipsize(entry.getKey(), LEN_ID_CLUSTER),
                    (v.getUser() == null) ? "-" : ellipsize(v.getUser(), LEN_USER),
                    (v.getStarted() == null) ? "-" : v.getStarted(),
                    (v.getKeyName() == null ? "-" : ellipsize(v.getKeyName(), LEN_KEY)),
                    (v.getPublicIp() == null ? "-" : v.getPublicIp()),
                    (v.getPrivateIp() == null ? "-" : v.getPrivateIp()),
                    ((v.getMasterInstance() != null ? 1 : 0) + v.getWorkerInstances().size()),
                    (v.getSecurityGroup() == null ? "-" : ellipsize(v.getSecurityGroup(), LEN_ID_GROUP)),
                    (v.getSubnet() == null ? "-" : ellipsize(v.getSubnet().getId(), LEN_ID_SUBNET)),
                    (v.getNetwork() == null ? "-" : ellipsize(v.getNetwork().getId(), LEN_ID_NET)));

        }
        return display.toString();
    }

    /**
     * Shorten a string to fit a maximum length and possibly add an ellipse at the end.
     *
     * @param s string to shorten
     * @param maxLen maximum length for the string to fit into
     * @return shortened string
     */
    private String ellipsize(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        if (s.length() > maxLen) {
            return s.substring(0, maxLen - 1).concat("…");
        } else {
            return s;
        }
    }

    public final String toDetailString(String clusterId) {
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
        List<Instance> workerInstances = cluster.getWorkerInstances();
        if (workerInstances != null) {
            for (Instance workerInstance : workerInstances) {
                    display.append("\nworker-instance:\n");
                    addInstanceToDetailString(display, workerInstance);
            }
        }
        return display.toString();
    }

    /**
     * Displays detailed information about cluster nodes when using '-l <cluster-id>'.
     * @param display cluster node information String
     * @param instance cluster node
     */
    private void addInstanceToDetailString(StringBuilder display, Instance instance) {
        display.append("  id: ").append(instance.getId()).append("\n");
        display.append("  name: ").append(instance.getName()).append("\n");
        display.append("  hostname: ").append(instance.getHostname()).append("\n");
        if (!instance.isMaster()) {
            display.append("  worker-batch: ").append(instance.getBatchIndex()).append("\n");
        }
        display.append("  private-ip: ").append(instance.getPrivateIp()).append("\n");
        display.append("  public-ip: ").append(instance.getPublicIp()).append("\n");
        display.append("  key-name: ").append(instance.getKeyName()).append("\n");
        Configuration.InstanceConfiguration instanceConfig = instance.getConfiguration();
        if (instanceConfig != null) {
            display.append("  type: ").append(instanceConfig.getType()).append("\n");
            display.append("  image: ").append(instanceConfig.getImage()).append("\n");
        }
        display.append("  launch-date: ").append(instance.getCreationTimestamp().format(LoadClusterConfigurationIntent.DATE_TIME_FORMATTER)).append("\n");
    }
}

package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Creates a Map of BiBiGrid cluster instances
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public abstract class ListIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(ListIntent.class);

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
        String lineFormat = "%16s | %13s | %19s | %14s | %15s | %15s | %7s | %13s | %11s | %11s%n";
        formatter.format(lineFormat,
                "cluster-id", "user", "launch date", "key name", "public-ip", "private-ip", "# inst", "group-id", "subnet-id",
                "network-id");
        display.append(new String(new char[161]).replace('\0', '-')).append("\n");
        for (Map.Entry<String, Cluster> entry : clusterMap.entrySet()) {
            Cluster v = entry.getValue();
            formatter.format(lineFormat,
                    entry.getKey(),
                    (v.getUser() == null) ? "-" : ellipsize(v.getUser(), 13),
                    (v.getStarted() == null) ? "-" : v.getStarted(),
                    (v.getKeyName() == null ? "-" : ellipsize(v.getKeyName(), 14)),
                    (v.getPublicIp() == null ? "-" : v.getPublicIp()),
                    (v.getPrivateIp() == null ? "-" : v.getPrivateIp()),
                    ((v.getMasterInstance() != null ? 1 : 0) + v.getWorkerInstances().size()),
                    (v.getSecurityGroup() == null ? "-" : ellipsize(v.getSecurityGroup(), 13)),
                    (v.getSubnet() == null ? "-" : ellipsize(v.getSubnet().getId(), 11)),
                    (v.getNetwork() == null ? "-" : ellipsize(v.getNetwork().getId(), 11)));

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
        if (cluster.getWorkerInstances() != null) {
            for (Instance workerInstance : cluster.getWorkerInstances()) {
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

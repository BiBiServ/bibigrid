package de.unibi.cebitec.bibigrid.core.intents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
     * Return a json representation string of found cluster objects map.
     */
    public final String toJsonString(){
        Map<String, Object> jsonMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        if (clusterMap == null) {
            clusterMap = new HashMap<>();
            // TODO searchClusterIfNecessary();
        }
        if (clusterMap.isEmpty()) {
            jsonMap.put("info","No BiBiGrid cluster found!");
            try {
                return mapper.writeValueAsString(jsonMap);
            }
            catch(JsonProcessingException e){
                return "{\"Internal server error\":\"JsonProcessingException\"}";
            }
        }
        List<Map<String, Object>> list = new LinkedList<>();
        for (Map.Entry<String, Cluster> entry : clusterMap.entrySet()) {
            Cluster v = entry.getValue();
            Map<String, Object> cluster = new HashMap<>();
            cluster.put("cluster-id",v.getClusterId());
            cluster.put("user",(v.getUser() == null) ? "-" : v.getUser());
            cluster.put("launch date", (v.getStarted() == null) ? "-" : v.getStarted());
            cluster.put("key name", (v.getKeyName() == null ? "-" : v.getKeyName()));
            cluster.put("public-ip",(v.getPublicIp() == null ? "-" : v.getPublicIp()));
            cluster.put("# inst", ((v.getMasterInstance() != null ? 1 : 0) + v.getWorkerInstances().size()));
            cluster.put("group-id", (v.getSecurityGroup() == null ? "-" : v.getSecurityGroup()));
            cluster.put("subnet-id", (v.getSubnet() == null ? "-" : v.getSubnet().getId()));
            cluster.put("network-id"   , (v.getNetwork() == null ? "-" : v.getNetwork().getId()));
            list.add(cluster);
        }
        jsonMap.put("info", list);
        try {
            return mapper.writeValueAsString(jsonMap);
        }
        catch(JsonProcessingException e){
            return "{\"Internal server error\":\"JsonProcessingException\"}";
        }
    }


    /**
     * Return a String representation of found cluster objects map.
     */
    @Override
    public final String toString() {
        if (clusterMap.isEmpty()) {
            return "No Cluster started.";
        }
        LOG.info("Listing Cluster Configurations:\n");
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
        display.append(new String(new char[(9 * 3) + LEN_TOTAL]).replace('\0', '-')).append("\n");
        List<Cluster> clusters = new ArrayList(clusterMap.values());
        Collections.sort(clusters);
        for (Cluster cluster : clusters) {
            formatter.format(lineFormat,
                    ellipsize(cluster.getClusterId(), LEN_ID_CLUSTER),
                    (cluster.getUser() == null) ? "-" : ellipsize(cluster.getUser(), LEN_USER),
                    (cluster.getStarted() == null) ? "-" : cluster.getStarted(),
                    (cluster.getKeyName() == null ? "-" : ellipsize(cluster.getKeyName(), LEN_KEY)),
                    (cluster.getPublicIp() == null ? "-" : cluster.getPublicIp()),
                    (cluster.getPrivateIp() == null ? "-" : cluster.getPrivateIp()),
                    ((cluster.getMasterInstance() != null ? 1 : 0) + cluster.getWorkerInstances().size()),
                    (cluster.getSecurityGroup() == null ? "-" : ellipsize(cluster.getSecurityGroup(), LEN_ID_GROUP)),
                    (cluster.getSubnet() == null ? "-" : ellipsize(cluster.getSubnet().getId(), LEN_ID_SUBNET)),
                    (cluster.getNetwork() == null ? "-" : ellipsize(cluster.getNetwork().getId(), LEN_ID_NET)));
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

    public String toDetailString(String clusterId) {
        Cluster cluster = clusterMap.get(clusterId);
        StringBuilder display = new StringBuilder();
        display.append("Listing detailed Cluster Configuration:\n");
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

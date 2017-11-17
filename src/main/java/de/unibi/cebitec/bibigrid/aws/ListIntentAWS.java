package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import de.unibi.cebitec.bibigrid.intents.ListIntent;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.Configuration;

import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the general ListIntent interface for an AWS based cluster.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ListIntentAWS extends IntentAWS implements ListIntent {
    public static final Logger LOG = LoggerFactory.getLogger(ListIntentAWS.class);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

    private Map<String, Cluster> clusterMap;
    private final Configuration conf;

    ListIntentAWS(final Configuration conf) {
        this.conf = conf;
    }

    @Override
    public Map<String, Cluster> getList() {
        searchClusterIfNecessary();
        return clusterMap;
    }

    public String toString() {
        searchClusterIfNecessary();
        if (clusterMap.isEmpty()) {
            return "No BiBiGrid cluster found!\n";
        }
        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        display.append("\n");
        formatter.format("%15s | %10s | %19s | %20s | %7s | %11s | %11s%n", "cluster-id", "user", "launch date", "key name", "# inst", "group-id", "subnet-id");
        display.append(new String(new char[115]).replace('\0', '-')).append("\n");
        for (String id : clusterMap.keySet()) {
            Cluster v = clusterMap.get(id);
            formatter.format("%15s | %10s | %19s | %20s | %7d | %11s | %11s%n",
                    id,
                    (v.getUser() == null) ? "<NA>" : v.getUser(),
                    (v.getStarted() == null) ? "-" : v.getStarted(),
                    (v.getKeyName() == null ? "-" : v.getKeyName()),
                    ((v.getMasterInstance() != null ? 1 : 0) + v.getSlaveInstances().size()),
                    (v.getSecurityGroup() == null ? "-" : (v.getSecurityGroup().length() > 11 ? v.getSecurityGroup().substring(0, 9) + ".." : v.getSecurityGroup())),
                    (v.getSubnet() == null ? "-" : v.getSubnet()));
        }
        return display.toString();
    }

    private void searchClusterIfNecessary() {
        if (clusterMap != null) {
            return;
        }
        clusterMap = new HashMap<>();
        AmazonEC2Client ec2 = getClient(conf);
        Cluster cluster;

        // Instances
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
        List<Reservation> reservations = describeInstancesResult.getReservations();

        for (Reservation r : reservations) {
            for (Instance i : r.getInstances()) {
                if (i.getState().getName().equals("pending")
                        || i.getState().getName().equals("running")
                        || i.getState().getName().equals("stopping")
                        || i.getState().getName().equals("stopped")) {
                    // check for cluster-id
                    String clusterId = getValueForName(i.getTags(), "bibigrid-id");
                    String name = getValueForName(i.getTags(), "name");
                    String user = getValueForName(i.getTags(), "user");
                    if (clusterId == null && name != null) { // old style : use name to determine clusterId
                        if (name.contains("master-") || name.contains("slave-")) {
                            String[] tmp = name.split("-");
                            clusterId = tmp[tmp.length - 1];
                        }
                    }
                    if (clusterId == null) { // not an bibigrid instance : skip
                        continue;
                    }
                    // check if map contains a value object
                    cluster = clusterMap.containsKey(clusterId) ? clusterMap.get(clusterId) : new Cluster();
                    // master/slave instance ?
                    if (name != null && name.contains("master-")) {
                        if (cluster.getMasterInstance() == null) {
                            cluster.setMasterInstance(i.getInstanceId());
                            cluster.setStarted(dateFormatter.format(i.getLaunchTime()));
                        } else {
                            LOG.error("Detect two master instances ({},{}) for cluster '{}' ",
                                    cluster.getMasterInstance(), i.getInstanceId(), clusterId); // ???
                            System.exit(1);
                        }
                    } else {
                        cluster.addSlaveInstance(i.getInstanceId());
                    }
                    //keyname - should be always the same for all instances of one cluster
                    if (cluster.getKeyName() != null) {
                        if (!cluster.getKeyName().equals(i.getKeyName())) {
                            LOG.error("Detect two different keynames ({},{}) for cluster '{}'",
                                    cluster.getKeyName(), i.getKeyName(), clusterId);
                        }
                    } else {
                        cluster.setKeyName(i.getKeyName());
                    }
                    // user - should be always the same for all instances of one cluster
                    if (user != null) {
                        if (cluster.getUser() == null) {
                            cluster.setUser(user);
                        } else if (!cluster.getUser().equals(user)) {
                            LOG.error("Detect two different users ({},{}) for cluster '{}'",
                                    cluster.getUser(), user, clusterId);
                        }
                    }
                    clusterMap.put(clusterId, cluster);
                }
            }
        }
    }

    private static String getValueForName(List<Tag> tags, String name) {
        for (Tag t : tags) {
            if (t.getKey().equalsIgnoreCase(name)) {
                return t.getValue();
            }
        }
        return null;
    }
}

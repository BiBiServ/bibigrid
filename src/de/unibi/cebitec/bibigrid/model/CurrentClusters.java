package de.unibi.cebitec.bibigrid.model;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribePlacementGroupsRequest;
import com.amazonaws.services.ec2.model.DescribePlacementGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.PlacementGroup;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import de.unibi.cebitec.bibigrid.ctrl.TerminateIntent;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentClusters {

    public static final Logger log = LoggerFactory.getLogger(TerminateIntent.class);

    public static SimpleDateFormat dateformatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

    private Map<String, Cluster> clustermap = new HashMap<>();

    /**
     * Build a map of possible BiBiGrid clusters
     *
     * @param ec2 - a initialized AmAzonEC2Client Instance
     */
    public CurrentClusters(AmazonEC2Client ec2) {
        Cluster cluster;

        // =================================
        // == Instances   
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
                    String clusterid = getValueforName(i.getTags(), "bibigrid-id");
                    String name = getValueforName(i.getTags(), "name");
                    if (clusterid == null && name != null) { // old style : use name to determine clusterid
                        if (name.contains("master-") || name.contains("slave-")) {
                            String[] tmp = name.split("-");
                            clusterid = tmp[tmp.length - 1];
                        }
                    }
                    if (clusterid == null) { // not an bibigrid instance : skip
                        continue;
                    }
                    // check if map contains a value objekt
                    if (clustermap.containsKey(clusterid)) {
                        cluster = clustermap.get(clusterid);
                    } else {
                        cluster = new Cluster();
                    }

                    // master//slave instance ?
                    if (name != null && name.contains("master-")) {
                        if (cluster.getMasterinstance() == null) {
                            cluster.setMasterinstance(i.getInstanceId());

                            cluster.setStarted(dateformatter.format(i.getLaunchTime()));
                        } else {
                            log.error("Detect two master instances ({},{}) for cluster '{}' ", cluster.getMasterinstance(), i.getInstanceId(), clusterid); // ???
                            System.exit(1);
                        }
                    } else {
                        cluster.addSlaveInstance(i.getInstanceId());
                    }

                    //keyname - should be always the same for all instances of one cluster
                    if (cluster.getKeyname() != null) {
                        if (!cluster.getKeyname().equals(i.getKeyName())) {
                            log.error("Detect two different keynames ({},{}) for cluster '{}'", cluster.getKeyname(), i.getKeyName(), clusterid);
                        }

                    } else {
                        cluster.setKeyname(i.getKeyName());
                    }

                    clustermap.put(clusterid, cluster);
                }

            }
        }

        // =================================
        // == Subnets
        DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
        DescribeSubnetsResult describeSubnetResult = ec2.describeSubnets(describeSubnetsRequest);
        for (Subnet s : describeSubnetResult.getSubnets()) {
            String clusterid = getValueforName(s.getTags(), "bibigrid-id");
            String name = getValueforName(s.getTags(), "name");
            if (clusterid == null && name != null) {//old style : use name
                if (name.contains("subnet-")) {
                    String[] tmp = name.split("-");
                    clusterid = tmp[tmp.length - 1];
                }
            }
            if (clusterid == null) { // not an bibigrid subnet : skip
                continue;
            }
            // check if map contains a value objekt
            if (clustermap.containsKey(clusterid)) {
                cluster = clustermap.get(clusterid);
            } else {
                cluster = new Cluster();
            }
            cluster.setSubnet(s.getSubnetId());
            cluster.setVpc(s.getVpcId());

            clustermap.put(clusterid, cluster);
        }

        // =================================
        // == Security Groups
        DescribeSecurityGroupsRequest describeSecurityGroupsRequest = new DescribeSecurityGroupsRequest();
        DescribeSecurityGroupsResult describeSecurityGroupsResult = ec2.describeSecurityGroups(describeSecurityGroupsRequest);
        for (SecurityGroup sg : describeSecurityGroupsResult.getSecurityGroups()) {
            String clusterid = getValueforName(sg.getTags(), "bibigrid-id");
            String name = sg.getGroupName();
            if (clusterid == null && name != null) {//old style : use name
                if (name.contains("bibigrid-")) {
                    String[] tmp = name.split("-");
                    clusterid = tmp[tmp.length - 1];
                }
            }
            if (clusterid == null) { // not an bibigrid security group : skip
                continue;
            }
            // check if map contains a value objekt
            if (clustermap.containsKey(clusterid)) {
                cluster = clustermap.get(clusterid);
            } else {
                cluster = new Cluster();
            }
            cluster.setSecuritygroup(sg.getGroupId());

            clustermap.put(clusterid, cluster);

        }

        // =================================
        // == Placement Groups
        DescribePlacementGroupsRequest describePlacementGroupsRequest = new DescribePlacementGroupsRequest();
        DescribePlacementGroupsResult describePlacementGroupsResult = ec2.describePlacementGroups(describePlacementGroupsRequest);
        for (PlacementGroup pg : describePlacementGroupsResult.getPlacementGroups()) {
            String name = pg.getGroupName();
            String clusterid = null;
            if (name.contains("bibigrid-")) {
                String[] tmp = name.split("-");
                clusterid = tmp[tmp.length - 1];
            }
            if (clusterid == null) { // not an bibigrid placement group : skip
                continue;
            }
            // check if map contains a value objekt
            if (clustermap.containsKey(clusterid)) {
                cluster = clustermap.get(clusterid);
            } else {
                cluster = new Cluster();
            }
            cluster.setPlacementgroup(name);

            clustermap.put(clusterid, cluster);
        }

    }

    public String printClusterList() {
        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        if (clustermap.isEmpty()) {
            display.append("No BiBiGrid cluster found!\n");
        } else {
            display.append("\n");
            formatter.format("%15s | %19s | %15s | %7s | %11s | %11s%n", "cluster-id", "launch date", "key name", "# inst", "group-id", "subnet-id");
            display.append(new String(new char[93]).replace('\0', '-')).append("\n");

            for (String id : clustermap.keySet()) {
                Cluster v = clustermap.get(id);
                formatter.format("%15s | %19s | %15s | %7d | %11s | %11s%n",
                        id,
                        v.getStarted(),
                        (v.getKeyname() == null ? "-" : v.getKeyname()),
                        ((v.getMasterinstance() != null ? 1 : 0) + v.getSlaveinstances().size()),
                        (v.getSecuritygroup() == null ? "-" : v.getSecuritygroup()),
                        (v.getSubnet() == null ? "-" : v.getSubnet()));
            }
        }
        return display.toString();
    }

    public Map<String, Cluster> getClusterMap() {
        return clustermap;
    }

    private static String getValueforName(List<Tag> tags, String name) {
        for (Tag t : tags) {
            if (t.getKey().equalsIgnoreCase(name)) {
                return t.getValue();
            }
        }
        return null;
    }

}

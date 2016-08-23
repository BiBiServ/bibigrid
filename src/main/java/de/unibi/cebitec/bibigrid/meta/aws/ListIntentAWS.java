/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.aws;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import de.unibi.cebitec.bibigrid.ctrl.TerminateIntent;
import de.unibi.cebitec.bibigrid.meta.ListIntent;
import de.unibi.cebitec.bibigrid.meta.openstack.ListIntentOpenstack;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ListIntentAWS implements ListIntent {

    public static final Logger log = LoggerFactory.getLogger(TerminateIntent.class);
    private static final SimpleDateFormat dateformatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

    private AmazonEC2Client ec2;
    private final Map<String, Cluster> clustermap = new HashMap<>();
    private final Configuration conf;

    public ListIntentAWS(final Configuration conf) {
        this.conf = conf;
        ec2 = new AmazonEC2Client(conf.getCredentials());
        ec2.setEndpoint("ec2." + conf.getRegion() + ".amazonaws.com");
        searchCluster();
    }

   
    
    
    
    

   

    @Override
    public Map<String, Cluster> getList() {
        return clustermap;
    }
    
    public String toString() {
        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        if (clustermap.isEmpty()) {
            display.append("No BiBiGrid cluster found!\n");
        } else {
            display.append("\n");
            formatter.format("%15s | %10s | %19s | %20s | %7s | %11s | %11s%n", "cluster-id", "user", "launch date", "key name", "# inst", "group-id", "subnet-id");
            display.append(new String(new char[115]).replace('\0', '-')).append("\n");

            for (String id : clustermap.keySet()) {
                Cluster v = clustermap.get(id);
                formatter.format("%15s | %10s | %19s | %20s | %7d | %11s | %11s%n",
                        id,
                        (v.getUser() == null) ? "<NA>" : v.getUser(),
                        (v.getStarted() == null) ? "-" : v.getStarted(),
                        (v.getKeyname() == null ? "-" : v.getKeyname()),
                        ((v.getMasterinstance() != null ? 1 : 0) + v.getSlaveinstances().size()),
                        (v.getSecuritygroup() == null ? "-" : (v.getSecuritygroup().length() > 11 ? v.getSecuritygroup().substring(0, 9) + ".." : v.getSecuritygroup())),
                        (v.getSubnet() == null ? "-" : v.getSubnet()));
            }
        }
        return display.toString();
    }

     private ListIntentAWS searchCluster() {
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
                    String user = getValueforName(i.getTags(), "user");
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

                    // user - should be always the same for all instances of one cluser
                    if (user != null) {
                        if (cluster.getUser() == null) {
                            cluster.setUser(user);
                        } else if (!cluster.getUser().equals(user)) {
                            log.error("Detect two different users ({},{}) for cluster '{}'", cluster.getUser(), user, clusterid);
                        }
                    }

                    clustermap.put(clusterid, cluster);
                }

            }

        }
        return this;
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

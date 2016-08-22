
package de.unibi.cebitec.bibigrid.meta.aws;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeletePlacementGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import static de.unibi.cebitec.bibigrid.ctrl.TerminateIntent.log;
import de.unibi.cebitec.bibigrid.meta.TerminateIntent;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.util.List;
import java.util.Map;

/**
 * AWS specific implementation of terminate intent.
 * 
 * 
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 *         Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class TerminateIntentAWS implements TerminateIntent {

    private final Configuration conf;
    private final Map<String, Cluster> clustermap;
    private final AmazonEC2Client ec2;

    public TerminateIntentAWS(final Configuration conf) {
        this.conf = conf;
        
        ec2 = new AmazonEC2Client(conf.getCredentials());
        ec2.setEndpoint("ec2." + conf.getRegion() + ".amazonaws.com");
        clustermap = new ListIntentAWS(conf).getList();
        
    }

    @Override
    public boolean terminate() {
        // check if cluster with given id exists
        if (!clustermap.containsKey(conf.getClusterId())) {
            log.error("Cluster with '{}' not found.");
            return false;
        }

        Cluster cluster = clustermap.get(conf.getClusterId());

        ////////////////////////////////////////////////////////////////////////
        ///// terminate instance(s)
        List<String> instances = cluster.getSlaveinstances();
        if (cluster.getMasterinstance() != null) {
            instances.add(cluster.getMasterinstance());
        }
        if (instances.size() > 0) {
            TerminateInstancesRequest terminateInstanceRequest = new TerminateInstancesRequest();
            terminateInstanceRequest.setInstanceIds(instances);
            TerminateInstancesResult terminateInstanceResult = ec2.terminateInstances(terminateInstanceRequest);

            log.info("Wait for instances to shut down. This can take a while, so please be patient!");
            do {
//                DescribeInstanceStatusRequest describeInstanceStatusRequest = new  DescribeInstanceStatusRequest();
//                describeInstanceStatusRequest.setInstanceIds(instances);

                DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
                describeInstancesRequest.setInstanceIds(instances);

                DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
                boolean allterminated = true;
                for (Reservation r : describeInstancesResult.getReservations()) {
                    List<Instance> li = r.getInstances();
                    for (Instance i : li) {
                        log.info(V, "Instance {} {}", i.getInstanceId(), i.getState().getName());
                        allterminated = allterminated & i.getState().getName().equals("terminated");
                    }
                }

                if (allterminated) {
                    break;
                }

                // wait until instances are shut down
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    log.error("Can't sleep.");
                }

            } while (true);

           // log.info("Instance(s) ({}) terminated.",join(",", instances));
        }

        ////////////////////////////////////////////////////////////////////////
        ///// terminate placement group
        if (cluster.getPlacementgroup() != null) {
            DeletePlacementGroupRequest deletePalacementGroupRequest = new DeletePlacementGroupRequest();
            deletePalacementGroupRequest.setGroupName(cluster.getPlacementgroup());
            ec2.deletePlacementGroup(deletePalacementGroupRequest);
            log.info("PlacementGroup terminated.");
        }

        ////////////////////////////////////////////////////////////////////////
        ///// terminate subnet
        if (cluster.getSubnet() != null) {
            DeleteSubnetRequest deleteSubnetRequest = new DeleteSubnetRequest();
            deleteSubnetRequest.setSubnetId(cluster.getSubnet());
            ec2.deleteSubnet(deleteSubnetRequest);
            log.info("Subnet terminated.");
        }

        ////////////////////////////////////////////////////////////////////////
        ///// terminate security group
        if (cluster.getSecuritygroup() != null) {
            DeleteSecurityGroupRequest deleteSecurityGroupRequest = new DeleteSecurityGroupRequest();
            deleteSecurityGroupRequest.setGroupId(cluster.getSecuritygroup());
            ec2.deleteSecurityGroup(deleteSecurityGroupRequest);
        }

        log.info("Cluster '{}' terminated!", conf.getClusterId());
        return true;
    }

}

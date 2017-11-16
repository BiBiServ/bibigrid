package de.unibi.cebitec.bibigrid.aws;

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
import de.unibi.cebitec.bibigrid.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;

import java.util.List;
import java.util.Map;

/**
 * AWS specific implementation of terminate intent.
 * 
 * 
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class TerminateIntentAWS implements TerminateIntent {
    public static final Logger LOG = LoggerFactory.getLogger(TerminateIntentAWS.class);
    private final Configuration conf;

    TerminateIntentAWS(final Configuration conf) {
        this.conf = conf;
    }

    @Override
    public boolean terminate() {
        AmazonEC2Client ec2 = new AmazonEC2Client(conf.getCredentials());
        ec2.setEndpoint("ec2." + conf.getRegion() + ".amazonaws.com");
        Map<String, Cluster> clusterMap = new ListIntentAWS(conf).getList();
        // check if cluster with given id exists
        if (!clusterMap.containsKey(conf.getClusterId())) {
            LOG.error("Cluster with '{}' not found.");
            return false;
        }
        Cluster cluster = clusterMap.get(conf.getClusterId());
        // terminate instance(s)
        List<String> instances = cluster.getSlaveInstances();
        if (cluster.getMasterInstance() != null) {
            instances.add(cluster.getMasterInstance());
        }
        if (instances.size() > 0) {
            TerminateInstancesRequest terminateInstanceRequest = new TerminateInstancesRequest();
            terminateInstanceRequest.setInstanceIds(instances);
            TerminateInstancesResult terminateInstanceResult = ec2.terminateInstances(terminateInstanceRequest);

            LOG.info("Wait for instances to shut down. This can take a while, so please be patient!");
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
                        LOG.info(V, "Instance {} {}", i.getInstanceId(), i.getState().getName());
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
                    LOG.error("Can't sleep.");
                }
            } while (true);
           // log.info("Instance(s) ({}) terminated.",join(",", instances));
        }
        // terminate placement group
        if (cluster.getPlacementGroup() != null) {
            DeletePlacementGroupRequest deletePalacementGroupRequest = new DeletePlacementGroupRequest();
            deletePalacementGroupRequest.setGroupName(cluster.getPlacementGroup());
            ec2.deletePlacementGroup(deletePalacementGroupRequest);
            LOG.info("PlacementGroup terminated.");
        }
        // terminate subnet
        if (cluster.getSubnet() != null) {
            DeleteSubnetRequest deleteSubnetRequest = new DeleteSubnetRequest();
            deleteSubnetRequest.setSubnetId(cluster.getSubnet());
            ec2.deleteSubnet(deleteSubnetRequest);
            LOG.info("Subnet terminated.");
        }
        // terminate security group
        if (cluster.getSecurityGroup() != null) {
            DeleteSecurityGroupRequest deleteSecurityGroupRequest = new DeleteSecurityGroupRequest();
            deleteSecurityGroupRequest.setGroupId(cluster.getSecurityGroup());
            ec2.deleteSecurityGroup(deleteSecurityGroupRequest);
        }
        LOG.info("Cluster '{}' terminated!", conf.getClusterId());
        return true;
    }
}

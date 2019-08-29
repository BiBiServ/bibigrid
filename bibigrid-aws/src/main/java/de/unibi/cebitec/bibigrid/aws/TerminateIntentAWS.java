package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DeletePlacementGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS specific implementation of terminate intent.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class TerminateIntentAWS extends TerminateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntentAWS.class);

    TerminateIntentAWS(ProviderModule providerModule, Client client, Configuration config) {
        super(providerModule, client, config);
    }

    @Override
    protected boolean terminateCluster(Cluster cluster) {
        final AmazonEC2 ec2 = ((ClientAWS) client).getInternal();
        terminateInstances(ec2, cluster);
        terminatePlacementGroup(ec2, cluster);
        terminateSubnet(ec2, cluster);
        terminateSecurityGroup(ec2, cluster);
        return true;
    }

    private void terminateInstances(final AmazonEC2 ec2, final Cluster cluster) {
        List<String> instanceIds = new ArrayList<>();
        if (cluster.getMasterInstance() != null) {
            instanceIds.add(cluster.getMasterInstance().getId());
        }
        if (cluster.getWorkerInstances() != null) {
            for (de.unibi.cebitec.bibigrid.core.model.Instance instance : cluster.getWorkerInstances()) {
                instanceIds.add(instance.getId());
            }
        }
        if (instanceIds.size() > 0) {
            TerminateInstancesRequest terminateInstanceRequest = new TerminateInstancesRequest();
            terminateInstanceRequest.setInstanceIds(instanceIds);
            ec2.terminateInstances(terminateInstanceRequest);
            LOG.info("Waiting for instances to shut down. This might take a while.");
            do {
                DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
                describeInstancesRequest.setInstanceIds(instanceIds);
                DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
                boolean allTerminated = true;
                for (Reservation reservation : describeInstancesResult.getReservations()) {
                    for (Instance instance : reservation.getInstances()) {
                        LOG.info(V, "Instance {} {}", instance.getInstanceId(), instance.getState().getName());
                        allTerminated &= instance.getState().getName().equals("terminated");
                    }
                }
                if (allTerminated) {
                    break;
                }
                // wait until instances are shut down
                sleep(5);
            } while (true);
            // log.info("Instance(s) ({}) terminated.",join(",", instances));
        }
    }

    private void terminatePlacementGroup(final AmazonEC2 ec2, final Cluster cluster) {
        if (cluster.getPlacementGroup() != null) {
            DeletePlacementGroupRequest deletePlacementGroupRequest = new DeletePlacementGroupRequest();
            deletePlacementGroupRequest.setGroupName(cluster.getPlacementGroup());
            ec2.deletePlacementGroup(deletePlacementGroupRequest);
            LOG.info("PlacementGroup terminated.");
        }
    }

    private void terminateSubnet(final AmazonEC2 ec2, final Cluster cluster) {
        if (cluster.getSubnet() != null) {
            DeleteSubnetRequest deleteSubnetRequest = new DeleteSubnetRequest();
            deleteSubnetRequest.setSubnetId(cluster.getSubnet().getId());
            ec2.deleteSubnet(deleteSubnetRequest);
            LOG.info("Subnet terminated.");
        }
    }

    private void terminateSecurityGroup(final AmazonEC2 ec2, final Cluster cluster) {
        if (cluster.getSecurityGroup() != null) {
            DeleteSecurityGroupRequest deleteSecurityGroupRequest = new DeleteSecurityGroupRequest();
            deleteSecurityGroupRequest.setGroupId(cluster.getSecurityGroup());
            ec2.deleteSecurityGroup(deleteSecurityGroupRequest);
            LOG.info("Security group terminated.");
        }
    }
}

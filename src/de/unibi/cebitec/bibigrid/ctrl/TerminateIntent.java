package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.model.CurrentClusters;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminateIntent extends Intent {

    public static final Logger log = LoggerFactory.getLogger(TerminateIntent.class);

    @Override
    public String getCmdLineOption() {
        return "t";
    }

    @Override
    public List<String> getRequiredOptions() {
        return Arrays.asList(new String[]{"t", "e", "a"});
    }

    @Override
    public boolean execute() throws IntentNotConfiguredException {
        if (getConfiguration() == null) {
            throw new IntentNotConfiguredException();
        }

        try {
            AmazonEC2 ec2 = new AmazonEC2Client(this.getConfiguration().getCredentials());
            ec2.setEndpoint(this.getConfiguration().getEndpoint());

            if (!CurrentClusters.exists(this.getConfiguration().getClusterId())) {
                log.error("Invalid cluster-id!");
                return false;
            }
            String masterReservationId = CurrentClusters.getMasterReservationId(this.getConfiguration().getClusterId());
            String slaveReservationId = CurrentClusters.getSlaveReservationId(this.getConfiguration().getClusterId());

            log.info("Terminating cluster with id: {}", this.getConfiguration().getClusterId());

            log.info("Gathering information about the instances involved ...");
            DescribeInstancesRequest descrReq = new DescribeInstancesRequest();
            DescribeInstancesResult descrReqRes = ec2.describeInstances(descrReq);
            List<Reservation> allReservations = descrReqRes.getReservations();
            List<Reservation> reservationsToTerminate = new ArrayList<>();
            for (Reservation r : allReservations) {
                if (r.getReservationId().equals(masterReservationId) || r.getReservationId().equals(slaveReservationId)) {
                    reservationsToTerminate.add(r);
                }
            }

            List<String> instanceIdsToTerminate = new ArrayList<>();
            for (Reservation r : reservationsToTerminate) {
                for (Instance i : r.getInstances()) {
                    if (i.getState().getName().equals(InstanceStateName.Running.toString())) {
                        log.debug("Adding instance {} to termination list.", i.getInstanceId());
                        instanceIdsToTerminate.add(i.getInstanceId());
                    }
                }
            }

            //if cluster has already been terminated (e.g. by hand) the only thing left is to delete it from preferences
            if (instanceIdsToTerminate.isEmpty()) {
                log.info("Looks like this cluster has already been terminated. Removing it from the list ...");
                CurrentClusters.removeCluster(this.getConfiguration().getClusterId());
                return true;
            }
            DescribePlacementGroupsRequest descrPgGroup = new DescribePlacementGroupsRequest().withGroupNames(CreateIntent.PLACEMENT_GROUP_PREFIX + this.getConfiguration().getClusterId());

            DescribePlacementGroupsResult descrPgGroupResult = ec2.describePlacementGroups(descrPgGroup);
            if (!descrPgGroupResult.getPlacementGroups().isEmpty()) {
                log.info("Deleting placement group.");
                DeletePlacementGroupRequest pgTermReq = new DeletePlacementGroupRequest(CreateIntent.PLACEMENT_GROUP_PREFIX + this.getConfiguration().getClusterId());
                ec2.deletePlacementGroup(pgTermReq);
            }
            TerminateInstancesRequest termReq = new TerminateInstancesRequest();
            termReq.withInstanceIds(instanceIdsToTerminate);
            TerminateInstancesResult termReqRes = ec2.terminateInstances(termReq);

            log.info("Waiting for {} instances to terminate ...", instanceIdsToTerminate.size());
            do {
                DescribeInstancesRequest descrTerminatingReq = new DescribeInstancesRequest();
                descrTerminatingReq.withInstanceIds(instanceIdsToTerminate);
                DescribeInstancesResult descrTerminatingReqRes = ec2.describeInstances(descrTerminatingReq);

                boolean allTerminated = true;
                for (Reservation r : descrTerminatingReqRes.getReservations()) {
                    for (Instance i : r.getInstances()) {
                        if (!i.getState().getName().equals(InstanceStateName.Terminated.toString())) {
                            allTerminated = false;
                        }
                    }
                }
                if (allTerminated) {
                    break;
                }
                sleep(10);
                log.info("...");
            } while (true);
            sleep(5);

            log.info("Deleting security group ...");
            DeleteSecurityGroupRequest delSecReq = new DeleteSecurityGroupRequest();
            delSecReq.setGroupName(CreateIntent.SECURITY_GROUP_PREFIX + this.getConfiguration().getClusterId());
            ec2.deleteSecurityGroup(delSecReq);
            log.info(I, "Cluster terminated. ({})", this.getConfiguration().getClusterId());

            CurrentClusters.removeCluster(this.getConfiguration().getClusterId());

        } catch (AmazonClientException ace) {
            log.error("{}", ace);
            return false;
        }
        return true;
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ie) {
            log.error("Thread.sleep interrupted!");
        }
    }
}
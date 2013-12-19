package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.model.Filter;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.model.CurrentClusters;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import de.unibi.cebitec.bibigrid.util.VerboseOutputFilter;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
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
            ec2.setEndpoint("ec2." + this.getConfiguration().getRegion() + ".amazonaws.com");
            String clusterId = "";
            String masterReservationId = "";
            String autoScalingGroup = "";
            if (!CurrentClusters.exists(this.getConfiguration().getClusterId())) {
                log.error("Can't find local information about cluster!");
                log.info("Trying to find running clusters.");
                DescribeInstancesRequest descrReq = new DescribeInstancesRequest();
                
                Filter idFilter = new Filter("tag:bibigrid-id",Arrays.asList(this.getConfiguration().getClusterId()));          
                List<Filter> filters = new ArrayList<>();
                filters.add(idFilter);
                descrReq.setFilters(filters);
                DescribeInstancesResult res = ec2.describeInstances(descrReq);

                Reservation reservation = getClusterIdFromTags(res);
                if (reservation == null) {
                    log.error("There is no cluster with such ID");
                    return false;
                } else {
                    log.info("Id has been found.");
                    for (Instance e : reservation.getInstances()) {
                        for (com.amazonaws.services.ec2.model.Tag tag : e.getTags()) {
                            if (tag.getKey().equals("bibigrid-id")) {
                                clusterId = tag.getValue();
                            }
                        }
                    }
                    masterReservationId = reservation.getReservationId();
                    autoScalingGroup = "as_group-" + clusterId;
                    log.info("Will now attempt shutdown.");
                }

            } else {
                clusterId = this.getConfiguration().getClusterId();
                masterReservationId = CurrentClusters.getMasterReservationId(clusterId);
                autoScalingGroup = CurrentClusters.getAutoScalingGroupName(clusterId);

            }
            log.info("Terminating cluster with id: {}", clusterId);

            log.info("Gathering information about the instances involved ...");
            DescribeInstancesRequest descrReq = new DescribeInstancesRequest();
            DescribeInstancesResult descrReqRes = ec2.describeInstances(descrReq);
            List<Reservation> allReservations = descrReqRes.getReservations();
            List<Reservation> reservationsToTerminate = new ArrayList<>();
            for (Reservation r : allReservations) {
                if (r.getReservationId().equals(masterReservationId)) {
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

            // cluster has already been terminated (e.g. by hand) the only thing left is to delete it from preferences

            TerminateInstancesRequest termReq = new TerminateInstancesRequest();
            termReq.withInstanceIds(instanceIdsToTerminate);
            TerminateInstancesResult termReqRes = ec2.terminateInstances(termReq);

            AmazonAutoScaling as = new AmazonAutoScalingClient(this.getConfiguration().getCredentials());
            as.setEndpoint("autoscaling." + this.getConfiguration().getRegion() + ".amazonaws.com");



            DescribeAutoScalingGroupsResult describeResult = as.describeAutoScalingGroups();


            if (!describeResult.getAutoScalingGroups().isEmpty()) {
                boolean found = false;
                for (AutoScalingGroup e : describeResult.getAutoScalingGroups()) {
                    if (e.getAutoScalingGroupName().equals("as_group-" + clusterId)) {
                        log.info("AutoScaling group found.");
                        found = true;
                        break;
                    }
                }

                log.info("Removing policies...");
                DescribePoliciesResult policyResult = as.describePolicies(new DescribePoliciesRequest().withAutoScalingGroupName(autoScalingGroup));

                for (ScalingPolicy sp : policyResult.getScalingPolicies()) {
                    as.deletePolicy(new DeletePolicyRequest().withPolicyName(sp.getPolicyARN()));
                    sleep(1);
                }




                log.info("The instances are shutting down. Please be patient. This can take a moment...");
                if (found) {
                    UpdateAutoScalingGroupRequest shutDownRequest = new UpdateAutoScalingGroupRequest().withAutoScalingGroupName(autoScalingGroup).withMinSize(0).withMaxSize(0);



                    as.updateAutoScalingGroup(shutDownRequest);
                }


                DescribeAutoScalingGroupsResult result;


                while (true) {
                    result = as.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroup));

                    if (!result.getAutoScalingGroups().isEmpty()) {

                        if (result.getAutoScalingGroups().get(0).getInstances().isEmpty()) {
                            log.info(I, "All instances have been shutdown");
                            break;
                        } else {
                            log.info(VerboseOutputFilter.V, "Instances still existing");
                            sleep(10);
                        }

                    }
                }
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
                        log.info(I, "Master terminated");
                        break;
                    }
                    sleep(10);
                    log.info("...");
                } while (true);
                sleep(5);



                log.info(V, "Suspending processes.");
                as.suspendProcesses(new SuspendProcessesRequest().withAutoScalingGroupName(autoScalingGroup));
                DeleteAutoScalingGroupRequest shutdown = new DeleteAutoScalingGroupRequest().withForceDelete(true).withAutoScalingGroupName(autoScalingGroup);
                as.deleteAutoScalingGroup(shutdown);
                sleep(5);
                log.info(I, "AutoScaling group deleted");

            }
            DescribeLaunchConfigurationsResult launchConfigResult = as.describeLaunchConfigurations(new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(clusterId + "-config"));

            if (!launchConfigResult.getLaunchConfigurations().isEmpty()) {
                as.deleteLaunchConfiguration(new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(clusterId + "-config"));

                DescribeLaunchConfigurationsResult res = as.describeLaunchConfigurations(new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(clusterId + "-onSpot"));

                if (!res.getLaunchConfigurations().isEmpty()) {
                    as.deleteLaunchConfiguration(new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(clusterId + "-onSpot"));
                }
                sleep(5);
            }
            log.info("Deleting security group ...");

            DeleteSecurityGroupRequest delSecReq = new DeleteSecurityGroupRequest();
            delSecReq.setGroupName(CreateIntent.SECURITY_GROUP_PREFIX + clusterId);
            ec2.deleteSecurityGroup(delSecReq);
            log.info(I, "Security Group deleted.");

            DescribePlacementGroupsRequest descrPgGroup = new DescribePlacementGroupsRequest().withGroupNames(CreateIntent.PLACEMENT_GROUP_PREFIX + clusterId);

            DescribePlacementGroupsResult descrPgGroupResult = ec2.describePlacementGroups(descrPgGroup);
            
            if (!descrPgGroupResult.getPlacementGroups().isEmpty()) {
                log.info("Deleting placement group.");
                DeletePlacementGroupRequest pgTermReq = new DeletePlacementGroupRequest(CreateIntent.PLACEMENT_GROUP_PREFIX + clusterId);
                ec2.deletePlacementGroup(pgTermReq);
            }


            log.info(I, "Cluster terminated. ({})", clusterId);


            if (CurrentClusters.exists(clusterId)) {
                CurrentClusters.removeCluster(clusterId);
            }

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

    private Reservation getClusterIdFromTags(DescribeInstancesResult res) {
        Reservation returnReservation = null;
        for (Reservation e : res.getReservations()) {
            for (Instance f : e.getInstances()) {
                for (com.amazonaws.services.ec2.model.Tag tag : f.getTags()) {
                    if (tag.getValue().equals(this.getConfiguration().getClusterId())) {
                        return e;
                    }
                    
                }
                System.out.println(f.getInstanceId());
            }
        }
        return returnReservation;
    }
}

package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeletePolicyRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.DescribePoliciesRequest;
import com.amazonaws.services.autoscaling.model.DescribePoliciesResult;
import com.amazonaws.services.autoscaling.model.ScalingPolicy;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
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
            String useGluster = "";
            String glusterReservationId = "";
            if (!CurrentClusters.exists(this.getConfiguration().getClusterId())) {
                log.error("Can't find local information about cluster!");
                log.info("Trying to find running clusters.");
                DescribeInstancesRequest descrReq = new DescribeInstancesRequest();

                Filter idFilter = new Filter("tag:bibigrid-id", Arrays.asList(this.getConfiguration().getClusterId()));
                List<Filter> filters = new ArrayList<>();
                filters.add(idFilter);
                descrReq.setFilters(filters);
                DescribeInstancesResult res = ec2.describeInstances(descrReq);

                Reservation reservation = getClusterIdFromTags(res);
                
                //gluster
                DescribeInstancesRequest descrReqGluster = new DescribeInstancesRequest();
                Filter idFilterGluster = new Filter("tag:bibigrid-gluster", Arrays.asList(this.getConfiguration().getClusterId()));
                List<Filter> filtersGluster = new ArrayList<>();
                filtersGluster.add(idFilterGluster);
                descrReqGluster.setFilters(filtersGluster);
                DescribeInstancesResult resGluster = ec2.describeInstances(descrReqGluster);

                Reservation reservationGluster = getClusterIdFromTags(resGluster);
                //--------
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
                    if (reservationGluster != null){
                        useGluster = String.valueOf(true);
                        glusterReservationId = reservationGluster.getReservationId();
                    }
                    log.info("Will now attempt shutdown.");
                }

            } else {
                clusterId = this.getConfiguration().getClusterId();
                masterReservationId = CurrentClusters.getMasterReservationId(clusterId);
                autoScalingGroup = CurrentClusters.getAutoScalingGroupName(clusterId);
                useGluster = CurrentClusters.getUseGluster(clusterId);
                glusterReservationId = CurrentClusters.getGlusterReservationId(clusterId);
            }
            
            if (useGluster.equals(String.valueOf(true))){
                return terminateGluster(ec2, clusterId, masterReservationId, autoScalingGroup, glusterReservationId);
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
            if (reservationsToTerminate.isEmpty()) {
                log.error("No reservations found. Cluster-ID seems to be outdated.");
                CurrentClusters.removeCluster(clusterId);
                return false;
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
            if (!instanceIdsToTerminate.isEmpty()) {
                TerminateInstancesRequest termReq = new TerminateInstancesRequest();
                termReq.withInstanceIds(instanceIdsToTerminate);
                TerminateInstancesResult termReqRes = ec2.terminateInstances(termReq);
            } else {
                log.info(V, "Master instance already terminated.");
            }
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

            DescribeSecurityGroupsResult describeSecurityGrpResult = ec2.describeSecurityGroups();
            boolean secGroupExists = false;
            for (SecurityGroup e : describeSecurityGrpResult.getSecurityGroups()) {
                if (e.getGroupName().equals(CreateIntent.SECURITY_GROUP_PREFIX + clusterId)) {
                    secGroupExists = true;
                }
            }
            if (secGroupExists) {
                DeleteSecurityGroupRequest delSecReq = new DeleteSecurityGroupRequest().withGroupName(CreateIntent.SECURITY_GROUP_PREFIX + clusterId);
                ec2.deleteSecurityGroup(delSecReq);
                log.info(V, "Attempting to delete security group...");
            }
            log.info(I, "Security Group deleted.");

            sleep(1);

            DescribePlacementGroupsResult descrPgGroupResult = ec2.describePlacementGroups();
            boolean placementgroupExists = false;

            for (PlacementGroup e : descrPgGroupResult.getPlacementGroups()) {
                if (e.getGroupName().equals(CreateIntent.PLACEMENT_GROUP_PREFIX + clusterId)) {
                    placementgroupExists = true;
                }
            }
            if (placementgroupExists) {
                DeletePlacementGroupRequest pgTermReq = new DeletePlacementGroupRequest(CreateIntent.PLACEMENT_GROUP_PREFIX + clusterId);
                ec2.deletePlacementGroup(pgTermReq);
                log.info(I, "Deleting placement group...");
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

    private boolean terminateGluster(AmazonEC2 ec2, String clusterId, String masterReservationId, String autoScalingGroup, String glusterReservationId) {
        try {
            log.info("Terminating cluster with id: {}", clusterId);

            log.info("Gathering information about the instances involved ...");
            DescribeInstancesRequest descrReq = new DescribeInstancesRequest();
            DescribeInstancesResult descrReqRes = ec2.describeInstances(descrReq);
            List<Reservation> allReservations = descrReqRes.getReservations();
            List<Reservation> reservationsToTerminate = new ArrayList<>();
            for (Reservation r : allReservations) {
                if (r.getReservationId().equals(masterReservationId) || r.getReservationId().equals(glusterReservationId)) {
                    reservationsToTerminate.add(r);
                }
            }
            if (reservationsToTerminate.isEmpty()) {
                log.error("No reservations found. Cluster-ID seems to be outdated.");
                CurrentClusters.removeCluster(clusterId);
                return false;
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
            if (!instanceIdsToTerminate.isEmpty()) {
                TerminateInstancesRequest termReq = new TerminateInstancesRequest();
                termReq.withInstanceIds(instanceIdsToTerminate);
                TerminateInstancesResult termReqRes = ec2.terminateInstances(termReq);
            } else {
                log.info(V, "Master and gluster node instances already terminated.");
            }
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
                            log.info(I, "All slave instances have been shutdown");
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
                        log.info(I, "Master and gluster nodes terminated");
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
  
            
            
            DescribeVpcsResult describeVpcsResult = ec2.describeVpcs();
            Vpc vpc = null;

            for (Vpc e : describeVpcsResult.getVpcs()) {
                for (Tag t : e.getTags()){
                    if (t.getKey().equals("Name") && t.getValue().equals("vpc-" + clusterId)){
                        vpc = e;
                        break;
                    }
                }
            }
            if (vpc != null) {
                //internetGateway
                DescribeInternetGatewaysResult igRes = ec2.describeInternetGateways();
                InternetGateway igToDel = null;
                for (InternetGateway i : igRes.getInternetGateways()){
                    if (i.getAttachments().size() > 0 && i.getAttachments().get(0).getVpcId().equals(vpc.getVpcId())){
                        igToDel = i;
                        break;
                    }
                }
                if (igToDel != null){
                    ec2.detachInternetGateway(new DetachInternetGatewayRequest()
                            .withInternetGatewayId(igToDel.getInternetGatewayId())
                            .withVpcId(vpc.getVpcId()));
                    sleep(1);
                    ec2.deleteInternetGateway(new DeleteInternetGatewayRequest()
                            .withInternetGatewayId(igToDel.getInternetGatewayId()));
                    log.info(V, "Internet Gateway deleted.");
                }
                //get Subnet
                DescribeSubnetsResult descSubnetRes = ec2.describeSubnets();
                Subnet subnetToDel = null;
                for (Subnet s : descSubnetRes.getSubnets()){
                    if(s.getVpcId().equals(vpc.getVpcId())){
                        subnetToDel = s;
                        break;
                    }
                }


                //Route Tables
                if (subnetToDel != null){
                    DescribeRouteTablesResult descRouteTableRes = ec2.describeRouteTables();
                    RouteTable routeTableToDelete = null;
                    for (RouteTable r : descRouteTableRes.getRouteTables()){
                        if (r.getAssociations().size() > 0 
                                && r.getAssociations().get(0).getSubnetId() != null
                                && r.getAssociations().get(0).getSubnetId().equals(subnetToDel.getSubnetId())){
                            routeTableToDelete = r;
                            break;
                        }
                    }
                    if (routeTableToDelete != null){
                        ec2.disassociateRouteTable(new DisassociateRouteTableRequest()
                                .withAssociationId(routeTableToDelete.getAssociations().get(0).getRouteTableAssociationId()));
                        sleep(1);
                        ec2.deleteRouteTable(new DeleteRouteTableRequest()
                                .withRouteTableId(routeTableToDelete.getRouteTableId()));
                        log.info(V, "Route Table deleted.");
                    }
                }

                //Sec Group
                log.info(V, "Deleting security group ...");
                sleep(5);
                DescribeSecurityGroupsResult describeSecurityGrpResult = ec2.describeSecurityGroups();
                SecurityGroup secGroupToDel = null;
                for (SecurityGroup e : describeSecurityGrpResult.getSecurityGroups()) {
                    if (e.getGroupName().equals(CreateIntent.SECURITY_GROUP_PREFIX + clusterId)) {
                        secGroupToDel = e;
                    }
                }
                if (secGroupToDel != null) {
                    DeleteSecurityGroupRequest delSecReq = new DeleteSecurityGroupRequest().withGroupId(secGroupToDel.getGroupId());
                    ec2.deleteSecurityGroup(delSecReq);
                    log.info(V, "Attempting to delete security group...");
                }
                log.info(V, "Security Group deleted.");

                //delete Subnets
                if (subnetToDel != null){
                    ec2.deleteSubnet(new DeleteSubnetRequest(subnetToDel.getSubnetId()));
                    log.info(V, "Subnet deleted.");
                }


                //vpc
                DeleteVpcRequest delVpcReq = new DeleteVpcRequest(vpc.getVpcId());
                ec2.deleteVpc(delVpcReq);
                log.info(V, "Attempting to delete vpc and stuff...");
            }
            log.info(I, "vpc deleted");
            
            
            /*
            log.info("Deleting security group ...");

            DescribeSecurityGroupsResult describeSecurityGrpResult = ec2.describeSecurityGroups();
            boolean secGroupExists = false;
            for (SecurityGroup e : describeSecurityGrpResult.getSecurityGroups()) {
                if (e.getGroupName().equals(CreateIntent.SECURITY_GROUP_PREFIX + clusterId)) {
                    secGroupExists = true;
                }
            }
            if (secGroupExists) {
                DeleteSecurityGroupRequest delSecReq = new DeleteSecurityGroupRequest().withGroupName(CreateIntent.SECURITY_GROUP_PREFIX + clusterId);
                ec2.deleteSecurityGroup(delSecReq);
                log.info(V, "Attempting to delete security group...");
            }
            log.info(I, "Security Group deleted.");
*/
            sleep(5);

            DescribePlacementGroupsResult descrPgGroupResult = ec2.describePlacementGroups();
            boolean placementgroupExists = false;

            for (PlacementGroup e : descrPgGroupResult.getPlacementGroups()) {
                if (e.getGroupName().equals(CreateIntent.PLACEMENT_GROUP_PREFIX + clusterId)) {
                    placementgroupExists = true;
                }
            }
            if (placementgroupExists) {
                DeletePlacementGroupRequest pgTermReq = new DeletePlacementGroupRequest(CreateIntent.PLACEMENT_GROUP_PREFIX + clusterId);
                ec2.deletePlacementGroup(pgTermReq);
                log.debug(V, "Attempting to delete placement group...");
            }
            log.info(I, "Placement group deleted.");
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
}

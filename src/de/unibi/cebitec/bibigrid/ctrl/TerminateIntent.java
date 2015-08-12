package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeletePlacementGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.CurrentClusters;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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
        ////////////////////////////////////////////////////////////////////////
        ///// create client 
        AmazonEC2Client ec2 = new AmazonEC2Client(this.getConfiguration().getCredentials());
        ec2.setEndpoint("ec2." + this.getConfiguration().getRegion() + ".amazonaws.com");

        ////////////////////////////////////////////////////////////////////////
        ///// create currentclusters
        CurrentClusters cc = new CurrentClusters(ec2);

        Map<String, Cluster> clustermap = cc.getClusterMap();

        if (!clustermap.containsKey(this.getConfiguration().getClusterId())) {
            log.error("Cluster with '{}' not found.");
            return false;
        }

        Cluster cluster = clustermap.get(this.getConfiguration().getClusterId());

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
            
            do {
                if (terminateInstanceResult.break;
                
                
                log.info("Wait for instances to shut down.");
                 // wait until instances are shut down
                try {  
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    log.error("Can't sleep.");
                }
                
                
                
            } while (true);
            
            log.info("Instance(s) ({}) terminated.",String.join(",", instances));
            
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



        log.info("Cluster '{}' terminated!", this.getConfiguration().getClusterId());

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

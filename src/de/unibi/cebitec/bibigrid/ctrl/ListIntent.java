package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.services.ec2.AmazonEC2Client;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.model.CurrentClusters;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListIntent extends Intent {

    public static final Logger log = LoggerFactory.getLogger(ListIntent.class);

    @Override
    public String getCmdLineOption() {
        return "l";
    }
    
    @Override
    public List<String> getRequiredOptions() {
        return Arrays.asList(new String[]{"l", "k", "e", "a"});
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
        ///// print cluster info
        CurrentClusters cc = new CurrentClusters(ec2);
        log.info(cc.printClusterList());
        
        return true;
    }
}
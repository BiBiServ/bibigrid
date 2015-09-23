package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.AmazonClientException;
import com.jcraft.jsch.*;
import de.unibi.cebitec.bibigrid.StartUpOgeCluster;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.CreateClusterAWS;
import de.unibi.cebitec.bibigrid.meta.openstack.CreateClusterOpenstack;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateIntent extends Intent {

    public static final Logger log = LoggerFactory.getLogger(CreateIntent.class);

    @Override
    public String getCmdLineOption() {
        return "c";
    }

    @Override
    public List<String> getRequiredOptions() {
        return Arrays.asList(new String[]{"m", "M", "s", "S", "n", "u", "k", "i", "e", "a", "z", "g", "r", "b", "i"});
    }

    @Override
    public boolean execute() throws IntentNotConfiguredException {
        if (getConfiguration() == null) {
            throw new IntentNotConfiguredException();
        }
        try {
            if (!startClusterAtSelectedCloudProvider()) {
                log.error(StartUpOgeCluster.ABORT_WITH_INSTANCES_RUNNING);
                Intent cleanupIntent = new TerminateIntent();
                cleanupIntent.setConfiguration(getConfiguration());
                cleanupIntent.execute();
                return false;
            }
        } catch (AmazonClientException | JSchException ace) {
            log.error("{}", ace);
            return false;
        }
        return true;
    }

    private boolean startClusterAtSelectedCloudProvider() throws AmazonClientException, JSchException {
        switch (getConfiguration().getMetaMode()) {
            case "default":
            case "aws-ec2":
                return new CreateClusterAWS(getConfiguration())
                        .createClusterEnvironment()
                            .createVPC()
                            .createSubnet()
                            .createSecurityGroup()
                            .createPlacementGroup()
                        .configureClusterMasterInstance()
                        .launchClusterInstances();
            case "openstack":
                return new CreateClusterOpenstack(getConfiguration(), "http://172.21.32.13:5000/v2.0/")
                        .createClusterEnvironment()
                            .createVPC()
                            .createSubnet()
                            .createSecurityGroup()
                            .createPlacementGroup()
                        .configureClusterMasterInstance()
                        .launchClusterInstances();

            default:
                log.error("Malformed meta-mode! [use: 'aws-ec2','openstack' or leave it blanc.");
                return false;
        }
    }

}

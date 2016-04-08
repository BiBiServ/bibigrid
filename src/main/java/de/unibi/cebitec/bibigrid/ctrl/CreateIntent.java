package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.AmazonClientException;
import com.jcraft.jsch.*;
import de.unibi.cebitec.bibigrid.StartUp;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.CreateClusterAWS;
import de.unibi.cebitec.bibigrid.meta.openstack.CreateClusterOpenstack;
import de.unibi.cebitec.bibigrid.model.Configuration.MODE;
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
    public List<String> getRequiredOptions(MODE mode) {
        switch (mode) {
            case AWS:
                return Arrays.asList(new String[]{"m", "M", "s", "S", "n", "u", "k", "i", "e", "a", "z", "g", "r", "b"});
            case OPENSTACK:
                return Arrays.asList(new String[]{"m", "M", "s", "S", "n", "u", "k", "i", "e", "z", "g", "r", "b", "osu", "ost", "osp", "ose"});
        }
        return null;
    }

    @Override
    public boolean execute() throws IntentNotConfiguredException {
        if (getConfiguration() == null) {
            throw new IntentNotConfiguredException();
        }
        try {
            if (!startClusterAtSelectedCloudProvider()) {
                log.error(StartUp.ABORT_WITH_INSTANCES_RUNNING);
                Intent cleanupIntent = new TerminateIntent();
                cleanupIntent.setConfiguration(getConfiguration());
                cleanupIntent.execute();
                return false;
            }
        } catch (AmazonClientException | JSchException ace) {
            //ace.printStackTrace();
            log.error("Exception : {} ", ace.getMessage());
            return false;
        }
        return true;
    }

    private boolean startClusterAtSelectedCloudProvider() throws AmazonClientException, JSchException {
        switch (getConfiguration().getMode()) {
            case AWS:
                return new CreateClusterAWS(getConfiguration())
                        .createClusterEnvironment()
                            .createVPC()
                            .createSubnet()
                            .createSecurityGroup()
                            .createPlacementGroup()
                        .configureClusterMasterInstance()
                        .configureClusterSlaveInstance()
                        .launchClusterInstances();
            case OPENSTACK:
                return new CreateClusterOpenstack(getConfiguration())
                        .createClusterEnvironment()
                            .createVPC()
                            .createSubnet()
                            .createSecurityGroup()
                            .createPlacementGroup()
                        .configureClusterMasterInstance()
                        .configureClusterSlaveInstance()
                        .launchClusterInstances();

            default:
                log.error("Malformed meta-mode! [use: 'aws-ec2','openstack' or leave it blanc.");
                return false;
        }
    }

}

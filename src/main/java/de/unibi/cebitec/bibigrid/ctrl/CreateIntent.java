package de.unibi.cebitec.bibigrid.ctrl;

import com.jcraft.jsch.*;
import de.unibi.cebitec.bibigrid.StartUp;
import de.unibi.cebitec.bibigrid.exception.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.CreateClusterAWS;
import de.unibi.cebitec.bibigrid.meta.googlecloud.CreateClusterGoogleCloud;
import de.unibi.cebitec.bibigrid.meta.openstack.CreateClusterOpenstack;
import de.unibi.cebitec.bibigrid.model.Configuration.MODE;
import de.unibi.cebitec.bibigrid.exception.ConfigurationException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateIntent extends Intent {

    public static final Logger LOG = LoggerFactory.getLogger(CreateIntent.class);

    @Override
    public String getCmdLineOption() {
        return "c";
    }

    @Override
    public List<String> getRequiredOptions(MODE mode) {
        switch (mode) {
            case AWS:
                return Arrays.asList("m", "M", "s", "S", "n", "k", "i", "e", "a", "z", "g", "r", "b");
            case OPENSTACK:
                return Arrays.asList("m", "M", "s", "S", "n", "k", "i", "e", "z", "g", "r", "b", "osu", "ost", "osp", "ose");
            case GOOGLECLOUD:
                return Arrays.asList("m", "M", "s", "S", "n", "k", "i", "e", "z", "g", "r", "b", "gcf", "gpid");
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
                LOG.error(StartUp.ABORT_WITH_INSTANCES_RUNNING);
                Intent cleanupIntent = new TerminateIntent();
                cleanupIntent.setConfiguration(getConfiguration());
                cleanupIntent.execute();
                return false;
            }
        } catch (ConfigurationException | JSchException ace) {
            LOG.error("Exception : {}",ace.getMessage());
            return false;
        }
        return true;
    }

    private boolean startClusterAtSelectedCloudProvider() throws ConfigurationException,  JSchException {
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
            case GOOGLECLOUD:
                return new CreateClusterGoogleCloud(getConfiguration())
                        .createClusterEnvironment()
                            .createVPC()
                            .createSubnet()
                            .createSecurityGroup()
                            .createPlacementGroup()
                        .configureClusterMasterInstance()
                        .configureClusterSlaveInstance()
                        .launchClusterInstances();

            default:
                LOG.error("Malformed meta-mode! [use: 'aws-ec2','openstack','googlecloud' or leave it blanc.");
                return false;
        }
    }

}

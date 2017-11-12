package de.unibi.cebitec.bibigrid.ctrl;

import com.jcraft.jsch.*;
import de.unibi.cebitec.bibigrid.StartUp;
import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.model.exceptions.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.aws.CreateClusterAWS;
import de.unibi.cebitec.bibigrid.meta.googlecloud.CreateClusterGoogleCloud;
import de.unibi.cebitec.bibigrid.meta.openstack.CreateClusterOpenstack;
import de.unibi.cebitec.bibigrid.model.Configuration.MODE;
import de.unibi.cebitec.bibigrid.model.exceptions.ConfigurationException;

import java.util.*;

import de.unibi.cebitec.bibigrid.util.RuleBuilder;
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
                return Arrays.asList(
                        RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.MASTER_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT_S.toString(),
                        RuleBuilder.RuleNames.KEYPAIR_S.toString(),
                        RuleBuilder.RuleNames.IDENTITY_FILE_S.toString(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString(),
                        RuleBuilder.RuleNames.NFS_SHARES_S.toString(),
                        RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString(),
                        RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_S.toString());
            case OPENSTACK:
                return Arrays.asList(
                        RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.MASTER_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT_S.toString(),
                        RuleBuilder.RuleNames.KEYPAIR_S.toString(),
                        RuleBuilder.RuleNames.IDENTITY_FILE_S.toString(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString(),
                        RuleBuilder.RuleNames.NFS_SHARES_S.toString(),
                        RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString(),
                        RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
            case GOOGLECLOUD:
                return Arrays.asList(
                        RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.MASTER_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT_S.toString(),
                        RuleBuilder.RuleNames.KEYPAIR_S.toString(),
                        RuleBuilder.RuleNames.IDENTITY_FILE_S.toString(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString(),
                        RuleBuilder.RuleNames.NFS_SHARES_S.toString(),
                        RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString(),
                        RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString(),
                        RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString());
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
            LOG.error("Exception : {}", ace.getMessage());
            return false;
        }
        return true;
    }

    private boolean startClusterAtSelectedCloudProvider() throws ConfigurationException, JSchException {
        CreateCluster cluster;
        switch (getConfiguration().getMode()) {
            case AWS:
                cluster = new CreateClusterAWS(getConfiguration());
                break;
            case OPENSTACK:
                cluster = new CreateClusterOpenstack(getConfiguration());
                break;
            case GOOGLECLOUD:
                cluster = new CreateClusterGoogleCloud(getConfiguration());
                break;
            default:
                LOG.error("Malformed meta-mode! [use: 'aws-ec2','openstack','googlecloud' or leave it blank.");
                return false;
        }
        return cluster
                .createClusterEnvironment()
                .createVPC()
                .createSubnet()
                .createSecurityGroup()
                .createPlacementGroup()
                .configureClusterMasterInstance()
                .configureClusterSlaveInstance()
                .launchClusterInstances();
    }
}

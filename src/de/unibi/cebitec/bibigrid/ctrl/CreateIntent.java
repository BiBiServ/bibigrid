package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.*;
import com.jcraft.jsch.*;
import de.unibi.cebitec.bibigrid.StartUpOgeCluster;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.meta.CreateClusterAWS;
import de.unibi.cebitec.bibigrid.model.Port;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import de.unibi.cebitec.bibigrid.util.*;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.*;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateIntent extends Intent {

    public static final Logger log = LoggerFactory.getLogger(CreateIntent.class);
    public static final String PREFIX = "bibigrid-";
    public static final String SECURITY_GROUP_PREFIX = PREFIX + "sg-";

    public static final String MASTER_SSH_USER = "ubuntu";
    public static final String PLACEMENT_GROUP_PREFIX = PREFIX + "pg-";
    public static final String SUBNET_PREFIX = PREFIX + "subnet-";
    private AmazonEC2 ec2;
    private Vpc vpc;
    private Subnet subnet;
    private String MASTERIP;

    @Override
    public String getCmdLineOption() {
        return "c";
    }

    @Override
    public List<String> getRequiredOptions() {
        return Arrays.asList(new String[]{"m", "M", "s", "S", "n", "u", "k", "i", "e", "a", "z", "g", "r", "b"});
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
                        .configureClusterMasterInstance()
                        .configureClusterSlaveInstance()
                        .launchClusterInstances();
            case "openstack":
                return true;
            default:
                log.error("Malformed meta-mode! [use: 'aws-ec2','openstack' or leave it blanc.");
                return false;
        }
    }

}

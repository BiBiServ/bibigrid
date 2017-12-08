package de.unibi.cebitec.bibigrid.core.intents;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.JSchLogger;
import de.unibi.cebitec.bibigrid.core.util.SshFactory;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * CreateCluster Interface must be implemented by all "real" CreateCluster classes and
 * provides the minimum of general functions for the environment, the configuration of
 * master and slave instances and launching the cluster.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class CreateCluster implements Intent {
    private static final Logger LOG = LoggerFactory.getLogger(CreateCluster.class);
    public static final String PREFIX = "bibigrid-";
    protected static final String MASTER_SSH_USER = "ubuntu";

    protected final ProviderModule providerModule;
    protected String clusterId;

    protected CreateCluster(ProviderModule providerModule) {
        this.providerModule = providerModule;
    }

    public String getClusterId() {
        return clusterId;
    }

    /**
     * The environment creation procedure. For a successful environment creation
     * you will need an Environment-Instance which implements the
     * CreateClusterEnvironment interface.
     * <h1><u>Example</u></h1>
     * <h2> The default procedure (AWS) </h2>
     * Configuration conf;
     * <ol>
     * <li>new CreateClusterAWS(conf).createClusterEnvironment()</li>
     * <li>&#09.createVPC()</li>
     * <li>&#09.createSubnet()</li>
     * <li>&#09.createSecurityGroup()</li>
     * <li>&#09.createPlacementGroup()</li>
     * <li>.configureClusterMasterInstance()</li>
     * <li>.configureClusterSlaveInstance()</li>
     * <li>.launchClusterInstances()</li>
     * </ol>
     */
    public abstract CreateClusterEnvironment createClusterEnvironment();

    /**
     * Configure and manage Master-instance to launch.
     */
    public abstract CreateCluster configureClusterMasterInstance();

    /**
     * Configure and manage Slave-instances to launch.
     */
    public abstract CreateCluster configureClusterSlaveInstance();

    /**
     * Start the configured cluster now.
     */
    public abstract boolean launchClusterInstances();

    protected String generateClusterId() {
        // Cluster ID is a cut down base64 encoded version of a random UUID:
        UUID clusterIdUUID = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(clusterIdUUID.getMostSignificantBits());
        bb.putLong(clusterIdUUID.getLeastSignificantBits());
        String clusterIdBase64 = Base64.encodeBase64URLSafeString(bb.array()).replace("-", "").replace("_", "");
        int len = clusterIdBase64.length() >= 15 ? 15 : clusterIdBase64.length();
        // All resource ids must be lower case in google cloud!
        clusterId = clusterIdBase64.substring(0, len).toLowerCase(Locale.US);
        LOG.debug("cluster id: {}", clusterId);
        return clusterId;
    }

    protected void logFinishedInfoMessage(String masterPublicIp, Configuration config, String clusterId) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n You might want to set the following environment variable:\n\n");
        sb.append("export BIBIGRID_MASTER=").append(masterPublicIp).append("\n\n");
        sb.append("You can then log on the master node with:\n\n")
                .append("ssh -i ")
                .append(config.getIdentityFile())
                .append(" ubuntu@$BIBIGRID_MASTER\n\n");
        sb.append("The cluster id of your started cluster is : ")
                .append(clusterId)
                .append("\n\n");
        sb.append("The can easily terminate the cluster at any time with :\n")
                .append("./bibigrid -t ").append(clusterId).append(" ");
        if (config.isAlternativeConfigFile()) {
            sb.append("-o ").append(config.getAlternativeConfigPath()).append(" ");
        }
        sb.append("\n");
        LOG.info(sb.toString());
    }

    protected void saveGridPropertiesFile(String masterPublicIp, Configuration config, String clusterId) {
        if (config.getGridPropertiesFile() != null) {
            Properties gp = new Properties();
            gp.setProperty("BIBIGRID_MASTER", masterPublicIp);
            gp.setProperty("IdentityFile", config.getIdentityFile().toString());
            gp.setProperty("clusterId", clusterId);
            if (config.isAlternativeConfigFile()) {
                gp.setProperty("AlternativeConfigFile", config.getAlternativeConfigPath());
            }
            try {
                gp.store(new FileOutputStream(config.getGridPropertiesFile()), "Auto-generated by BiBiGrid");
            } catch (IOException e) {
                LOG.error(I, "Exception while creating grid properties file : " + e.getMessage());
            }
        }
    }

    protected void configureMaster(String masterPrivateIp, String masterPublicIp, List<String> slaveIps,
                                   Configuration config) {
        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        LOG.info("Now configuring ...");
        String execCommand = SshFactory.buildSshCommand(masterPrivateIp, masterPublicIp, slaveIps, config);
        LOG.info(V, "Building SSH-Command : {}", execCommand);
        boolean configured = false;
        int ssh_attempts = 25; // TODO attempts
        while (!configured && ssh_attempts > 0) {
            try {
                ssh.addIdentity(config.getIdentityFile().toString());
                LOG.info("Trying to connect to master ({})...", ssh_attempts);
                sleep(4);
                // Create new Session to avoid packet corruption.
                Session sshSession = SshFactory.createNewSshSession(ssh, masterPublicIp, MASTER_SSH_USER,
                        config.getIdentityFile());
                // Start connect attempt
                //noinspection ConstantConditions
                sshSession.connect();
                LOG.info("Connected to master!");

                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                BufferedReader stdout = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(channel.getErrStream()));
                channel.setCommand(execCommand);

                LOG.info(V, "Connecting ssh channel...");
                channel.connect();

                String lineOut, lineError = null;
                while (((lineOut = stdout.readLine()) != null) || ((lineError = stderr.readLine()) != null)) {
                    if (lineOut != null) {
                        if (lineOut.contains("CONFIGURATION_FINISHED")) {
                            configured = true;
                        }
                        LOG.info(V, "SSH: {}", lineOut);
                    }
                    if (lineError != null && !configured) {
                        if (lineError.contains("sudo: unable to resolve host")) {
                            LOG.warn(V, "SSH: {}", lineError);
                        } else {
                            LOG.error("SSH: {}", lineError);
                        }
                    }
                    if (channel.isClosed() && configured) {
                        LOG.info(V, "SSH: exit-status: {}", channel.getExitStatus());
                        configured = true;
                    }
                    sleep(2);
                }
                if (configured) {
                    channel.disconnect();
                    sshSession.disconnect();
                }
            } catch (IOException | JSchException e) {
                ssh_attempts--;
                if (ssh_attempts == 0) {
                    LOG.error(V, "SSH: {}", e);
                }
            }
        }
        if (configured) {
            LOG.info(I, "Master instance has been configured.");
        } else {
            LOG.error("Master instance configuration failed!");
        }
    }

    protected void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ie) {
            LOG.error("Thread.sleep interrupted!");
            ie.printStackTrace();
        }
    }
}

package de.unibi.cebitec.bibigrid.core.intents;

import com.jcraft.jsch.*;
import de.unibi.cebitec.bibigrid.core.model.AnsibleConfig;
import de.unibi.cebitec.bibigrid.core.model.AnsibleHostsConfig;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.AnsibleResources;
import de.unibi.cebitec.bibigrid.core.util.JSchLogger;
import de.unibi.cebitec.bibigrid.core.util.ShellScriptCreator;
import de.unibi.cebitec.bibigrid.core.util.SshFactory;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    public static final String MASTER_SSH_USER = "ubuntu";
    private static final int SSH_ATTEMPTS = 25; // TODO: decide value or parameter

    protected final ProviderModule providerModule;
    private final Configuration config;
    protected final String clusterId;

    protected CreateCluster(Configuration config, ProviderModule providerModule) {
        this.config = config;
        this.providerModule = providerModule;
        clusterId = generateClusterId();
        config.setClusterId(clusterId);
    }

    private String generateClusterId() {
        // Cluster ID is a cut down base64 encoded version of a random UUID:
        UUID clusterIdUUID = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(clusterIdUUID.getMostSignificantBits());
        bb.putLong(clusterIdUUID.getLeastSignificantBits());
        String clusterIdBase64 = Base64.encodeBase64URLSafeString(bb.array()).replace("-", "").replace("_", "");
        int len = clusterIdBase64.length() >= 15 ? 15 : clusterIdBase64.length();
        // All resource ids must be lower case in google cloud!
        String clusterId = clusterIdBase64.substring(0, len).toLowerCase(Locale.US);
        LOG.debug("cluster id: {}", clusterId);
        return clusterId;
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
     *
     * @throws ConfigurationException
     */
    public abstract CreateClusterEnvironment createClusterEnvironment() throws ConfigurationException;

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

    protected void logFinishedInfoMessage(String masterPublicIp) {
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

    protected void saveGridPropertiesFile(String masterPublicIp) {
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

    protected void configureMaster(final String masterPrivateIp, final String masterPublicIp,
                                   final String masterHostname, final List<String> slaveIps,
                                   final List<String> slaveHostnames, final String subnetCidr) {
        // TODO
        String user = config.getUser() != null ? config.getUser() : MASTER_SSH_USER;
        AnsibleHostsConfig ansibleHostsConfig = new AnsibleHostsConfig(config);
        AnsibleConfig ansibleConfig = new AnsibleConfig(config);
        ansibleConfig.setSubnetCidr(subnetCidr);
        ansibleConfig.setMasterIpHostname(masterPrivateIp, masterHostname);
        for (int i = 0; i < slaveIps.size(); i++) {
            ansibleConfig.addSlaveIpHostname(slaveIps.get(i), slaveHostnames.get(i));
            ansibleHostsConfig.addSlaveIp(slaveIps.get(i));
        }

        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        LOG.info("Now configuring ...");
        boolean configured = false;
        int sshAttempts = SSH_ATTEMPTS;
        while (!configured && sshAttempts > 0) {
            try {
                ssh.addIdentity(config.getIdentityFile().toString());
                LOG.info("Trying to connect to master ({})...", sshAttempts);
                sleep(4);
                // Create new Session to avoid packet corruption.
                Session sshSession = SshFactory.createNewSshSession(ssh, masterPublicIp, user, config.getIdentityFile());
                if (sshSession == null)
                    continue;
                // Start connection attempt
                sshSession.connect();
                LOG.info("Connected to master!");
                configured = uploadAnsibleToMaster(sshSession, ansibleHostsConfig, ansibleConfig) &&
                        installAndExecuteAnsible(sshSession);
                if (configured) {
                    sshSession.disconnect();
                } else {
                    sshAttempts--;
                }
            } catch (IOException | JSchException e) {
                sshAttempts--;
                LOG.error("SSH: {}", e);
                if (sshAttempts == 0) {
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

    private boolean uploadAnsibleToMaster(Session sshSession, AnsibleHostsConfig hostsConfig, AnsibleConfig commonConfig)
            throws JSchException {
        boolean uploadCompleted;
        ChannelSftp channel = (ChannelSftp) sshSession.openChannel("sftp");
        LOG.info(V, "Connecting sftp channel...");
        channel.connect();
        try {
            // Collect the Ansible files from resources for upload
            AnsibleResources resources = new AnsibleResources();
            // First the folders need to be created
            for (String folderPath : resources.getDirectories()) {
                String fullPath = channel.getHome() + "/" + folderPath;
                LOG.info(V, "SFTP: Create folder {}", fullPath);
                try {
                    channel.cd(fullPath);
                } catch (SftpException e) {
                    channel.mkdir(fullPath);
                }
                channel.cd(channel.getHome());
            }
            // Each file is uploaded to it's relative path in the home folder
            for (String filepath : resources.getFiles().keySet()) {
                FileInputStream stream = new FileInputStream(resources.getFiles().get(filepath));
                // Upload the file stream via sftp to the home folder
                String fullPath = channel.getHome() + "/" + filepath;
                LOG.info(V, "SFTP: Upload file {}", fullPath);
                channel.put(stream, fullPath);
            }
            // Write the hosts configuration file
            try (OutputStreamWriter writer = new OutputStreamWriter(channel.put(channel.getHome() + "/" +
                    AnsibleResources.HOSTS_CONFIG_FILE))) {
                writer.write(hostsConfig.toString());
            }
            // Write the commons configuration file
            try (OutputStreamWriter writer = new OutputStreamWriter(channel.put(channel.getHome() + "/" +
                    AnsibleResources.COMMONS_CONFIG_FILE))) {
                writer.write(commonConfig.toString());
            }
            uploadCompleted = true;
        } catch (SftpException | IOException e) {
            LOG.error("SFTP: {}", e);
            uploadCompleted = false;
        } finally {
            channel.disconnect();
        }
        return uploadCompleted;
    }

    private boolean installAndExecuteAnsible(Session sshSession) throws JSchException, IOException {
        boolean configured = false;
        String execCommand = ShellScriptCreator.getMasterAnsibleExecutionScript();
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
        channel.disconnect();
        return configured;
    }

    protected void sleep(int seconds) {
        sleep(seconds, true);
    }

    protected void sleep(int seconds, boolean throwException) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ie) {
            if (throwException) {
                LOG.error("Thread.sleep interrupted!");
                ie.printStackTrace();
            }
        }
    }
}

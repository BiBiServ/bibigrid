package de.unibi.cebitec.bibigrid.core.intents;

import com.jcraft.jsch.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * CreateCluster Interface must be implemented by all "real" CreateCluster classes and
 * provides the minimum of general functions for the environment, the configuration of
 * master and slave instances and launching the cluster.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class CreateCluster extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(CreateCluster.class);
    public static final String PREFIX = "bibigrid-";
    static final String MASTER_NAME_PREFIX = PREFIX + "master";
    static final String SLAVE_NAME_PREFIX = PREFIX + "slave";

    protected final ProviderModule providerModule;
    protected final Client client;
    protected final Configuration config;
    protected final String clusterId;
    protected CreateClusterEnvironment environment;

    private Instance masterInstance;
    private List<Instance> slaveInstances;
    protected DeviceMapper masterDeviceMapper;

    protected CreateCluster(ProviderModule providerModule, Client client, Configuration config) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
        clusterId = generateClusterId();
        LOG.debug("cluster id: {}", clusterId);
        config.setClusterIds(clusterId);
    }

    static String generateClusterId() {
        // Cluster ID is a cut down base64 encoded version of a random UUID:
        UUID clusterIdUUID = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(clusterIdUUID.getMostSignificantBits());
        bb.putLong(clusterIdUUID.getLeastSignificantBits());
        String clusterIdBase64 = Base64.getUrlEncoder().encodeToString(bb.array());
        clusterIdBase64 = clusterIdBase64.replace("-", "").replace("_", "");
        int len = Math.min(clusterIdBase64.length(), 15);
        // All resource ids must be lower case in google cloud!
        return clusterIdBase64.substring(0, len).toLowerCase(Locale.US);
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
     * <li>&#09.createNetwork()</li>
     * <li>&#09.createSubnet()</li>
     * <li>&#09.createSecurityGroup()</li>
     * <li>&#09.createPlacementGroup()</li>
     * <li>.configureClusterMasterInstance()</li>
     * <li>.configureClusterSlaveInstance()</li>
     * <li>.launchClusterInstances()</li>
     * </ol>
     *
     * @throws ConfigurationException Throws an exception if the creation of the cluster environment failed.
     */
    public CreateClusterEnvironment createClusterEnvironment() throws ConfigurationException {
        return environment = providerModule.getClusterEnvironment(client, this);
    }

    /**
     * Configure and manage Master-instance to launch.
     */
    public CreateCluster configureClusterMasterInstance() {
        List<Configuration.MountPoint> masterVolumeToMountPointMap = resolveMountSources(config.getMasterMounts());
        InstanceType masterSpec = config.getMasterInstance().getProviderType();
        int deviceOffset = masterSpec.getEphemerals() + masterSpec.getSwap();
        masterDeviceMapper = new DeviceMapper(providerModule, masterVolumeToMountPointMap, deviceOffset);
        LOG.info("Master instance configured.");
        return this;
    }

    /**
     * Configure and manage Slave-instances to launch.
     */
    public CreateCluster configureClusterSlaveInstance() {
        LOG.info("Slave instance(s) configured.");
        return this;
    }

    /**
     * Resolve the mount source volumes or snapshots.
     */
    protected abstract List<Configuration.MountPoint> resolveMountSources(List<Configuration.MountPoint> mountPoints);

    /**
     * Start the configured cluster now.
     */
    public boolean launchClusterInstances(final boolean prepare) {
        try {
            String masterNameTag = MASTER_NAME_PREFIX + "-" + clusterId;
            masterInstance = launchClusterMasterInstance(masterNameTag);
            if (masterInstance == null) {
                return false;
            }
            slaveInstances = new ArrayList<>();
            int totalSlaveInstanceCount = config.getSlaveInstanceCount();
            if (totalSlaveInstanceCount > 0) {
                LOG.info("Requesting {} slave instance(s) with {} different configurations...",
                        totalSlaveInstanceCount, config.getSlaveInstances().size());
                for (int i = 0; i < config.getSlaveInstances().size(); i++) {
                    Configuration.SlaveInstanceConfiguration instanceConfiguration = config.getSlaveInstances().get(i);
                    LOG.info("Requesting {} slave instance(s) with same configuration...",
                            instanceConfiguration.getCount());
                    String slaveNameTag = SLAVE_NAME_PREFIX + "-" + clusterId;
                    List<Instance> slavesBatch = launchClusterSlaveInstances(i, instanceConfiguration, slaveNameTag);
                    if (slavesBatch == null) {
                        return false;
                    }
                    slaveInstances.addAll(slavesBatch);
                }
            } else {
                LOG.info("No Slave instance(s) requested!");
            }
            // just to be sure, everything is present, wait x seconds
            sleep(4);
            LOG.info("Cluster (ID: {}) successfully created!", clusterId);
            final String masterIp = config.isUseMasterWithPublicIp() ? masterInstance.getPublicIp() :
                    masterInstance.getPrivateIp();
            configureMaster(masterInstance, slaveInstances, environment.getSubnet().getCidr(), prepare);
            logFinishedInfoMessage(masterIp);
            saveGridPropertiesFile(masterIp);
        } catch (Exception e) {
            // print stacktrace only verbose mode, otherwise the message is fine
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                LOG.error(e.getMessage(), e);
            } else {
                LOG.error(e.getMessage());
            }
            return false;
        }
        return true;
    }

    /**
     * Start the configured cluster master instance.
     *
     * @param masterNameTag The generated name tag for the master instance.
     */
    protected abstract Instance launchClusterMasterInstance(String masterNameTag);

    /**
     * Start the batch of cluster slave instances.
     */
    protected abstract List<Instance> launchClusterSlaveInstances(
            int batchIndex, Configuration.SlaveInstanceConfiguration instanceConfiguration, String slaveNameTag);

    protected String buildSlaveInstanceName(int batchIndex, int slaveIndex) {
        return SLAVE_NAME_PREFIX + (batchIndex + 1) + "-" + (slaveIndex + 1) + "-" + clusterId;
    }

    private void logFinishedInfoMessage(final String masterPublicIp) {
        if (SshFactory.isOsWindows()) {
            logFinishedInfoMessageWindows(masterPublicIp);
        } else {
            logFinishedInfoMessageUnix(masterPublicIp);
        }
    }

    private void logFinishedInfoMessageWindows(final String masterPublicIp) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n Only Windows 10 has built in ssh support (currently beta). We recommend you download")
                .append(" PuTTY from https://www.putty.org\n\n");
        sb.append("You might want to set the following environment variable:\n\n");
        sb.append("setx BIBIGRID_MASTER \"").append(masterPublicIp).append("\"\n\n");
        sb.append("You can then log on the master node with:\n\n")
                .append("putty -i ")
                .append(config.getSshPrivateKeyFile())
                .append(" ").append(config.getSshUser()).append("@%BIBIGRID_MASTER%\n\n");
        sb.append("The cluster id of your started cluster is: ").append(clusterId).append("\n\n");
        sb.append("You can easily terminate the cluster at any time with:\n")
                .append("./bibigrid -t ").append(clusterId).append(" ");
        if (config.isAlternativeConfigFile()) {
            sb.append("-o ").append(config.getAlternativeConfigPath()).append(" ");
        }
        sb.append("\n");
        LOG.info(sb.toString());
    }

    private void logFinishedInfoMessageUnix(final String masterPublicIp) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n You might want to set the following environment variable:\n\n");
        sb.append("export BIBIGRID_MASTER=").append(masterPublicIp).append("\n\n");
        sb.append("You can then log on the master node with:\n\n")
                .append("ssh -i ")
                .append(config.getSshPrivateKeyFile())
                .append(" ").append(config.getSshUser()).append("@$BIBIGRID_MASTER\n\n");
        sb.append("The cluster id of your started cluster is: ").append(clusterId).append("\n\n");
        sb.append("You can easily terminate the cluster at any time with:\n")
                .append("./bibigrid -t ").append(clusterId).append(" ");
        if (config.isAlternativeConfigFile()) {
            sb.append("-o ").append(config.getAlternativeConfigPath()).append(" ");
        }
        sb.append("\n");
        LOG.info(sb.toString());
    }

    private void saveGridPropertiesFile(String masterPublicIp) {
        if (config.getGridPropertiesFile() != null) {
            Properties gp = new Properties();
            gp.setProperty("BIBIGRID_MASTER", masterPublicIp);
            gp.setProperty("SSHPublicKeyFile", config.getSshPublicKeyFile());
            gp.setProperty("SSHPrivateKeyFile", config.getSshPrivateKeyFile());
            gp.setProperty("clusterId", clusterId);
            if (config.isAlternativeConfigFile()) {
                gp.setProperty("AlternativeConfigFile", config.getAlternativeConfigPath());
            }
            try (FileOutputStream stream = new FileOutputStream(config.getGridPropertiesFile())) {
                gp.store(stream, "Auto-generated by BiBiGrid");
            } catch (IOException e) {
                LOG.error(I, "Exception while creating grid properties file: " + e.getMessage());
            }
        }
    }

    private void configureMaster(final Instance masterInstance, final List<Instance> slaveInstances,
                                 final String subnetCidr, final boolean prepare) {
        AnsibleHostsConfig ansibleHostsConfig = new AnsibleHostsConfig(config, slaveInstances);
        AnsibleConfig ansibleConfig = new AnsibleConfig(config, providerModule.getBlockDeviceBase(), subnetCidr,
                masterInstance, slaveInstances);
        ansibleConfig.setMasterMounts(masterDeviceMapper);

        final String masterIp = config.isUseMasterWithPublicIp() ? masterInstance.getPublicIp() :
                masterInstance.getPrivateIp();
        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        LOG.info("Now configuring...");
        boolean configured = false;
        boolean sshPortIsReady = SshFactory.pollSshPortIsAvailable(masterIp);
        if (sshPortIsReady) {
            try {
                ssh.addIdentity(config.getSshPrivateKeyFile());
                LOG.info("Trying to connect to master...");
                sleep(4);
                // Create new Session to avoid packet corruption.
                Session sshSession = SshFactory.createNewSshSession(ssh, masterIp, config.getSshUser(),
                        Paths.get(config.getSshPrivateKeyFile()));
                if (sshSession != null) {
                    // Start connection attempt
                    sshSession.connect();
                    LOG.info("Connected to master!");

                    configured = uploadAnsibleToMaster(sshSession, ansibleHostsConfig, ansibleConfig, slaveInstances) &&
                            installAndExecuteAnsible(sshSession, prepare);
                    sshSession.disconnect();
                }
            } catch (IOException | JSchException e) {
                LOG.error("SSH: ", e);
            }
        }
        if (configured) {
            LOG.info(I, "Master instance has been configured.");
        } else {
            LOG.error("Master instance configuration failed!");
        }
    }

    /**
     * Uploads ansible roles to master instance.
     *
     * @param sshSession ssh connection to master
     * @param hostsConfig Configuration and list of slave IPs
     * @param commonConfig common Configuration
     * @param slaveInstances list of slave instances
     * @return true, if upload completed successfully
     * @throws JSchException possible SSH connection error
     */
    private boolean uploadAnsibleToMaster(Session sshSession, AnsibleHostsConfig hostsConfig,
                                          AnsibleConfig commonConfig, List<Instance> slaveInstances) throws JSchException {
        boolean uploadCompleted;
        ChannelSftp channel = (ChannelSftp) sshSession.openChannel("sftp");
        LOG.info("Upload Ansible playbook to master instance.");
        LOG.info(V, "Connecting sftp channel...");
        channel.connect();
        try {
            // Collect Ansible files from resources for upload
            AnsibleResources resources = new AnsibleResources();
            this.uploadResourcesFiles(resources, channel);

            // Divide into master and slave roles to write in site.yml
            Map<String, String> customMasterRoles = new LinkedHashMap<>();
            Map<String, String> customSlaveRoles = new LinkedHashMap<>();

            // Add Ansible roles
            List<Configuration.AnsibleRoles> ansibleRoles = config.getAnsibleRoles();
            for (Configuration.AnsibleRoles role : ansibleRoles) {
                String roleName = getSingleFileName(role.getFile()).split(".tgz")[0].split(".tar.gz")[0];
                Map<String, Object> roleVars = role.getVars();
                // Set role key - value pairs
                if (role.getVarsFile() != null) {
                    // VarsFile readable since it is proved in Validation
                    String vars = new String(Files.readAllBytes(Paths.get(role.getVarsFile())));
                    Yaml yaml = new Yaml();
                    Map<String, Object> additionalVars = yaml.load(vars);
                    roleVars.putAll(additionalVars);
                }
                String roleVarsFile = "";
                if (roleVars != null && !roleVars.isEmpty()) {
                    roleVarsFile = AnsibleResources.VARS_PATH + roleName + "-vars.yml";
                    commonConfig.writeAnsibleVarsFile(channel.put(channel.getHome() + "/" +
                            AnsibleResources.ROOT_PATH + roleVarsFile), roleVars);
                }
                switch (role.getHosts()) {
                    case "master":
                        customMasterRoles.put(roleName, roleVarsFile);
                        break;
                    case "slaves":
                        customSlaveRoles.put(roleName, roleVarsFile);
                        break;
                    default:
                        customMasterRoles.put(roleName, roleVarsFile);
                        customSlaveRoles.put(roleName, roleVarsFile);
                }

                this.uploadAnsibleRole(channel, resources, role.getFile());
            }

            // Add galaxy roles
            List<Configuration.AnsibleGalaxyRoles> ansibleGalaxyRoles = config.getAnsibleGalaxyRoles();
            for (Configuration.AnsibleGalaxyRoles role : ansibleGalaxyRoles) {
                String roleName = role.getName();
                Map<String, Object> roleVars = role.getVars();
                // Set role key - value pairs
                // Put vars from external vars file into Map
                if (role.getVarsFile() != null) {
                    String vars = new String(Files.readAllBytes(Paths.get(role.getVarsFile())));
                    Yaml yaml = new Yaml();
                    Map<String, Object> additionalVars = yaml.load(vars);
                    roleVars.putAll(additionalVars);
                }
                String roleVarsFile = "";
                if (roleVars != null && !roleVars.isEmpty()) {
                    roleVarsFile = AnsibleResources.VARS_PATH + roleName + "-vars.yml";
                    commonConfig.writeAnsibleVarsFile(channel.put(channel.getHome() + "/" +
                            AnsibleResources.ROOT_PATH + roleVarsFile), roleVars);
                }
                // Replace ansible galaxy name with self-specified
                role.setName(roleName);
                switch (role.getHosts()) {
                    case "master":
                        customMasterRoles.put(roleName, roleVarsFile);
                        break;
                    case "slaves":
                        customSlaveRoles.put(roleName, roleVarsFile);
                        break;
                    default:
                        customMasterRoles.put(roleName, roleVarsFile);
                        customSlaveRoles.put(roleName, roleVarsFile);
                }
            }

            // Write the hosts configuration file
            try (OutputStreamWriter writer = new OutputStreamWriter(channel.put(channel.getHome() + "/" +
                    AnsibleResources.HOSTS_CONFIG_FILE), StandardCharsets.UTF_8)) {
                writer.write(hostsConfig.toString());
            }
            // Write the commons configuration file
            commonConfig.writeCommonFile(channel.put(channel.getHome() + "/"
                    + AnsibleResources.COMMONS_CONFIG_FILE));

            // Write custom site file
            commonConfig.writeSiteFile(channel.put(channel.getHome() + "/"
                            + AnsibleResources.SITE_CONFIG_FILE),
                     customMasterRoles, customSlaveRoles);

            // Write requirements file for ansible-galaxy support
            if (!ansibleGalaxyRoles.isEmpty()) {
                commonConfig.writeRequirementsFile(channel.put(channel.getHome() + "/"
                        + AnsibleResources.REQUIREMENTS_CONFIG_FILE));
            }

            // Write slave instance specific configuration file
            for (Instance slave : slaveInstances) {
                String filename = channel.getHome() + "/" + AnsibleResources.CONFIG_ROOT_PATH + "/"
                        + slave.getPrivateIp() + ".yml";
                commonConfig.writeInstanceFile(slave, channel.put(filename));
            }
            uploadCompleted = true;
        } catch (SftpException | IOException e) {
            LOG.error("SFTP: ", e);
            uploadCompleted = false;
        } finally {
            channel.disconnect();
        }
        return uploadCompleted;
    }

    /**
     * Uploads common Ansible Resources files.
     *
     * @param resources ansible configuration
     * @param channel client side of sftp server channel
     */
    private void uploadResourcesFiles(AnsibleResources resources, ChannelSftp channel) {
        try {
            // First the folders need to be created
            createSftpFolders(channel, resources, resources.getFiles());
            // Each file is uploaded to it's relative path in the home folder
            for (String filepath : resources.getFiles()) {
                InputStream stream = resources.getFileStream(filepath);
                // Upload the file stream via sftp to the home folder
                String fullPath = channel.getHome() + "/" + filepath;
                LOG.info(V, "SFTP: Upload file {}", fullPath);
                channel.put(stream, fullPath);
            }
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates folders for every directory, given a file structure.
     *
     * @param channel client side of sftp server channel
     * @param resources ansible configuration
     * @param files list of files in file structure
     * @throws SftpException possible SFTP failure
     */
    private void createSftpFolders(ChannelSftp channel, AnsibleResources resources, List<String> files) throws SftpException {
        for (String folderPath : resources.getDirectories(files)) {
            String fullPath = channel.getHome() + "/" + folderPath;
            try {
                channel.cd(fullPath);
            } catch (SftpException e) {
                LOG.info(V, "SFTP: Create folder {}", fullPath);
                channel.mkdir(fullPath);
            }
            channel.cd(channel.getHome());
        }
    }

    /**
     * Uploads single ansible role (.tar.gz, .tgz) to remote instance.
     *
     * @param channel client side of sftp server channel
     * @param resources ansible configuration
     * @param roleFile path/to/role on local machine
     * @throws SftpException possible SFTP failure
     * @throws IOException possible File failure
     */
    private void uploadAnsibleRole(ChannelSftp channel, AnsibleResources resources, String roleFile)
            throws SftpException, IOException {
        Path rootRolePath = Paths.get(roleFile);
        // playbook/roles/ROLE_NAME
        roleFile = this.getSingleFileName(roleFile);
        String remotePath = AnsibleResources.ROLES_ROOT_PATH + roleFile;
        // Walks through valid files in rootRolePath and collects Stream to path list
        List<Path> files = Files.walk(rootRolePath).filter(p -> p.toFile().isFile()).collect(Collectors.toList());
        // create a list of files in role
        List<String> targetFiles = files.stream().map(p -> remotePath + rootRolePath.relativize(p)).collect(Collectors.toList());
        createSftpFolders(channel, resources, targetFiles);
        for (int i = 0; i < files.size(); i++) {
            InputStream stream = new FileInputStream(files.get(i).toFile());
            // Upload the file stream via sftp to the home folder
            String fullPath = channel.getHome() + "/" + targetFiles.get(i).replace("\\", "/");
            LOG.info(V, "SFTP: Upload file {}", fullPath);
            channel.put(stream, fullPath);
        }
    }

    /**
     * Turns path/to/file.* into file.*.
     * @param roleFile path/to/file
     * @return fileName
     */
    private String getSingleFileName(String roleFile) {
        roleFile = roleFile.replace("\\", "/");
        String[] pathway = roleFile.split("/");
        return pathway[pathway.length - 1];
    }

    /**
     * Installs and executes ansible roles on remote.
     *
     * @param sshSession transfer via ssh session
     * @param prepare true, if still preparation necessary
     * @return true, if configuration finished
     * @throws JSchException ssh openChannel exception
     * @throws IOException BufferedReader exceptions
     */
    private boolean installAndExecuteAnsible(final Session sshSession, final boolean prepare)
            throws IOException, JSchException {
        LOG.info("Configure and execute Ansible. This take a while. Please be patient.");
        boolean configured = false;
        String execCommand = ShellScriptCreator.getMasterAnsibleExecutionScript(prepare);
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
                int indexOfLogMessage = lineOut.indexOf("\"[BIBIGRID] ");
                if (indexOfLogMessage > 0) {
                    LOG.info("Ansible: {}", lineOut.substring(indexOfLogMessage + 12, lineOut.length() - 1));
                } else {
                    LOG.info(V, "SSH: {}", lineOut);
                }
                if (lineOut.contains("fatal") && lineOut.contains(" FAILED! => ")) {
                    LOG.info("Ansible: There might be a problem with the ansible script ({}). " +
                            "Please check '/var/log/ansible-playbook.log' on the master instance " +
                            "after BiBiGrid finished.", lineOut);
                }
            }
            if (lineError != null && !configured) {
                LOG.error("SSH: {}", lineError);
            }
            if (channel.isClosed() && configured) {
                LOG.info(V, "SSH: exit-status: {}", channel.getExitStatus());
                configured = true;
            }
        }
        channel.disconnect();
        return configured;
    }

    public Configuration getConfig() {
        return config;
    }

    public Instance getMasterInstance() {
        return masterInstance;
    }

    public List<Instance> getSlaveInstances() {
        return slaveInstances;
    }
}

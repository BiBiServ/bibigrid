package de.unibi.cebitec.bibigrid.core.intents;

import com.jcraft.jsch.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.*;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * CreateCluster Interface must be implemented by all "real" CreateCluster classes and
 * provides the minimum of general functions for the environment, the configuration of
 * master and worker instances and launching the cluster.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class CreateCluster extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(CreateCluster.class);
    public static final String PREFIX = "bibigrid-";
    static final String MASTER_NAME_PREFIX = PREFIX + "master";
    static final String WORKER_NAME_PREFIX = PREFIX + "worker";

    protected final ProviderModule providerModule;
    protected final Client client;
    protected final Configuration config;
    protected final String clusterId;
    protected CreateClusterEnvironment environment;

    private Instance masterInstance;
    private List<Instance> workerInstances;
    protected DeviceMapper masterDeviceMapper;

    private Thread interruptionMessageHook;

    /**
     * Creates a cluster from scratch or uses clusterId to manually / dynamically scale an available cluster.
     * @param providerModule Specific cloud provider access
     * @param client Client to communicate with cloud provider
     * @param config Configuration
     * @param clusterId optional if cluster already started and has to be scaled
     */
    protected CreateCluster(ProviderModule providerModule, Client client, Configuration config, String clusterId) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
        this.clusterId = clusterId != null ? clusterId : generateClusterId();
        this.interruptionMessageHook = new Thread(() ->
                LOG.error("Cluster setup was interrupted!\n\n" +
                        "Please clean up the remains using: -t {}\n\n", this.clusterId));
    }

    /**
     * Generates a cut down base64 encoded version of a random UUID.
     * @return cluster id
     */
    static String generateClusterId() {
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
     * <li>.configureClusterWorkerInstance()</li>
     * <li>.launchClusterInstances()</li>
     * </ol>
     *
     * @throws ConfigurationException Throws an exception if the creation of the cluster environment failed.
     */
    public CreateClusterEnvironment createClusterEnvironment() throws ConfigurationException {
        Runtime.getRuntime().addShutdownHook(this.interruptionMessageHook);
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
     * Configure and manage Worker-instances to launch.
     */
    public CreateCluster configureClusterWorkerInstance() {
        LOG.info("Worker instance(s) configured.");
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
            masterInstance.setMaster(true);
            if (masterInstance == null) {
                return false;
            }
            workerInstances = new ArrayList<>();
            int totalWorkerInstanceCount = config.getWorkerInstanceCount();
            if (totalWorkerInstanceCount > 0) {
                LOG.info("Requesting {} worker instance(s) with {} different configurations...",
                        totalWorkerInstanceCount, config.getWorkerInstances().size());
                for (int i = 0; i < config.getWorkerInstances().size(); i++) {
                    Configuration.WorkerInstanceConfiguration instanceConfiguration = config.getWorkerInstances().get(i);
                    LOG.info("Requesting {} worker instance(s) with same configuration...",
                            instanceConfiguration.getCount());
                    String workerNameTag = WORKER_NAME_PREFIX + "-" + clusterId;
                    List<Instance> workersBatch = launchClusterWorkerInstances(i, instanceConfiguration, workerNameTag);
                    if (workersBatch == null) {
                        return false;
                    }
                    workerInstances.addAll(workersBatch);
                }
            } else {
                LOG.info("No Worker instance(s) requested!");
            }
            // just to be sure, everything is present, wait x seconds
            sleep(4);
            LOG.info("Cluster (ID: {}) successfully created!", clusterId);
            final String masterIp = config.isUseMasterWithPublicIp() ? masterInstance.getPublicIp() :
                    masterInstance.getPrivateIp();
            configure(masterInstance, workerInstances, environment.getSubnet().getCidr(), prepare);
            logFinishedInfoMessage(masterIp);
            saveGridPropertiesFile(masterIp);
        } catch (Exception e) {
            // print stacktrace only verbose mode, otherwise the message is fine
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                LOG.error(e.getMessage(), e);
            } else {
                LOG.error(e.getMessage());
            }
            if (Configuration.DEBUG) {
                logFinishedInfoMessage(
                        config.isUseMasterWithPublicIp() ? masterInstance.getPublicIp() : masterInstance.getPrivateIp());
            }

            Runtime.getRuntime().removeShutdownHook(this.interruptionMessageHook);
            return false;
        }
        Runtime.getRuntime().removeShutdownHook(this.interruptionMessageHook);
        return true;
    }

    /**
     * Adds worker instance(s) with specified batch to cluster.
     * Adopts the configuration from the other worker instances in batch
     * @return true, if worker instance(s) created successfully
     */
    public boolean createWorkerInstances(int batchIndex, int count) {
        ListIntent listIntent = providerModule.getListIntent(client, config);
        Map<String, Cluster> clusterMap = listIntent.getList();
        List<Instance> workersBatch = null;
        for (Cluster cluster : clusterMap.values()) {
            if (cluster.getClusterId().equals(clusterId)) {
                workersBatch = cluster.getWorkerInstances(batchIndex);
            }
        }
        if (workersBatch == null) {
            LOG.error("No workers with specified batch index {} found.", batchIndex);
            return false;
        }
        Instance workerBatchInstance = workersBatch.get(0);
        workerBatchInstance.loadConfiguration();
//        listIntent.loadInstanceConfiguration(workerBatchInstance);
        Configuration.WorkerInstanceConfiguration instanceConfiguration =
                (Configuration.WorkerInstanceConfiguration) workerBatchInstance.getConfiguration();
        instanceConfiguration.setCount(count);
        String workerNameTag = WORKER_NAME_PREFIX + "-" + clusterId;
        int workerIndex = workersBatch.size();
        List<Instance> additionalWorkers =
                launchAdditionalClusterWorkerInstances(batchIndex, workerIndex, instanceConfiguration, workerNameTag);
        if (additionalWorkers == null) {
            return false;
        } else {
            workersBatch.addAll(additionalWorkers);
            workerInstances.addAll(workersBatch);
        }
        return true;
    }

    /**
     * Start the configured cluster master instance.
     *
     * @param masterNameTag The generated name tag for the master instance.
     * @return master Instance
     */
    protected abstract Instance launchClusterMasterInstance(String masterNameTag);

    /**
     * Start the batch of cluster worker instances.
     * @return List of worker Instances
     */
    protected abstract List<Instance> launchClusterWorkerInstances(
            int batchIndex, Configuration.WorkerInstanceConfiguration instanceConfiguration, String workerNameTag);

    /**
     * Start additional cluster worker instances in scaling up process with specified batch.
     * @return List of worker Instances
     */
    protected abstract List<Instance> launchAdditionalClusterWorkerInstances(
            int batchIndex, int workerIndex, Configuration.WorkerInstanceConfiguration instanceConfiguration, String workerNameTag);

    protected String buildWorkerInstanceName(int batchIndex, int workerIndex) {
        return WORKER_NAME_PREFIX + "-" + (batchIndex) + "-" + (workerIndex) + "-" + clusterId;
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
                .append(Configuration.KEYS_DIR).append("\\").append(config.getClusterKeyPair().getName())
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
                .append(Configuration.KEYS_DIR).append("/").append(config.getClusterKeyPair().getName())
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

    /**
     * Uploads ansible scripts via SSH to master and rolls out on specified nodes.
     * @param masterInstance master
     * @param workerInstances cluster worker nodes
     * @param subnetCidr used to allocate IP addresses
     * @param prepare check if in preparation
     * @throws ConfigurationException thrown by 'uploadAnsibleToMaster' and 'installAndExecuteAnsible'
     *    in the case anything failed during the upload or ansible run. The exception is caught by
     *    'launchClusterInstances'.
     *    But not closing the sshSession blocks the JVM to exit(). Therefore we have to catch the
     *    ConfigurationException, close the sshSession and throw a new ConfigurationException
     */
    private void configure(final Instance masterInstance, final List<Instance> workerInstances,
                           final String subnetCidr, final boolean prepare) throws ConfigurationException {
        AnsibleHostsConfig ansibleHostsConfig = new AnsibleHostsConfig(config, workerInstances);
        AnsibleConfig ansibleConfig = new AnsibleConfig(config, providerModule.getBlockDeviceBase(), subnetCidr,
                masterInstance, workerInstances);
        ansibleConfig.setMasterMounts(masterDeviceMapper);

        final String masterIp = config.isUseMasterWithPublicIp() ? masterInstance.getPublicIp() :
                masterInstance.getPrivateIp();
        LOG.info("Now configuring...");
        boolean configured = false;
        boolean sshPortIsReady = SshFactory.pollSshPortIsAvailable(masterIp);
        if (sshPortIsReady) {
            try {
                LOG.info("Trying to connect to master...");
                sleep(4);
                // Create new Session to avoid packet corruption.
                Session sshSession = SshFactory.createSshSession(config,masterIp);
                if (sshSession != null) {
                    // Start connection attempt
                    sshSession.connect();
                    LOG.info("Connected to master!");
                    try {
                        uploadAnsibleToMaster(sshSession, ansibleHostsConfig, ansibleConfig, workerInstances);
                        installAndExecuteAnsible(sshSession, prepare);
                    } catch (ConfigurationException e) {
                        throw new ConfigurationException(e.getMessage());
                    } finally {
                        sshSession.disconnect();
                    }
                }
            } catch (IOException | JSchException e) {
                if (VerboseOutputFilter.SHOW_VERBOSE) {
                    e.printStackTrace();
                }
                throw new ConfigurationException(e);
            }
        }
        LOG.info(I, "Cluster has been configured.");
    }

    /**
     * Uploads ansible roles to master instance.
     *
     * @param sshSession ssh connection to master
     * @param hostsConfig Configuration and list of worker IPs
     * @param commonConfig common Configuration
     * @param workerInstances list of worker instances
     * @throws JSchException possible SSH connection error
     * @throws ConfigurationException possible upload error
     */
    private void uploadAnsibleToMaster(Session sshSession, AnsibleHostsConfig hostsConfig,
                                          AnsibleConfig commonConfig, List<Instance> workerInstances) throws JSchException, ConfigurationException {

        ChannelSftp channel = (ChannelSftp) sshSession.openChannel("sftp");
        LOG.info("Uploading Ansible playbook to master instance.");
        LOG.info(V, "Connecting sftp channel...");
        channel.connect();
        try {
            // Collect Ansible files from resources for upload
            AnsibleResources resources = new AnsibleResources();
            uploadResourcesFiles(resources, channel);

            // Divide into master and worker roles to write in site.yml
            Map<String, String> customMasterRoles = new LinkedHashMap<>();
            Map<String, String> customWorkerRoles = new LinkedHashMap<>();

            // create Role Upload Path on master
            createSFTPFolder(channel,AnsibleResources.UPLOAD_PATH);


            // Add "extra" Ansible role
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
                    case "worker":
                    case "workers":
                        customWorkerRoles.put(roleName, roleVarsFile);
                        break;
                    default:
                        customMasterRoles.put(roleName, roleVarsFile);
                        customWorkerRoles.put(roleName, roleVarsFile);
                }

                uploadAnsibleRole(channel, role.getFile());
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
                    case "worker":
                    case "workers":
                        customWorkerRoles.put(roleName, roleVarsFile);
                        break;
                    default:
                        customMasterRoles.put(roleName, roleVarsFile);
                        customWorkerRoles.put(roleName, roleVarsFile);
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
                     customMasterRoles, customWorkerRoles);

            // Write requirements file for ansible-galaxy support
            if (!ansibleGalaxyRoles.isEmpty()) {
                commonConfig.writeRequirementsFile(channel.put(channel.getHome() + "/"
                        + AnsibleResources.REQUIREMENTS_CONFIG_FILE));
            }

            // Write worker instance specific configuration file
            for (Instance worker : workerInstances) {
                String filename = channel.getHome() + "/" + AnsibleResources.CONFIG_ROOT_PATH + "/"
                        + worker.getPrivateIp() + ".yml";
                commonConfig.writeInstanceFile(worker, channel.put(filename));
            }

        } catch (SftpException | IOException e) {
            throw new ConfigurationException(e);

        } finally {
            channel.disconnect();
        }

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
            createSFTPFolder(channel,fullPath);
        }
    }

    /** Creates a folder for given (if not already exists
     *
     * @param channel client side of sftp server channel
     * @param path path to be created
     * @throws SftpException possible SFTP failure
     */
    private void createSFTPFolder(ChannelSftp channel, String path) throws SftpException {
        try {
            channel.cd(path);
        } catch (SftpException e) {
            LOG.info(V, "SFTP: Create folder {}", path);
            channel.mkdir(path);
        }
        channel.cd(channel.getHome());
    }

    /**
     * Uploads single ansible role (.tar.gz, .tgz) to remote instance to temporary folder.
     *
     * @param channel client side of sftp server channel
     * @param roleFile path/to/role on local machine
     * @throws SftpException possible SFTP failure
     * @throws IOException possible File failure
     */
    private void uploadAnsibleRole(ChannelSftp channel, String roleFile)
            throws SftpException, IOException {
        String remotePath = AnsibleResources.UPLOAD_PATH + getSingleFileName(roleFile);
        InputStream stream = new FileInputStream(roleFile);
        // target location on master
        LOG.info(V, "SFTP: Upload file {} to {}", roleFile, remotePath );
        // Upload the file stream via sftp
        channel.put(stream, remotePath );
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
     * @throws JSchException ssh openChannel exception
     * @throws IOException BufferedReader exceptions
     * @throws ConfigurationException if configuration was unsuccesful
     */
    private void installAndExecuteAnsible(final Session sshSession,  final boolean prepare)
            throws IOException, JSchException, ConfigurationException {
        LOG.info("Ansible is now configuring your cloud instances. This might take a while.");

        String execCommand = ShellScriptCreator.getMasterAnsibleExecutionScript(prepare, config);
        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");

        LineReaderRunnable stdout = new LineReaderRunnable(channel.getInputStream(), true);
        LineReaderRunnable stderr = new LineReaderRunnable(channel.getErrStream(), false);

        // Create threads ...
        Thread t_stdout = new Thread(stdout);
        Thread t_stderr = new Thread(stderr);

        // ... start them ...
        t_stdout.start();
        t_stderr.start();

        // ... start ansible ...
        channel.setCommand(execCommand);
        // ... connect channel
        channel.connect();

        // ... wait for threads finished ...
        try {
            t_stdout.join();
            t_stderr.join();
        } catch (InterruptedException e) {
            throw new ConfigurationException("Exception occurred during evaluation of ansible output!");
        }

        // and  disconnect channel
        channel.disconnect();

        if (stdout.getReturnCode() != 0) {
            throw new ConfigurationException("Cluster configuration failed.\n" + stdout.getReturnMsg());
        }
    }

    public Configuration getConfig() {
        return config;
    }

    public Instance getMasterInstance() {
        return masterInstance;
    }

    public List<Instance> getWorkerInstances() {
        return workerInstances;
    }

}

/**
 * Watch and parse the stdout and stderr stream at the same time.
 * Since BufferReader.readline() blocks,
 * the only solution I found is to work with separate threads for stdout and stderror of the ssh channel.
 *
 * The following code snipset seems to be more complicated than it should be (in other languages).
 * If you find a better solution feel free to replace it.
 */
class LineReaderRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(CreateCluster.class);
    private static final String LOG_MSG_BG = "\"[BIBIGRID] ";

    private BufferedReader br;

    private boolean regular;

    private int returnCode = -1;
    private String returnMsg = "";

    /**
     * Initializes new Runnable with input / error stream.
     * @param stream InputStream with regular and error Logging
     * @param regular true, if regular stream, false, if error stream
     */
    LineReaderRunnable(InputStream stream, boolean regular) {
        this.br = new BufferedReader(new InputStreamReader(stream));
        this.regular = regular;
    }

    private void work_on_line(String line) {
        if (regular) {
            if (line.contains("CONFIGURATION FINISHED")) {
                returnCode = 0;
                returnMsg = ""; // clear possible msg
            } else if (line.contains("failed:")) {
                returnMsg = line;
            }
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                // in verbose mode show every line generated by ansible
                LOG.info(V, "{}", line);
            } else {
                // otherwise show only ansible msg containing "[BIBIGRID]"
                int indexOfLogMessage = line.indexOf(LOG_MSG_BG);
                if (indexOfLogMessage > 0) {
                    LOG.info("[Ansible] {}",  line.substring(indexOfLogMessage + LOG_MSG_BG.length(), line.length() - 1));
                }
            }
        } else {
            // Check for real errors and print them to the error log ...
            if (line.toLowerCase().contains("ERROR".toLowerCase())) {
                LOG.error("{}", line);
            } else { // ... and everything else as warning !
                LOG.warn(V,"{}",line);
            }
        }
    }

    private void work_on_exception(Exception e) {
        LOG.error("Evaluate stderr : " + e.getMessage());
        returnCode = 1;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = br.readLine()) != null ) {
                work_on_line(line);
            }
        } catch (IOException ex) {
            work_on_exception(ex);
        }
    }

    int getReturnCode(){
        return returnCode;
    }

    String getReturnMsg(){
        return returnMsg;
    }
}
package de.unibi.cebitec.bibigrid.core.intents;

import com.jcraft.jsch.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import de.unibi.cebitec.bibigrid.core.util.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
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
 *         Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public abstract class CreateCluster extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(CreateCluster.class);
    public static final String PREFIX = "bibigrid";
    public static final String SEPARATOR = "-";
    private static final String MASTER_NAME_PREFIX = PREFIX + SEPARATOR + "master";
    private static final String WORKER_NAME_PREFIX = PREFIX + SEPARATOR + "worker";

    protected final ProviderModule providerModule;
    protected final Configuration config;
    protected Cluster cluster;
    protected CreateClusterEnvironment environment;

    protected DeviceMapper masterDeviceMapper;

    private Thread interruptionMessageHook;

    /**
     * Creates a cluster from scratch or uses clusterId to manually / dynamically scale an available cluster.
     * @param providerModule Specific cloud provider access
     * @param config Configuration
     * @param clusterId optional if cluster already started and has to be scaled
     */
    protected CreateCluster(ProviderModule providerModule, Configuration config, String clusterId) {
        String cid = clusterId != null ? clusterId : generateClusterId();
        cluster = new Cluster(cid);
        this.providerModule = providerModule;
        this.config = config;
        this.interruptionMessageHook = new Thread(() ->
                LOG.error("Cluster setup was interrupted!\n\n" +
                        "Please clean up the remains using: -t {}\n\n", cluster.getClusterId()));
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
        return environment = providerModule.getClusterEnvironment(this);
    }

    /**
     * Configure and manage Master- and Worker-instance(s) to launch.
     *
     * @return true, if configuration successful
     */
    public boolean configureClusterInstances() {
        Map<InstanceType, Integer> instanceTypes = new HashMap<>();
        instanceTypes.put(config.getMasterInstance().getProviderType(), 1);
        LOG.warn("Put master instance type {} into quotas map.", config.getMasterInstance().getProviderType());
        for (Configuration.WorkerInstanceConfiguration worker : config.getWorkerInstances()) {
            InstanceType providerType = worker.getProviderType();
            if (instanceTypes.containsKey(providerType)) {
                instanceTypes.put(worker.getProviderType(), instanceTypes.get(providerType) + worker.getCount());
            } else {
                instanceTypes.put(worker.getProviderType(), worker.getCount());
            }
            LOG.warn("Put worker instance type {} into quotas map.", worker.getProviderType());
        }
        if (providerModule.getValidateIntent(config).
                checkQuotasExceeded(instanceTypes)) {
            LOG.error("Quotas exceeded. No additional workers could be launched.");
            return false;
        }
        this.configureClusterMasterInstance();
        this.configureClusterWorkerInstance();
        return true;
    }

    /**
     * Configure and manage Master-instance to launch.
     */
    public void configureClusterMasterInstance() {
        List<Configuration.MountPoint> masterVolumeToMountPointMap = resolveMountSources(config.getMasterMounts());
        InstanceType masterSpec = config.getMasterInstance().getProviderType();
        int deviceOffset = masterSpec.getEphemerals() + masterSpec.getSwap();
        masterDeviceMapper = new DeviceMapper(providerModule, masterVolumeToMountPointMap, deviceOffset);
        LOG.info("Master instance configured.");
    }

    /**
     * Configure and manage Worker-instances to launch.
     */
    public void configureClusterWorkerInstance() {
        LOG.info("Worker instance(s) configured.");
    }

    /**
     * Resolve the mount source volumes or snapshots.
     */
    protected abstract List<Configuration.MountPoint> resolveMountSources(List<Configuration.MountPoint> mountPoints);

    /**
     * Start the configured cluster now.
     */
    public boolean launchClusterInstances() {
        try {
            String masterNameTag = MASTER_NAME_PREFIX + SEPARATOR + cluster.getClusterId();
            Instance masterInstance = launchClusterMasterInstance(masterNameTag);
            if (masterInstance == null) {
                return false;
            }
            List<Instance> workerInstances = new ArrayList<>();
            int totalWorkerInstanceCount = config.getWorkerInstanceCount();
            if (totalWorkerInstanceCount > 0) {
                LOG.info("Requesting {} worker instance(s) with {} different configurations...",
                        totalWorkerInstanceCount, config.getWorkerInstances().size());
                // loop over all batches
                for (int i = 0; i < config.getWorkerInstances().size(); i++) {
                    Configuration.WorkerInstanceConfiguration instanceConfiguration = config.getWorkerInstances().get(i);
                    LOG.info("Requesting {} worker instance(s) with same configuration...",
                            instanceConfiguration.getCount());
                    String workerNameTag = WORKER_NAME_PREFIX + SEPARATOR + cluster.getClusterId();
                    List<Instance> workersBatch = launchClusterWorkerInstances(i, instanceConfiguration, workerNameTag);
                    if (workersBatch == null) {
                        return false;
                    }
                    workerInstances.addAll(workersBatch);
                }
            } else {
                LOG.info("No Worker instance(s) requested!");
            }
            // set cluster initialization values
            cluster.setMasterInstance(masterInstance);
            cluster.setWorkerInstances(workerInstances);
            cluster.setPublicIp(masterInstance.getPublicIp());
            cluster.setPrivateIp(masterInstance.getPrivateIp());
            // just to be sure, everything is present, wait x seconds
            sleep(4);
            LOG.info("Cluster (ID: {}) successfully created!", cluster.getClusterId());
            final String masterIp = config.isUseMasterWithPublicIp() ? masterInstance.getPublicIp() :
                    masterInstance.getPrivateIp();
            configureAnsible();
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
                        config.isUseMasterWithPublicIp() ?
                                cluster.getMasterInstance().getPublicIp() : cluster.getMasterInstance().getPrivateIp());
            }

            Runtime.getRuntime().removeShutdownHook(this.interruptionMessageHook);
            return false;
        }
        Runtime.getRuntime().removeShutdownHook(this.interruptionMessageHook);
        return true;
    }

    /**
     * Adds additional worker instance(s) with specified batch to cluster.
     * Adopts the configuration from the other worker instances in batch
     * @return true, if worker instance(s) created successfully
     */
    public boolean createWorkerInstances(int batchIndex, int count) {
        if (cluster == null) {
            LOG.error("No cluster with specified clusterId {} found", cluster.getClusterId());
            return false;
        }
        try {
            config.getClusterKeyPair().setName(CreateCluster.PREFIX + cluster.getClusterId());
            config.getClusterKeyPair().load();
        } catch (IOException io) {
            LOG.error("Update may not be finished properly due to a KeyPair error.");
            io.printStackTrace();
            return false;
        }
        Session sshSession = null;
        boolean success = true;
        try {
            if (!SshFactory.pollSshPortIsAvailable(cluster.getMasterInstance().getPublicIp())) {
                return false;
            }
            sshSession = SshFactory.createSshSession(
                    config.getSshUser(),
                    config.getClusterKeyPair(),
                    cluster.getMasterInstance().getPublicIp());
            sshSession.connect();
            ChannelSftp channelSftp = (ChannelSftp) sshSession.openChannel("sftp");
            channelSftp.connect();
            Configuration.WorkerInstanceConfiguration instanceConfiguration;
            List<Instance> workersBatch = cluster.getWorkerInstances(batchIndex);
            if (workersBatch == null || workersBatch.isEmpty()) {
                // connect to master i.o. to load worker specification file
                String specification_file = channelSftp.getHome() + "/" + AnsibleResources.WORKER_SPECIFICATION_FILE;
                String file_path = channelSftp.getHome() + "/" + AnsibleResources.CONFIG_ROOT_PATH;
                Vector<ChannelSftp.LsEntry> dirEntries = channelSftp.ls(file_path);
                List<String> dirFiles = new ArrayList<>();
                for (ChannelSftp.LsEntry file : dirEntries) {
                    String lsFileAbsolutePath = AnsibleResources.CONFIG_ROOT_PATH + file.getFilename();
                    dirFiles.add(lsFileAbsolutePath);
                }
                if (!dirFiles.contains(AnsibleResources.WORKER_SPECIFICATION_FILE)) {
                    LOG.error("No workers with specified batch index {} found.", batchIndex);
                    channelSftp.disconnect();
                    sshSession.disconnect();
                    return false;
                }
                InputStream in = channelSftp.get(specification_file);
                instanceConfiguration = AnsibleConfig.readWorkerSpecificationFile(in, batchIndex);
                if (instanceConfiguration == null) {
                    LOG.error("No workers with specified batch index {} found.", batchIndex);
                    channelSftp.disconnect();
                    sshSession.disconnect();
                    return false;
                }
                instanceConfiguration.setProviderType(providerModule.getInstanceType(config, instanceConfiguration.getType()));
            } else {
                Instance workerBatchInstance = workersBatch.get(0);
                instanceConfiguration =
                        (Configuration.WorkerInstanceConfiguration) workerBatchInstance.getConfiguration();
            }
            Map<InstanceType, Integer> instanceTypes = new HashMap<>();
            instanceTypes.put(instanceConfiguration.getProviderType(), count);
            if (providerModule.getValidateIntent(config).
                    checkQuotasExceeded(instanceTypes)) {
                LOG.error("Quotas exceeded. No additional workers could be launched.");
                return false;
            }
            LOG.info("Creating {} worker " + (count == 1 ? "instance" : "instances") + " for batch {}.", count, batchIndex);
            instanceConfiguration.setCount(count);
            String workerNameTag = WORKER_NAME_PREFIX + "-" + cluster.getClusterId();
            int workerIndex = workersBatch.size() + 1;
            List<Instance> additionalWorkers =
                     launchAdditionalClusterWorkerInstances(batchIndex, workerIndex, instanceConfiguration, workerNameTag);
            if (additionalWorkers == null) {
                LOG.error("No additional workers could be launched.");
                channelSftp.disconnect();
                sshSession.disconnect();
                return false;
            } else {
                // loadIntent cluster ...
                // workerInstances = cluster.getWorkerInstances();
                // workerInstances.addAll(additionalWorkers);
                cluster.addWorkerInstances(additionalWorkers);
            }
            config.getClusterKeyPair().setName(CreateCluster.PREFIX + cluster.getClusterId());
            config.getClusterKeyPair().load();
            AnsibleConfig.updateAnsibleWorkerLists(sshSession, config, cluster, providerModule);
            SshFactory.executeScript(sshSession, ShellScriptCreator.executeScaleTasksOnMaster(Scale.up).concat(ShellScriptCreator.executePlaybookOnWorkers(additionalWorkers)));
            if (additionalWorkers.size() == 1) {
                LOG.info(I, "{} instance has been successfully added to cluster {}.", additionalWorkers.size(), cluster.getClusterId());
            } else {
                LOG.info(I, "{} instances have been successfully added to cluster {}.", additionalWorkers.size(), cluster.getClusterId());
            }
        } catch (JSchException sshError) {
            LOG.error("Update may not be finished properly due to a connection error.");
            success = false;
        } catch (IOException io) {
            LOG.error("Update may not be finished properly due to a KeyPair error.");
            success = false;
        } catch (ConfigurationException ce) {
            LOG.error("Update may not be finished properly due to a configuration error.");
            success = false;
        } catch (InstanceTypeNotFoundException | SftpException e) {
            e.printStackTrace();
        } finally {
            // disconnect sshSession if connected
            if (sshSession != null && sshSession.isConnected()) {
                sshSession.disconnect();
            }
        }
        return success;
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
    protected abstract List<Instance> launchClusterWorkerInstances(int batchIndex, Configuration.WorkerInstanceConfiguration instanceConfiguration, String workerNameTag);

    /**
     * Start additional cluster worker instances in scaling up process with specified batch.
     * @return List of worker Instances
     */
    protected abstract List<Instance> launchAdditionalClusterWorkerInstances(
            int batchIndex, int workerIndex,
            Configuration.WorkerInstanceConfiguration instanceConfiguration, String workerNameTag);

    protected String buildWorkerInstanceName(int batchIndex, int workerIndex) {
        return WORKER_NAME_PREFIX + SEPARATOR + (batchIndex) + SEPARATOR + (workerIndex) + "-" + cluster.getClusterId();
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
        sb.append("The cluster id of your started cluster is: ").append(cluster.getClusterId()).append("\n\n");
        sb.append("You can easily terminate the cluster at any time with:\n")
                .append("./bibigrid -t ").append(cluster.getClusterId()).append(" ");
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
        sb.append("The cluster id of your started cluster is: ").append(cluster.getClusterId()).append("\n\n");
        sb.append("You can easily terminate the cluster at any time with:\n")
                .append("./bibigrid -t ").append(cluster.getClusterId()).append(" ");
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
            gp.setProperty("clusterId", cluster.getClusterId());
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
     * @throws ConfigurationException thrown by 'uploadAnsibleToMaster' and 'executeScript'
     *    in the case anything failed during the upload or ansible run.
     *    Not closing the sshSession blocks the JVM to exit(). Therefore we have to catch the
     *    ConfigurationException, close the sshSession and throw a new ConfigurationException
     */
    private void configureAnsible() throws ConfigurationException {
        final String masterIp = config.isUseMasterWithPublicIp() ? cluster.getPublicIp() :
                cluster.getPrivateIp();
        LOG.info("Now configuring...");
        boolean sshPortIsReady = SshFactory.pollSshPortIsAvailable(masterIp);
        if (sshPortIsReady) {
            try {
                LOG.info("Trying to connect to master...");
                sleep(4);
                // Create new Session to avoid packet corruption.
                Session sshSession = SshFactory.createSshSession(config.getSshUser(), config.getClusterKeyPair(), masterIp);
                if (sshSession != null) {
                    // Start connection attempt
                    sshSession.connect();
                    LOG.info("Connected to master!");
                    try {
                        uploadAnsibleToMaster(sshSession);
                        LOG.info("Ansible is now configuring your cloud instances. This might take a while.");
                        SshFactory.executeScript(sshSession, ShellScriptCreator.getMasterAnsibleExecutionScript(config));
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
     * @throws JSchException possible SSH connection error
     * @throws ConfigurationException possible upload error
     */
    private void uploadAnsibleToMaster(Session sshSession) throws JSchException, ConfigurationException {
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
                // Written in site.yml, has to be 'vars/ROLE_NAME'
                String roleVarsFile = "";
                if (roleVars != null && !roleVars.isEmpty()) {
                    roleVarsFile = AnsibleResources.VARS_PATH + roleName + "-vars.yml";
                    AnsibleConfig.writeAnsibleVarsFile(channel.put(channel.getHome() + "/" +
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
                    AnsibleConfig.writeAnsibleVarsFile(channel.put(channel.getHome() + "/" +
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
            String channel_dir = channel.getHome() + "/";

            // Write custom site file
            String site_file = channel_dir + AnsibleResources.SITE_CONFIG_FILE;
            AnsibleConfig.writeSiteFile(channel.put(site_file),
                    customMasterRoles, customWorkerRoles);

            AnsibleConfig.writeHostsFile(channel, config.getSshUser(), cluster.getWorkerInstances(), config.useHostnames());

            // files for common configuration
            String login_file = channel_dir + AnsibleResources.COMMONS_LOGIN_FILE;
            String instances_file = channel_dir + AnsibleResources.COMMONS_INSTANCES_FILE;
            String config_file = channel_dir + AnsibleResources.COMMONS_CONFIG_FILE;
            String specification_file = channel_dir + AnsibleResources.WORKER_SPECIFICATION_FILE;

            // streams for common configuration
            OutputStream login_stream = channel.put(login_file);
            OutputStream instances_stream = channel.put(instances_file);
            OutputStream config_stream = channel.put(config_file);
            OutputStream specification_stream = channel.put(specification_file);

            // write files using stream
            AnsibleConfig.writeLoginFile(login_stream, config);
            AnsibleConfig.writeInstancesFile(instances_stream, cluster.getMasterInstance(), cluster.getWorkerInstances(), masterDeviceMapper, providerModule.getBlockDeviceBase());
            AnsibleConfig.writeConfigFile(config_stream, config, environment.getSubnet().getCidr());
            // TODO network should be written in instance configuration when initializing
            // security group und server group
            AnsibleConfig.writeWorkerSpecificationFile(specification_stream, config, environment);

            // Write requirements file for ansible-galaxy support
            if (!ansibleGalaxyRoles.isEmpty()) {
                String requirements_file = channel_dir + AnsibleResources.REQUIREMENTS_CONFIG_FILE;
                AnsibleConfig.writeRequirementsFile(channel.put(requirements_file), ansibleGalaxyRoles);
            }

            // Write worker instance specific configuration file
            for (Instance worker : cluster.getWorkerInstances()) {
                String filename = channel_dir + AnsibleResources.CONFIG_ROOT_PATH + "/"
                        + worker.getPrivateIp() + ".yml";
                AnsibleConfig.writeSpecificInstanceFile(channel.put(filename), worker, providerModule.getBlockDeviceBase());
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
            createSFTPFolder(channel, fullPath);
        }
    }

    /**
     * Creates a folder for given (if not already exists
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
     * @param channel  client side of sftp server channel
     * @param roleFile path/to/role on local machine
     * @throws SftpException possible SFTP failure
     * @throws IOException   possible File failure
     */
    private void uploadAnsibleRole(ChannelSftp channel, String roleFile)
            throws SftpException, IOException {
        String remotePath = AnsibleResources.UPLOAD_PATH + getSingleFileName(roleFile);
        InputStream stream = new FileInputStream(roleFile);
        // target location on master
        LOG.info(V, "SFTP: Upload file {} to {}", roleFile, remotePath);
        // Upload the file stream via sftp
        channel.put(stream, remotePath);
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

    public Configuration getConfig() {
        return config;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }
}


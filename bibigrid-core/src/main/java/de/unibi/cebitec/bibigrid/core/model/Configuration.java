package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Configuration {
    /* public const */
    public static boolean DEBUG = false;
    public static final String DEFAULT_WORKSPACE = "$HOME";
    public static final String CONFIG_DIR = System.getProperty("user.home")+System.getProperty("file.separator")+".bibigrid";
    public static final String KEYS_DIR = CONFIG_DIR + System.getProperty("file.separator")+"keys";
    public static final FileAttribute KEYS_PERMS = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    /* protected const */
    protected static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    @Deprecated
    private static final String DEFAULT_CONFIG_FILENAME = "configuration.yml";
    private static final String PROPERTIES_FILEPATH_PARAMETER = "o";

    public Configuration() throws IOException {
            Path config_path = Paths.get(CONFIG_DIR);
            Path key_path = Paths.get(KEYS_DIR);

            if (Files.notExists(config_path)) {
                LOG.info("Creating BiBiGrid configuration directory '{}'",CONFIG_DIR);
                Files.createDirectory(config_path);
            }
            if (Files.notExists(key_path)) {
                LOG.info("Creating BiBiGrid key directory '{}'", KEYS_DIR);
                Files.createDirectory(key_path);
            }
    }

    public static Configuration loadConfiguration(Class<? extends Configuration> configurationClass, String path) throws ConfigurationException{
        Path propertiesFilePath = null;

        Path defaultPropertiesFilePath = Paths.get(CONFIG_DIR, DEFAULT_CONFIG_FILENAME);
        if (path != null)  {
            Path newPath = Paths.get(path);
            if (Files.isReadable(newPath)) {
                propertiesFilePath = newPath;
                LOG.info("Using alternative config file: '{}'.", propertiesFilePath.toString());
            } else {
                LOG.error("Alternative config ({}) file is not readable. Falling back to default: '{}'", newPath.toString(), defaultPropertiesFilePath.toString());
            }
        }
        if (propertiesFilePath == null) {
            propertiesFilePath = defaultPropertiesFilePath;
        }

        try {
            return new Yaml().loadAs(new FileInputStream(propertiesFilePath.toFile()), configurationClass);
        } catch (FileNotFoundException e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            throw new ConfigurationException("Failed to load properties file.", e);
        } catch (YAMLException e) {
            throw new ConfigurationException("Failed to parse configuration file. "+e.getMessage(), e);
        }

    }


    /* properties */
    private String mode;
    private String user = System.getProperty("user.name");
    private String sshUser = "ubuntu";
    private String keypair;
    private String sshPublicKeyFile;
    private List<String> sshPublicKeyFiles = new ArrayList<>();
    private List<String> sshPublicKeys = new ArrayList<>();
    private String id;
    private ClusterKeyPair clusterKeyPair = new ClusterKeyPair();
    @Deprecated
    private String sshPrivateKeyFile;
    private String alternativeConfigPath;
    private String gridPropertiesFile;
    private String credentialsFile;
    private String region;
    private String availabilityZone;
    private String serverGroup;
    private List<Port> ports = new ArrayList<>();
    private boolean useMasterAsCompute;
    private boolean useMasterWithPublicIp = true;
    private InstanceConfiguration masterInstance = new InstanceConfiguration();
    private List<WorkerInstanceConfiguration> workerInstances = new ArrayList<>();
    private boolean oge;
    private boolean slurm;
    private boolean localDNSLookup;
    private String mungeKey;
    private boolean nfs = true;
    private boolean cloud9;
    private boolean theia;
    private boolean ganglia;
    private boolean zabbix;
    private ZabbixConf zabbixConf = new ZabbixConf();
    private List<String> nfsShares = new ArrayList<>(Arrays.asList("/vol/spool"));
    private List<MountPoint> masterMounts = new ArrayList<>();
    private List<MountPoint> extNfsShares = new ArrayList<>();
    private FS localFS = FS.XFS;
    private boolean debugRequests;
    private Properties ogeConf = OgeConf.initOgeConfProperties();
    private List<AnsibleRoles> ansibleRoles = new ArrayList<>();
    private List<AnsibleGalaxyRoles> ansibleGalaxyRoles = new ArrayList<>();

    private String network;
    private String subnet;

    private String[] clusterIds = new String [0];

    private String workspace = DEFAULT_WORKSPACE;

    public int getWorkerInstanceCount() {
        if (workerInstances == null) {
            return 0;
        }
        int workerInstanceCount = 0;
        for (WorkerInstanceConfiguration config : workerInstances) {
            workerInstanceCount += config.getCount();
        }
        return workerInstanceCount;
    }

    public void setGridPropertiesFile(String gridPropertiesFile) {
        this.gridPropertiesFile = gridPropertiesFile.trim();
    }

    public String getGridPropertiesFile() {
        return gridPropertiesFile;
    }

    public boolean isUseMasterAsCompute() {
        return useMasterAsCompute;
    }

    public void setUseMasterAsCompute(boolean useMasterAsCompute) {
        this.useMasterAsCompute = useMasterAsCompute;
    }

    public boolean isUseMasterWithPublicIp() {
        return useMasterWithPublicIp;
    }

    public void setUseMasterWithPublicIp(boolean useMasterWithPublicIp) {
        this.useMasterWithPublicIp = useMasterWithPublicIp;
    }

    @Deprecated
    public String getKeypair() {
        return keypair;
    }

    @Deprecated
    public void setKeypair(String keypair) {
        LOG.warn("Deprecation warning: Properties 'keypair' is not longer used.");
        this.keypair = keypair.trim();
        LOG.info(V, "Keypair name set. ({})", this.keypair);
    }

    public ClusterKeyPair getClusterKeyPair() {
        return clusterKeyPair;
    }



    public String getSshPublicKeyFile() {
        return sshPublicKeyFile;
    }

    public void setSshPublicKeyFile(String sshPublicKeyFile) {
        this.sshPublicKeyFile = sshPublicKeyFile.trim();
        LOG.info(V, "SSH public key file found. ({})", this.sshPublicKeyFile);
    }

    public List<String> getSshPublicKeyFiles() {
        return sshPublicKeyFiles;
    }

    public void setSshPublicKeyFiles(List<String> sshPublicKeyFiles) {
        this.sshPublicKeyFiles = sshPublicKeyFiles;
    }

    public List<String> getSshPublicKeys() {
        return sshPublicKeys;
    }

    public void setSshPublicKeys(List<String> sshPublicKeys) {
        this.sshPublicKeys = sshPublicKeys;
    }

    @Deprecated
    public String getSshPrivateKeyFile() {
        return sshPrivateKeyFile;
    }

    @Deprecated
    public void setSshPrivateKeyFile(String sshPrivateKeyFile) {
        LOG.warn("Deprecation warning: Properties 'sshPrivateKeyFile' is not longer used.");
        this.sshPrivateKeyFile = sshPrivateKeyFile.trim();
        LOG.info(V, "SSH private key file found. ({})", this.sshPrivateKeyFile);
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region.trim();
        LOG.info(V, "Region set. ({})", this.region);
    }

    public InstanceConfiguration getMasterInstance() {
        return masterInstance;
    }

    public void setMasterInstance(InstanceConfiguration masterInstance) {
        this.masterInstance = masterInstance;
        if (masterInstance != null) {
            StringBuilder display = new StringBuilder();
            display.append("[type=").append(masterInstance.getType()).append(", image=")
                    .append(masterInstance.getImage()).append("] ");
            LOG.info(V, "Master instance configuration set: {}", display);
        }
    }

    @Deprecated
    public List<WorkerInstanceConfiguration> getSlaveInstances() {
        LOG.warn("Property 'slaveInstances' is deprecated and will be removed in next major release. It is replaced 1:1 by 'workerInstances'.");
        return getWorkerInstances();
    }

    @Deprecated
    public void setSlaveInstances(List<WorkerInstanceConfiguration> workerInstances) {
            LOG.warn("Property 'slaveInstances' is deprecated and will be removed in next major release. It is replaced 1:1 by 'workerInstances'.");
            setWorkerInstances(workerInstances);
    }


    public List<WorkerInstanceConfiguration> getWorkerInstances() {
        return workerInstances;
    }

    public void setWorkerInstances(List<WorkerInstanceConfiguration> workerInstances) {
        this.workerInstances = workerInstances != null ? workerInstances : new ArrayList<>();
        if (workerInstances != null && !workerInstances.isEmpty()) {
            StringBuilder display = new StringBuilder();
            for (WorkerInstanceConfiguration instanceConfiguration : workerInstances) {
                display.append("[type=").append(instanceConfiguration.getType())
                        .append(", image=").append(instanceConfiguration.getImage())
                        .append(", count=").append(instanceConfiguration.getCount()).append("] ");
            }
            LOG.info(V, "Worker instance(s) configuration set: {}", display);
        }
    }

    /**
     * Helper getter so multiple instance types can be used in a simple for loop.
     */
    public List<WorkerInstanceConfiguration> getExpandedWorkerInstances() {
        List<WorkerInstanceConfiguration> result = new ArrayList<>();
        if (workerInstances.size() > 0) {
            int typeIndex = 0;
            int typeInstancesLeft = workerInstances.get(0).getCount();
            for (int i = 0; i < getWorkerInstanceCount(); i++) {
                result.add(workerInstances.get(typeIndex));
                // If we reach the count of the current type, move to the next instance type
                typeInstancesLeft--;
                if (typeInstancesLeft == 0) {
                    typeIndex += 1;
                    if (typeIndex < workerInstances.size()) {
                        typeInstancesLeft = workerInstances.get(typeIndex).getCount();
                    }
                }
            }
        }
        return result;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone.trim();
        LOG.info(V, "Availability zone set. ({})", this.availabilityZone);
    }

    public String getServerGroup() {
        return serverGroup;
    }

    public void setServerGroup(String serverGroup) {
        this.serverGroup = serverGroup.trim();
        LOG.info(V, "Server group set. ({})", this.serverGroup);
    }

    /**
     * Return a list of cluster id. Currently used for termination intent only.
     * @ToDo: Seems not the right place to store this information, since this is not part of a cluster configuration and only
     *      * necessary for termination intent.
     *
     * @return Return a list of cluster id.
     */
    public String[] getClusterIds() {
        return Arrays.copyOf(clusterIds, clusterIds.length);
    }


    /**
     * Set the id of current
     * @param id
     */
    public void setId(String id) {
        this.id = id;

    }

    public String getId(){
        return id;
    }

    /**
     * Set the clusterid for termination intent either as a single cluster "id" or as multiple "id1/id2/id3".
     * @ToDo: Seems not the right place to store this information, since this is not part of a cluster configuration and only
     *      * necessary for termination intent.
     */
    public void setClusterIds(String clusterIds) {
        this.clusterIds = clusterIds == null ? new String[0] : clusterIds.split("[/,]");
    }

    /**
     * Set the clusterid for termination intent as cluster id list".
     * @ToDo:  Seems not the right place to store this information, since this is not part of a cluster configuration and only
     * necessary for termination intent.
     */
    public void setClusterIds(String[] clusterIds) {
        this.clusterIds = clusterIds == null ? new String[0] : clusterIds;
    }

    public List<Port> getPorts() {
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports != null ? ports : new ArrayList<>();
        if (ports != null && !ports.isEmpty()) {
            StringBuilder portsDisplay = new StringBuilder();
            for (Port port : ports) {
                portsDisplay.append(port.toString()).append(" ");
            }
            LOG.info(V, "Additional open ports set: {}", portsDisplay);
        }
    }

    public List<MountPoint> getMasterMounts() {
        return masterMounts;
    }

    public void setMasterMounts(List<MountPoint> masterMounts) {
        this.masterMounts = masterMounts != null ? masterMounts : new ArrayList<>();
    }

    public List<String> getNfsShares() {
        return nfsShares;
    }

    public void setNfsShares(List<String> nfsShares) {
        if (this.nfsShares != null) {
            this.nfsShares.addAll(nfsShares);
        } else {
            this.nfsShares = new ArrayList<>(nfsShares);
        }
        if (this.nfsShares != null && !this.nfsShares.isEmpty()) {
            StringBuilder nfsSharesDisplay = new StringBuilder();
            for (String share : this.nfsShares) {
                nfsSharesDisplay.append(share).append(" ");
            }
            LOG.info(V, "NFS shares set: {}", nfsSharesDisplay);
        }
    }

    public List<MountPoint> getExtNfsShares() {
        return extNfsShares;
    }

    public void setExtNfsShares(List<MountPoint> extNfsShares) {
        this.extNfsShares = extNfsShares != null ? extNfsShares : new ArrayList<>();
    }

    public boolean isAlternativeConfigFile() {
        return alternativeConfigPath != null;
    }

    public String getAlternativeConfigPath() {
        return alternativeConfigPath;
    }

    public void setAlternativeConfigPath(String alternativeConfigPath) {
        this.alternativeConfigPath = alternativeConfigPath.trim();
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network.trim();
    }

    public boolean isNfs() {
        return nfs;
    }

    public void setNfs(boolean nfs) {
        this.nfs = nfs;
        LOG.info(V, "NFS support {}.", nfs ? "enabled" : "disabled");
    }

    public boolean isOge() {
        return oge;
    }

    public void setOge(boolean oge) {
        this.oge = oge;
        if (oge) {
            LOG.warn("GridEngine (oge) support is deprecated (only supported using Ubuntu 16.04.) " +
                    "and will be removed in the near future! Please use Slurm instead.");
        }
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode.trim();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user.trim();
    }

    public String getSshUser() {
        return sshUser;
    }

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser.trim();
    }

    public FS getLocalFS() {
        return localFS;
    }

    public void setLocalFS(FS localFS) {
        this.localFS = localFS;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet.trim();
    }

    public boolean isDebugRequests() {
        return debugRequests;
    }

    public void setDebugRequests(boolean debugRequests) {
        this.debugRequests = debugRequests;
        LOG.info(V, "Debug requests {}.", debugRequests ? "enabled" : "disabled");
    }

    public boolean isIDE() { return cloud9 || theia; }

    public boolean isCloud9() {
        return cloud9;
    }

    public void setCloud9(boolean cloud9) throws ConfigurationException {
        if (cloud9&&theia) {
            LOG.error("Only one IDE (either Theia or Cloud9) can be set.");
            throw new ConfigurationException("Only one IDE (either Theia or Cloud9) can be set.");
        }
        this.cloud9 = cloud9;
        LOG.info(V, "Cloud9 support {}.", cloud9 ? "enabled" : "disabled");
    }

    public boolean isTheia() {
        return theia;
    }

    public void setTheia(boolean theia) throws ConfigurationException {
        if (cloud9&&theia) {
            LOG.error("Only one IDE (either Theia or Cloud9) can be set.");
            throw new ConfigurationException("Only one IDE (either Theia or Cloud9) can be set.");
        }
        this.theia = theia;
        LOG.info(V, "Theia support {}.", theia ? "enabled" : "disabled");
    }

    public String getCredentialsFile() {
        return credentialsFile;
    }

    public void setCredentialsFile(String credentialsFile) {
        this.credentialsFile = credentialsFile;
    }

    public String getCloud9Workspace() {
        return getWorkspace();
    }

    public void setCloud9Workspace(String cloud9Workspace) {
        LOG.warn("Option cloud9Workspace is deprecated. Please use workspace instead.");
        setWorkspace(cloud9Workspace);
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        if (workspace == null || workspace.length() == 0) {
            workspace = DEFAULT_WORKSPACE;
        }
        this.workspace = workspace;
        LOG.info(V, "Workspace set: {}", workspace);
    }

    public boolean isLocalDNSLookup() {
        return localDNSLookup;
    }

    public void setLocalDNSLookup(boolean localDNSLookup) {
        this.localDNSLookup = localDNSLookup;
    }

    public boolean isSlurm() {
        return slurm;
    }

    public void setSlurm(boolean slurm) {
        this.slurm = slurm;
    }

    public String getMungeKey() {
        if (mungeKey == null) {
            // create a unique hash
            byte[] randomarray = new byte[32];
            Random random = new Random();
            for (int i = 0; i < 32; i++){
                randomarray[i] = (byte)(97 + random.nextInt(26));
            }
            new Random().nextBytes(randomarray);
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                mungeKey = bytesToHex(digest.digest(randomarray));
            } catch (NoSuchAlgorithmException e){
                LOG.warn("SHA-256 algorithm not found, proceed with unhashed munge key.");
                mungeKey = new String(randomarray, Charset.forName("UTF-8"));
            }
        }
        return mungeKey;
    }

    public void setMungeKey(String mungeKey) {
        this.mungeKey = mungeKey;
    }

    public boolean isGanglia() {
        return ganglia;
    }

    public void setGanglia(boolean ganglia) {
        this.ganglia = ganglia;
        if (ganglia) {
            LOG.warn("Ganglia (oge) support is deprecated (only supported using Ubuntu 16.04.) " +
                     "and will be removed in the near future. Please use Zabbix instead.");
        }
    }

    public boolean isZabbix() {
        return zabbix;
    }

    public void setZabbix(boolean zabbix) {
        this.zabbix = zabbix;
    }

    public ZabbixConf getZabbixConf() { return zabbixConf;}

    public void setZabbixConf(ZabbixConf zabbixConf) { this.zabbixConf =  zabbixConf;}

    public static class ZabbixConf {
        public ZabbixConf(){
        }

        private String db = "zabbix";
        private String db_user = "zabbix";
        private String db_password = "zabbix";
        private String timezone = "Europe/Berlin";
        private String server_name = "bibigrid";
        private String admin_password = "bibigrid";

        public String getDb() {
            return db;
        }

        public void setDb(String db) {
            this.db = db;
        }

        public String getDb_user() {
            return db_user;
        }

        public void setDb_user(String db_user) {
            this.db_user = db_user;
        }

        public String getDb_password() {
            return db_password;
        }

        public void setDb_password(String db_password) {
            this.db_password = db_password;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public String getServer_name() {
            return server_name;
        }

        public void setServer_name(String server_name) {
            this.server_name = server_name;
        }

        public String getAdmin_password() {
            return admin_password;
        }

        public void setAdmin_password(String admin_password) {
            this.admin_password = admin_password;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class InstanceConfiguration {
        public InstanceConfiguration() {
        }

        private String type;
        private String image;
        private InstanceType providerType;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type.trim();
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image.trim();
        }

        public InstanceType getProviderType() {
            return providerType;
        }

        public void setProviderType(InstanceType providerType) {
            this.providerType = providerType;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class WorkerInstanceConfiguration extends InstanceConfiguration {
        public WorkerInstanceConfiguration() {
        }

        private int count;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
            if (count < 0) {
                LOG.warn("Number of worker nodes has to be at least 0. ({})", count);
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class MountPoint {
        public MountPoint() {
        }

        public MountPoint(String source, String target) {
            this.source = source;
            this.target = target;
        }

        private String source;
        private String target;

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source.trim();
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target.trim();
        }
    }

    public enum FS {
        EXT2, EXT3, EXT4, XFS
    }


    /** private helper class that converts a byte array to an Hex String
     *
     * @param hash
     * @return
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte tmp : hash) {
            String hex = Integer.toHexString(0xff & tmp);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public Properties getOgeConf() {
        return ogeConf;
    }

    /**
     * Saves given values to ogeConf Properties.
     * @param ogeConf Properties
     */
    public void setOgeConf(Properties ogeConf) {
        for (String key : ogeConf.stringPropertyNames()) {
            this.ogeConf.setProperty(key, ogeConf.getProperty(key));
        }
    }

    /**
     * Provides support for GridEngine global configuration.
     */
    public static class OgeConf extends Properties {
        public static final String GRIDENGINE_FILES = "/playbook/roles/master/files/gridengine/";
        public static final String GLOBAL_OGE_CONF = GRIDENGINE_FILES + "global.conf";

        public OgeConf(Properties defaultProperties) {
            super(defaultProperties);
        }

        private static OgeConf initOgeConfProperties() {
            try {
                // Create and load default properties
                Properties defaultProperties = new Properties();
                defaultProperties.load(Configuration.class.getResourceAsStream(GLOBAL_OGE_CONF));
                // create global properties with default
                return new OgeConf(defaultProperties);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Checks if custom ansible roles used.
     *
     * @return true, if ansible roles given in configuration
     */
    public boolean hasCustomAnsibleRoles() {
        return ansibleRoles != null && !ansibleRoles.isEmpty();
    }

    /**
     * Checks if custom ansible-galaxy roles used.
     *
     * @return true, if ansible-galaxy roles given in configuration
     */
    public boolean hasCustomAnsibleGalaxyRoles() {
        return ansibleGalaxyRoles != null && !ansibleGalaxyRoles.isEmpty();
    }

    /**
     * @return List of Ansible roles
     */
    public List<AnsibleRoles> getAnsibleRoles() {
        return ansibleRoles;
    }

    public void setAnsibleRoles(List<AnsibleRoles> ansibleRoles) {
        if (this.ansibleRoles != null) {
            this.ansibleRoles.addAll(ansibleRoles);
        } else {
            this.ansibleRoles = new ArrayList<>(ansibleRoles);
        }
    }

    /**
     * @return List of Ansible Galaxy roles
     */
    public List<AnsibleGalaxyRoles> getAnsibleGalaxyRoles() {
        return ansibleGalaxyRoles;
    }

    public void setAnsibleGalaxyRoles(List<AnsibleGalaxyRoles> ansibleGalaxyRoles) {
        this.ansibleGalaxyRoles = (this.ansibleGalaxyRoles == null) ? ansibleGalaxyRoles : new ArrayList<>(ansibleGalaxyRoles);
    }

    /**
     * Provides support for (local) Ansible roles and playbooks.
     *
     * String name      : (optional) name of (ansible-galaxy) role or playbook, default is given name
     * String hosts     : host (master / worker / all)
     * Map vars         : (optional) additional key - value pairs of role
     * String varsFile  : (optional) file containing key - value pairs of role
     * String file      : file of role
     */
    public static class AnsibleRoles {
        private String name;
        private String file;
        private String hosts;
        private Map<String, Object> vars = new HashMap<>();
        private String varsFile;

        public String getName() {
            return name == null ? file : name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHosts() {
            return hosts;
        }

        public void setHosts(String hosts) {
            // Exception for deprecated values
            if (hosts.equals("slave") || hosts.equals("slaves")) {
                LOG.warn("Value 'slave[s]' for property ansibleRoles.hosts is deprecated and will be removed in next major release. " +
                        "It is replaced 1:1 by 'worker'.");
                this.hosts = "worker";
                return;
            }
            if (hosts.equals("all") || hosts.equals("worker") || hosts.equals("master")) {
                this.hosts = hosts;
                return;
            }
            LOG.error("Unsupported value '{}' for ansibleRoles.hosts.",hosts);
            this.hosts = "unsupported";
        }

        public Map<String, Object> getVars() {
            return vars;
        }

        public void setVars(Map<String, Object> vars) {
            this.vars = vars;
        }

        public String getVarsFile() {
            return varsFile;
        }

        public void setVarsFile(String varsFile) {
            this.varsFile = varsFile;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }
    }

    /**
     * Provides support for Ansible Galaxy and Git roles and playbooks.
     *
     * String name      : (optional) name of (ansible-galaxy) role or playbook, default is given name
     * String hosts     : host (master / worker / all)
     * Map vars         : (optional) additional key - value pairs of role
     * String varsFile  : (optional) file containing key - value pairs of role
     * String galaxy    : (optional) ansible-galaxy name
     * String git       : (optional) Git source (e.g. GitHub url)
     * String url       : (optional) url of role
     * Either galaxy, git or url has to be specified
     */
    public static class AnsibleGalaxyRoles {
        private String name;
        private String hosts;
        private Map<String, Object> vars = new HashMap<>();
        private String varsFile;

        private String galaxy;
        private String git;
        private String url;

        /**
         * Set name to galaxy, git or url if not set.
         * @return role name
         */
        public String getName() {
            if (name == null) {
                if (galaxy != null) {
                    name = galaxy;
                } else if (git != null) {
                    name = git;
                } else if (url != null) {
                    name = url;
                }
            }
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getHosts() {
            return hosts;
        }

        public void setHosts(String hosts) {
            this.hosts = hosts;
        }

        public Map<String, Object> getVars() {
            return vars;
        }

        public void setVars(Map<String, Object> vars) {
            this.vars = vars;
        }

        public String getVarsFile() {
            return varsFile;
        }

        public void setVarsFile(String varsFile) {
            this.varsFile = varsFile;
        }

        public String getGalaxy() {
            return galaxy;
        }

        public void setGalaxy(String galaxy) {
            this.galaxy = galaxy;
        }

        public String getGit() {
            return git;
        }

        public void setGit(String git) {
            this.git = git;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }

    /**
     * Representation of a SSH Cluster KeyPair.
     */
    public static class ClusterKeyPair {

        private String privateKey;
        private String publicKey;
        private String name;

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        /**
         * Return the name of the keypair
         *
         * @return
         */
        public String getName() {
            return name;
        }

        /**
         * Set the name of the keypair.
         *
         * @param name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         *  Store public AND private key in Configuration.KEY_DIR as name[.pub].
         *
         * @throws IOException
         */
        public void store() throws IOException {
            // private key
            try {
                Path p = Paths.get(KEYS_DIR + System.getProperty("file.separator") + name);
                Files.createFile(p, KEYS_PERMS);
                OutputStream fout = Files.newOutputStream(p);
                fout.write(privateKey.getBytes());
                fout.close();
            } catch (IOException e){
                throw new IOException("Error writing private key :"+e.getMessage(),e);
            }
            // public key
            try {
                Path p = Paths.get(KEYS_DIR + System.getProperty("file.separator") + name + ".pub");
                OutputStream fout = Files.newOutputStream(p);
                fout.write(publicKey.getBytes());
                fout.close();
            } catch (IOException e) {
                throw new IOException("Error writing public key :"+e.getMessage(),e);
            }
        }

        /**
         * Load public and private key from Configuration.KEYS_DIR.
         *
         * @throws IOException
         */
        public void load() throws IOException {
            //private key
            try {
                Path p = Paths.get(KEYS_DIR + System.getProperty("file.separator") + name);
                InputStream fin = Files.newInputStream(p);
                privateKey = new String(fin.readAllBytes());
            } catch (IOException e) {
                throw new IOException("Error loading private key :"+e.getMessage(),e);
            }
            // public key
            try {
                Path p = Paths.get(KEYS_DIR + System.getProperty("file.separator") + name + ".pub");
                InputStream fin = Files.newInputStream(p);
                publicKey = new String(fin.readAllBytes());
            } catch (IOException e){
                throw new IOException("Error loading public key :"+e.getMessage(),e);
            }
        }
    }
}

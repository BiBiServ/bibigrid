package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Configuration {
    protected static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    private static final String DEFAULT_WORKSPACE = "~/";

    private String mode;
    private String user = System.getProperty("user.name");
    private String sshUser = "ubuntu";
    private String keypair;
    private String sshPublicKeyFile;
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
    private List<SlaveInstanceConfiguration> slaveInstances = new ArrayList<>();
    private boolean oge;
    private boolean slurm;
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
    private String[] clusterIds;
    // private String cloud9Workspace = DEFAULT_WORKSPACE;  deprecated
    private String workspace = DEFAULT_WORKSPACE;

    public int getSlaveInstanceCount() {
        if (slaveInstances == null) {
            return 0;
        }
        int slaveInstanceCount = 0;
        for (SlaveInstanceConfiguration config : slaveInstances) {
            slaveInstanceCount += config.getCount();
        }
        return slaveInstanceCount;
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

    public String getKeypair() {
        return keypair;
    }

    public void setKeypair(String keypair) {
        this.keypair = keypair.trim();
        LOG.info(V, "Keypair name set. ({})", this.keypair);
    }

    public String getSshPublicKeyFile() {
        return sshPublicKeyFile;
    }

    public void setSshPublicKeyFile(String sshPublicKeyFile) {
        this.sshPublicKeyFile = sshPublicKeyFile.trim();
        LOG.info(V, "SSH public key file found! ({})", this.sshPublicKeyFile);
    }

    public String getSshPrivateKeyFile() {
        return sshPrivateKeyFile;
    }

    public void setSshPrivateKeyFile(String sshPrivateKeyFile) {
        this.sshPrivateKeyFile = sshPrivateKeyFile.trim();
        LOG.info(V, "SSH private key file found! ({})", this.sshPrivateKeyFile);
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
            LOG.info(V, "Master instances set: {}", display);
        }
    }

    public List<SlaveInstanceConfiguration> getSlaveInstances() {
        return slaveInstances;
    }

    public void setSlaveInstances(List<SlaveInstanceConfiguration> slaveInstances) {
        this.slaveInstances = slaveInstances != null ? slaveInstances : new ArrayList<>();
        if (slaveInstances != null && !slaveInstances.isEmpty()) {
            StringBuilder display = new StringBuilder();
            for (SlaveInstanceConfiguration instanceConfiguration : slaveInstances) {
                display.append("[type=").append(instanceConfiguration.getType())
                        .append(", image=").append(instanceConfiguration.getImage())
                        .append(", count=").append(instanceConfiguration.getCount()).append("] ");
            }
            LOG.info(V, "Slave instances set: {}", display);
        }
    }

    /**
     * Helper getter so multiple instance types can be used in a simple for loop.
     */
    public List<SlaveInstanceConfiguration> getExpandedSlaveInstances() {
        List<SlaveInstanceConfiguration> result = new ArrayList<>();
        if (slaveInstances.size() > 0) {
            int typeIndex = 0;
            int typeInstancesLeft = slaveInstances.get(0).getCount();
            for (int i = 0; i < getSlaveInstanceCount(); i++) {
                result.add(slaveInstances.get(typeIndex));
                // If we reach the count of the current type, move to the next instance type
                typeInstancesLeft--;
                if (typeInstancesLeft == 0) {
                    typeIndex += 1;
                    if (typeIndex < slaveInstances.size()) {
                        typeInstancesLeft = slaveInstances.get(typeIndex).getCount();
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

    public String[] getClusterIds() {
        return Arrays.copyOf(clusterIds, clusterIds.length);
    }

    /**
     * Set the cluster Id(s) either as a single cluster "id" or as multiple "id1/id2/id3".
     */
    public void setClusterIds(String clusterIds) {
        this.clusterIds = clusterIds == null ? new String[0] : clusterIds.split("[/,]");
    }

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
            LOG.warn("GridEngine (oge) support is deprecated and is only supported using Ubuntu 16.04. " +
                    "The Support will be removed in near future. Use Slurm instead.");
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
        LOG.warn("Option cloud9Workspace is deprecated. Use workspace instead.");
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
            LOG.warn("Ganglia (oge) support is deprecated and is only supported using Ubuntu 16.04. " +
                     "The Support will be removed in near future. Use Zabbix instead.");
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
    public static class SlaveInstanceConfiguration extends InstanceConfiguration {
        public SlaveInstanceConfiguration() {
        }

        private int count;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
            if (count < 0) {
                LOG.warn("Number of slave nodes has to be at least 0. ({})", count);
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
     * String hosts     : host (master / slave / all)
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
     * String hosts     : host (master / slave / all)
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
}

package de.unibi.cebitec.bibigrid.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Configuration {
    protected static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    private static final String DEFAULT_CLOUD9_WORKSPACE = "~/";

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
    private boolean cassandra;
    private boolean mesos;
    private boolean oge;
    private boolean slurm;
    private String mungeKey;
    private boolean hdfs;
    private boolean spark;
    private boolean nfs = true;
    private boolean cloud9;
    private List<String> nfsShares = new ArrayList<>(Arrays.asList("/vol/spool"));
    private List<MountPoint> masterMounts = new ArrayList<>();
    private List<MountPoint> extNfsShares = new ArrayList<>();
    private FS localFS = FS.XFS;
    private boolean debugRequests;
    private boolean useSpotInstances;
    private String network;
    private String subnet;
    private String[] clusterIds;
    private List<String> masterAnsibleRoles = new ArrayList<>();
    private List<String> slaveAnsibleRoles = new ArrayList<>();
    private String cloud9Workspace = DEFAULT_CLOUD9_WORKSPACE;

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

    public boolean isCassandra() {
        return cassandra;
    }

    public void setCassandra(boolean cassandra) {
        this.cassandra = cassandra;
        LOG.info(V, "Cassandra support {}.", cassandra ? "enabled" : "disabled");
        if (cassandra) {
            nfs = true;
        }
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
        this.clusterIds = clusterIds == null ? new String[0] : clusterIds.split("/");
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

    public boolean isMesos() {
        return mesos;
    }

    public void setMesos(boolean mesos) {
        this.mesos = mesos;
        LOG.info(V, "Mesos support {}.", mesos ? "enabled" : "disabled");
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
        LOG.warn("GridEngine (oge) support is deprecated and will be removed in near future. Use Slurm instead.");
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

    public boolean isHdfs() {
        return hdfs;
    }

    public void setHdfs(boolean hdfs) {
        this.hdfs = hdfs;
        LOG.info(V, "HDFS support {}.", hdfs ? "enabled" : "disabled");
        if (hdfs) {
            nfs = true;
        }
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

    public boolean isSpark() {
        return spark;
    }

    public void setSpark(boolean spark) {
        this.spark = spark;
        LOG.info(V, "Spark support {}.", spark ? "enabled" : "disabled");
    }

    public boolean isUseSpotInstances() {
        return useSpotInstances;
    }

    public void setUseSpotInstances(boolean useSpotInstances) {
        this.useSpotInstances = useSpotInstances;
        if (useSpotInstances) {
            LOG.info(V, "Use spot request for all");
        }
    }

    public boolean isCloud9() {
        return cloud9;
    }

    public void setCloud9(boolean cloud9) {
        this.cloud9 = cloud9;
    }

    public String getCredentialsFile() {
        return credentialsFile;
    }

    public void setCredentialsFile(String credentialsFile) {
        this.credentialsFile = credentialsFile;
    }

    public List<String> getMasterAnsibleRoles() {
        return masterAnsibleRoles;
    }

    public void setMasterAnsibleRoles(List<String> masterAnsibleRoles) {
        this.masterAnsibleRoles = masterAnsibleRoles != null ? masterAnsibleRoles : new ArrayList<>();
        if (masterAnsibleRoles != null && !masterAnsibleRoles.isEmpty()) {
            StringBuilder display = new StringBuilder();
            for (String role : masterAnsibleRoles) {
                display.append(role).append(" ");
            }
            LOG.info(V, "Additional master ansible roles set: {}", display);
        }
    }

    public List<String> getSlaveAnsibleRoles() {
        return slaveAnsibleRoles;
    }

    public void setSlaveAnsibleRoles(List<String> slaveAnsibleRoles) {
        this.slaveAnsibleRoles = slaveAnsibleRoles != null ? slaveAnsibleRoles : new ArrayList<>();
        if (slaveAnsibleRoles != null && !slaveAnsibleRoles.isEmpty()) {
            StringBuilder display = new StringBuilder();
            for (String role : slaveAnsibleRoles) {
                display.append(role).append(" ");
            }
            LOG.info(V, "Additional slave ansible roles set: {}", display);
        }
    }

    public String getCloud9Workspace() {
        return cloud9Workspace;
    }

    public void setCloud9Workspace(String cloud9Workspace) {
        if (cloud9Workspace == null || cloud9Workspace.length() == 0) {
            cloud9Workspace = DEFAULT_CLOUD9_WORKSPACE;
        }
        this.cloud9Workspace = cloud9Workspace;
        LOG.info(V, "Cloud9 workspace set: {}", cloud9Workspace);
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
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

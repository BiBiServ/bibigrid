package de.unibi.cebitec.bibigrid.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Configuration {
    protected static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    /*
    private String shellScriptFile;
    private String earlyMasterShellScriptFile;
    private String earlySlaveShellScriptFile;
    */
    private String mode = "aws";
    private String user = System.getProperty("user.name");
    private String sshUser = "ubuntu";
    private String keypair;
    private String identityFile;
    private String alternativeConfigPath;
    private String gridPropertiesFile;
    private String region;
    private String availabilityZone;
    private List<Port> ports = new ArrayList<>();
    private boolean useMasterAsCompute;
    private InstanceConfiguration masterInstance = new InstanceConfiguration();
    private List<SlaveInstanceConfiguration> slaveInstances = new ArrayList<>();
    private boolean cassandra;
    private boolean mesos;
    private boolean oge = true;
    private boolean hdfs;
    private boolean spark;
    private boolean nfs = true;
    private List<String> nfsShares = new ArrayList<>();
    private List<MountPoint> masterMounts = new ArrayList<>();
    private List<MountPoint> slaveMounts = new ArrayList<>();
    private List<MountPoint> extNfsShares = new ArrayList<>();
    private FS localFS = FS.XFS;
    private boolean debugRequests;
    private boolean useSpotInstances;
    private String vpc;
    private String subnet;
    private String[] clusterIds;

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

    public String getKeypair() {
        return keypair;
    }

    public void setKeypair(String keypair) {
        this.keypair = keypair.trim();
        LOG.info(V, "Keypair name set. ({})", this.keypair);
    }

    public String getIdentityFile() {
        return identityFile;
    }

    public void setIdentityFile(String identityFile) {
        this.identityFile = identityFile.trim();
        LOG.info(V, "Identity file found! ({})", this.identityFile);
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
        this.slaveInstances = slaveInstances;
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

    public String[] getClusterIds() {
        return clusterIds;
    }

    /**
     * Set the cluster Id(s) either as a single cluster "id" or as multiple "id1/id2/id3".
     */
    public void setClusterIds(String clusterIds) {
        this.clusterIds = clusterIds == null ? new String[0] : clusterIds.split("/");
    }

    public List<Port> getPorts() {
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports;
        if (ports != null && !ports.isEmpty()) {
            StringBuilder portsDisplay = new StringBuilder();
            for (Port port : ports) {
                portsDisplay.append(port.toString()).append(" ");
            }
            LOG.info(V, "Additional open ports set: {}", portsDisplay);
        }
    }

    /*
    public Path getShellScriptFile() {
        return shellScriptFile;
    }

    public void setShellScriptFile(Path shellScriptFile) {
        this.shellScriptFile = shellScriptFile;
    }

    public Path getEarlyMasterShellScriptFile() {
        return earlyMasterShellScriptFile;
    }

    public void setEarlyMasterShellScriptFile(Path shellEarlyScriptFile) {
        this.earlyMasterShellScriptFile = shellEarlyScriptFile;
    }

    public Path getEarlySlaveShellScriptFile() {
        return earlySlaveShellScriptFile;
    }

    public void setEarlySlaveShellScriptFile(Path shellEarlyScriptFile) {
        this.earlySlaveShellScriptFile = shellEarlyScriptFile;
    }
    */

    public List<MountPoint> getMasterMounts() {
        return masterMounts;
    }

    public void setMasterMounts(List<MountPoint> masterMounts) {
        this.masterMounts = masterMounts;
    }

    public List<MountPoint> getSlaveMounts() {
        return slaveMounts;
    }

    public void setSlaveMounts(List<MountPoint> slaveMounts) {
        this.slaveMounts = slaveMounts;
    }

    public List<String> getNfsShares() {
        return nfsShares;
    }

    public void setNfsShares(List<String> nfsShares) {
        this.nfsShares = nfsShares;
        if (nfsShares != null && !nfsShares.isEmpty()) {
            StringBuilder nfsSharesDisplay = new StringBuilder();
            for (String share : nfsShares) {
                nfsSharesDisplay.append(share).append(" ");
            }
            LOG.info(V, "NFS shares set: {}", nfsSharesDisplay);
        }
    }

    public List<MountPoint> getExtNfsShares() {
        return extNfsShares;
    }

    public void setExtNfsShares(List<MountPoint> extNfsShares) {
        this.extNfsShares = extNfsShares;
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

    public String getVpc() {
        return vpc;
    }

    public void setVpc(String vpc) {
        this.vpc = vpc.trim();
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
        LOG.info(V, "OpenGridEngine support {}.", oge ? "enabled" : "disabled");
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
}

package de.unibi.cebitec.bibigrid.core.model;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Configuration {
    private InstanceType masterInstanceType;
    private String masterImage;
    private InstanceType slaveInstanceType;
    private int slaveInstanceCount;
    private String slaveImage;
    private String availabilityZone;
    private String keypair;
    private Path identityFile;
    private String region;
    private String[] clusterIds;

    private Path shellScriptFile;
    private Path earlyMasterShellScriptFile, earlySlaveShellScriptFile;
    private Map<String, String> masterMounts;
    private Map<String, String> slaveMounts;
    private List<String> nfsShares;
    private Map<String, String> extNfsShares;
    private boolean logHttpRequests;

    private FS localFS = FS.XFS;

    private boolean useMasterAsCompute;
    private String alternativeConfigPath;
    private String vpcId;

    private boolean cassandra = false;
    private boolean mesos = false;
    private boolean nfs = true;
    private boolean oge = true;
    private boolean hdfs = false;
    private boolean spark = false;

    private String user = System.getProperty("user.name");
    private String sshUser = "ubuntu";

    private boolean useSpotInstances;

    /* network configuration (for OpenStack)*/
    private String subnetName = null;

    /* security group configuration */
    private List<Port> ports = new ArrayList<>();

    public enum FS {
        EXT2, EXT3, EXT4, XFS
    }

    private String mode = "aws";

    //grid-properties-file
    private File gridPropertiesFile = null;

    public void setGridPropertiesFile(File gridPropertiesFile) {
        this.gridPropertiesFile = gridPropertiesFile;
    }

    public File getGridPropertiesFile() {
        return gridPropertiesFile;
    }

    public boolean isCassandra() {
        return cassandra;
    }

    public void setCassandra(boolean cassandra) {
        this.cassandra = cassandra;
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

    public int getSlaveInstanceCount() {
        return slaveInstanceCount;
    }

    public void setSlaveInstanceCount(int slaveInstanceCount) {
        this.slaveInstanceCount = slaveInstanceCount;
    }

    public InstanceType getMasterInstanceType() {
        return masterInstanceType;
    }

    public void setMasterInstanceType(InstanceType masterInstanceType) {
        this.masterInstanceType = masterInstanceType;
    }

    public InstanceType getSlaveInstanceType() {
        return slaveInstanceType;
    }

    public void setSlaveInstanceType(InstanceType slaveInstanceType) {
        this.slaveInstanceType = slaveInstanceType;
    }

    public String getKeypair() {
        return keypair;
    }

    public void setKeypair(String keypair) {
        this.keypair = keypair;
    }

    public Path getIdentityFile() {
        return identityFile;
    }

    public void setIdentityFile(Path identityFile) {
        this.identityFile = identityFile;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getMasterImage() {
        return masterImage;
    }

    public void setMasterImage(String masterImage) {
        this.masterImage = masterImage != null ? masterImage.trim() : null;
    }

    public String getSlaveImage() {
        return slaveImage;
    }

    public void setSlaveImage(String slaveImage) {
        this.slaveImage = slaveImage != null ? slaveImage.trim() : null;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
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
    }

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

    public Map<String, String> getMasterMounts() {
        return masterMounts;
    }

    public void setMasterMounts(Map<String, String> masterMounts) {
        this.masterMounts = masterMounts;
    }

    public Map<String, String> getSlaveMounts() {
        return slaveMounts;
    }

    public void setSlaveMounts(Map<String, String> slaveMounts) {
        this.slaveMounts = slaveMounts;
    }

    public List<String> getNfsShares() {
        return nfsShares;
    }

    public void setNfsShares(List<String> nfsShares) {
        this.nfsShares = nfsShares;
    }

    public Map<String, String> getExtNfsShares() {
        return extNfsShares;
    }

    public void setExtNfsShares(Map<String, String> extNfsShares) {
        this.extNfsShares = extNfsShares;
    }

    public boolean isAlternativeConfigFile() {
        return alternativeConfigPath != null;
    }

    public String getAlternativeConfigPath() {
        return alternativeConfigPath;
    }

    public void setAlternativeConfigPath(String alternativeConfigPath) {
        this.alternativeConfigPath = alternativeConfigPath;
    }

    public String getVpcId() {
        return vpcId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public boolean isMesos() {
        return mesos;
    }

    public void setMesos(boolean mesos) {
        this.mesos = mesos;
    }

    public boolean isNfs() {
        return nfs;
    }

    public void setNfs(boolean nfs) {
        this.nfs = nfs;
    }

    public boolean isOge() {
        return oge;
    }

    public void setOge(boolean oge) {
        this.oge = oge;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getSshUser() {
        return sshUser;
    }

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
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
        if (hdfs) {
            nfs = true;
        }
    }

    public String getSubnetName() {
        return subnetName;
    }

    public void setSubnetName(String subnetName) {
        this.subnetName = subnetName;
    }

    public boolean isLogHttpRequests() {
        return logHttpRequests;
    }

    public void setLogHttpRequests(boolean logHttpRequests) {
        this.logHttpRequests = logHttpRequests;
    }

    public boolean isSpark() {
        return spark;
    }

    public void setSpark(boolean spark) {
        this.spark = spark;
    }

    public boolean isUseSpotInstances() {
        return useSpotInstances;
    }

    public void setUseSpotInstances(boolean useSpotInstances) {
        this.useSpotInstances = useSpotInstances;
    }
}

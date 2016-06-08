package de.unibi.cebitec.bibigrid.model;

import com.amazonaws.auth.AWSCredentials;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Configuration {

    private InstanceType masterInstanceType;  
    private String masterImage;
    private InstanceType slaveInstanceType;
    private int slaveInstanceCount;
    private String slaveImage;
    private String availabilityZone;
    private String keypair;
    private Path identityFile;
    private String region;
    private AWSCredentials credentials;
    private String clusterId;
    private List<Port> ports;
    private Path shellScriptFile;
    private Path earlyShellScriptFile;
    private Map<String, String> masterMounts;
    private Map<String, String> slaveMounts;
    private List<String> nfsShares;
    
    private FS localFS = FS.XFS;
    
    private boolean useMasterAsCompute;
    private boolean cassandra = false;
    private boolean alternativeConfigFile = false;
    private String alternativeConfigPath = "";
    private String vpcid;

    private boolean mesos = false;
    private boolean nfs = true;
    private boolean oge = true;
    private boolean hdfs = false;

    private String user = System.getProperty("user.name");
    
    /* AWS related Configuration options */
    private double bidPrice, bidPriceMaster;
    private boolean useSpotInstances;
    private boolean publicSlaveIps; 
    

    public static enum MODE {

        AWS, OPENSTACK
    }
    
    public static enum FS {
        EXT2, EXT3, EXT4, XFS
    }

    private MODE mode = MODE.AWS;

    private OpenStackCredentials openstackCredentials;

    //grid-properties-file
    private File gridpropertiesfile = null;

    public void setGridPropertiesFile(File s) {
        this.gridpropertiesfile = s;
    }

    public File getGridPropertiesFile() {
        return gridpropertiesfile;
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

    public AWSCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(AWSCredentials credentials) {
        this.credentials = credentials;
    }

    public String getMasterImage() {
        return masterImage;
    }

    public void setMasterImage(String masterImage) {
        if (masterImage != null) {
            masterImage = masterImage.trim();
        }
        this.masterImage = masterImage;
    }

    public String getSlaveImage() {
        return slaveImage;
    }

    public void setSlaveImage(String slaveImage) {
        if (slaveImage != null) {
            slaveImage = slaveImage.trim();
        }
        this.slaveImage = slaveImage;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
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

    public Path getEarlyShellScriptFile() {
        return earlyShellScriptFile;
    }

    public void setEarlyShellScriptFile(Path shellEarlyScriptFile) {
        this.earlyShellScriptFile = shellEarlyScriptFile;
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

    public boolean isAlternativeConfigFile() {
        return alternativeConfigFile;
    }

    public void setAlternativeConfigFile(boolean alternativeConfigFile) {
        this.alternativeConfigFile = alternativeConfigFile;
    }

    public String getAlternativeConfigPath() {
        return alternativeConfigPath;
    }

    public void setAlternativeConfigPath(String alternativeConfigPath) {
        this.alternativeConfigPath = alternativeConfigPath;
    }

    public String getVpcid() {
        return vpcid;
    }

    public void setVpcid(String vpcid) {
        this.vpcid = vpcid;
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

    public MODE getMode() {
        return mode;
    }

    public void setMode(MODE mode) {
        this.mode = mode;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public OpenStackCredentials getOpenstackCredentials() {
        return openstackCredentials;
    }

    public void setOpenstackCredentials(OpenStackCredentials openstackCredentials) {
        this.openstackCredentials = openstackCredentials;
    }

    public double getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(double bidPrice) {
        this.bidPrice = bidPrice;
    }

    public double getBidPriceMaster() {
        if (bidPriceMaster <= 0.0) {
            return bidPrice;
        }
        return bidPriceMaster;
    }

    public void setBidPriceMaster(double bidPriceMaster) {
        this.bidPriceMaster = bidPriceMaster;
    }

    public boolean isUseSpotInstances() {
        return useSpotInstances;
    }

    public void setUseSpotInstances(boolean useSpotInstances) {
        this.useSpotInstances = useSpotInstances;
    }

    public boolean isPublicSlaveIps() {
        return publicSlaveIps;
    }

    public void setPublicSlaveIps(boolean publicSlaveIps) {
        this.publicSlaveIps = publicSlaveIps;
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
    
    
    
    
}

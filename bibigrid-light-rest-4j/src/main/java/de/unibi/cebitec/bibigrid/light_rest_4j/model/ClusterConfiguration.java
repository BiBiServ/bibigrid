package de.unibi.cebitec.bibigrid.light_rest_4j.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterConfiguration {

    private String subnet;
    private String workspace;
    private java.util.List<String> sshPublicKeys;
    private Object ports;
    private Boolean oge;
    private String availabilityZone;
    private String network;
    private String mode;
    
    
    public enum LocalFSEnum {
        
        EXT2 ("EXT2"), 
        
        EXT3 ("EXT3"), 
        
        EXT4 ("EXT4"), 
        
        XFS ("XFS"); 
        

        private final String value;

        LocalFSEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        public static LocalFSEnum fromValue(String text) {
            for (LocalFSEnum b : LocalFSEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                return b;
                }
            }
            return null;
        }
    }

    private LocalFSEnum localFS;

    
    private java.util.List<String> nfsShares;
    private Boolean theia;
    private Boolean useSpotInstances;
    private java.util.List<Object> masterMounts;
    private Boolean useMasterAsCompute;
    private Boolean zabbix;
    private Boolean debugRequests;
    private java.util.List<Object> ansibleGalaxyRoles;
    private Boolean useMasterWithPublicIp;
    private java.util.List<Object> ansibleRoles;
    private Boolean slurm;
    private Object masterInstance;
    private Object zabbixConf;
    private Boolean cloud9;
    private Boolean ganglia;
    private java.util.List<Object> extNfsShares;
    private String sshUser;
    private Boolean nfs;
    private String region;
    private String user;
    private Boolean localDNSLookup;
    private java.util.List<Object> workerInstances;

    public ClusterConfiguration () {
    }

    @JsonProperty("subnet")
    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    @JsonProperty("workspace")
    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    @JsonProperty("sshPublicKeys")
    public java.util.List<String> getSshPublicKeys() {
        return sshPublicKeys;
    }

    public void setSshPublicKeys(java.util.List<String> sshPublicKeys) {
        this.sshPublicKeys = sshPublicKeys;
    }

    @JsonProperty("ports")
    public Object getPorts() {
        return ports;
    }

    public void setPorts(Object ports) {
        this.ports = ports;
    }

    @JsonProperty("oge")
    public Boolean getOge() {
        return oge;
    }

    public void setOge(Boolean oge) {
        this.oge = oge;
    }

    @JsonProperty("availabilityZone")
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    @JsonProperty("network")
    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    @JsonProperty("mode")
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @JsonProperty("localFS")
    public LocalFSEnum getLocalFS() {
        return localFS;
    }

    public void setLocalFS(LocalFSEnum localFS) {
        this.localFS = localFS;
    }

    @JsonProperty("nfsShares")
    public java.util.List<String> getNfsShares() {
        return nfsShares;
    }

    public void setNfsShares(java.util.List<String> nfsShares) {
        this.nfsShares = nfsShares;
    }

    @JsonProperty("theia")
    public Boolean getTheia() {
        return theia;
    }

    public void setTheia(Boolean theia) {
        this.theia = theia;
    }

    @JsonProperty("useSpotInstances")
    public Boolean getUseSpotInstances() {
        return useSpotInstances;
    }

    public void setUseSpotInstances(Boolean useSpotInstances) {
        this.useSpotInstances = useSpotInstances;
    }

    @JsonProperty("masterMounts")
    public java.util.List<Object> getMasterMounts() {
        return masterMounts;
    }

    public void setMasterMounts(java.util.List<Object> masterMounts) {
        this.masterMounts = masterMounts;
    }

    @JsonProperty("useMasterAsCompute")
    public Boolean getUseMasterAsCompute() {
        return useMasterAsCompute;
    }

    public void setUseMasterAsCompute(Boolean useMasterAsCompute) {
        this.useMasterAsCompute = useMasterAsCompute;
    }

    @JsonProperty("zabbix")
    public Boolean getZabbix() {
        return zabbix;
    }

    public void setZabbix(Boolean zabbix) {
        this.zabbix = zabbix;
    }

    @JsonProperty("debugRequests")
    public Boolean getDebugRequests() {
        return debugRequests;
    }

    public void setDebugRequests(Boolean debugRequests) {
        this.debugRequests = debugRequests;
    }

    @JsonProperty("ansibleGalaxyRoles")
    public java.util.List<Object> getAnsibleGalaxyRoles() {
        return ansibleGalaxyRoles;
    }

    public void setAnsibleGalaxyRoles(java.util.List<Object> ansibleGalaxyRoles) {
        this.ansibleGalaxyRoles = ansibleGalaxyRoles;
    }

    @JsonProperty("useMasterWithPublicIp")
    public Boolean getUseMasterWithPublicIp() {
        return useMasterWithPublicIp;
    }

    public void setUseMasterWithPublicIp(Boolean useMasterWithPublicIp) {
        this.useMasterWithPublicIp = useMasterWithPublicIp;
    }

    @JsonProperty("ansibleRoles")
    public java.util.List<Object> getAnsibleRoles() {
        return ansibleRoles;
    }

    public void setAnsibleRoles(java.util.List<Object> ansibleRoles) {
        this.ansibleRoles = ansibleRoles;
    }

    @JsonProperty("slurm")
    public Boolean getSlurm() {
        return slurm;
    }

    public void setSlurm(Boolean slurm) {
        this.slurm = slurm;
    }

    @JsonProperty("masterInstance")
    public Object getMasterInstance() {
        return masterInstance;
    }

    public void setMasterInstance(Object masterInstance) {
        this.masterInstance = masterInstance;
    }

    @JsonProperty("zabbixConf")
    public Object getZabbixConf() {
        return zabbixConf;
    }

    public void setZabbixConf(Object zabbixConf) {
        this.zabbixConf = zabbixConf;
    }

    @JsonProperty("cloud9")
    public Boolean getCloud9() {
        return cloud9;
    }

    public void setCloud9(Boolean cloud9) {
        this.cloud9 = cloud9;
    }

    @JsonProperty("ganglia")
    public Boolean getGanglia() {
        return ganglia;
    }

    public void setGanglia(Boolean ganglia) {
        this.ganglia = ganglia;
    }

    @JsonProperty("extNfsShares")
    public java.util.List<Object> getExtNfsShares() {
        return extNfsShares;
    }

    public void setExtNfsShares(java.util.List<Object> extNfsShares) {
        this.extNfsShares = extNfsShares;
    }

    @JsonProperty("sshUser")
    public String getSshUser() {
        return sshUser;
    }

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
    }

    @JsonProperty("nfs")
    public Boolean getNfs() {
        return nfs;
    }

    public void setNfs(Boolean nfs) {
        this.nfs = nfs;
    }

    @JsonProperty("region")
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @JsonProperty("user")
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @JsonProperty("localDNSLookup")
    public Boolean getLocalDNSLookup() {
        return localDNSLookup;
    }

    public void setLocalDNSLookup(Boolean localDNSLookup) {
        this.localDNSLookup = localDNSLookup;
    }

    @JsonProperty("workerInstances")
    public java.util.List<Object> getWorkerInstances() {
        return workerInstances;
    }

    public void setWorkerInstances(java.util.List<Object> workerInstances) {
        this.workerInstances = workerInstances;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClusterConfiguration ClusterConfiguration = (ClusterConfiguration) o;

        return Objects.equals(subnet, ClusterConfiguration.subnet) &&
               Objects.equals(workspace, ClusterConfiguration.workspace) &&
               Objects.equals(sshPublicKeys, ClusterConfiguration.sshPublicKeys) &&
               Objects.equals(ports, ClusterConfiguration.ports) &&
               Objects.equals(oge, ClusterConfiguration.oge) &&
               Objects.equals(availabilityZone, ClusterConfiguration.availabilityZone) &&
               Objects.equals(network, ClusterConfiguration.network) &&
               Objects.equals(mode, ClusterConfiguration.mode) &&
               Objects.equals(localFS, ClusterConfiguration.localFS) &&
               Objects.equals(nfsShares, ClusterConfiguration.nfsShares) &&
               Objects.equals(theia, ClusterConfiguration.theia) &&
               Objects.equals(useSpotInstances, ClusterConfiguration.useSpotInstances) &&
               Objects.equals(masterMounts, ClusterConfiguration.masterMounts) &&
               Objects.equals(useMasterAsCompute, ClusterConfiguration.useMasterAsCompute) &&
               Objects.equals(zabbix, ClusterConfiguration.zabbix) &&
               Objects.equals(debugRequests, ClusterConfiguration.debugRequests) &&
               Objects.equals(ansibleGalaxyRoles, ClusterConfiguration.ansibleGalaxyRoles) &&
               Objects.equals(useMasterWithPublicIp, ClusterConfiguration.useMasterWithPublicIp) &&
               Objects.equals(ansibleRoles, ClusterConfiguration.ansibleRoles) &&
               Objects.equals(slurm, ClusterConfiguration.slurm) &&
               Objects.equals(masterInstance, ClusterConfiguration.masterInstance) &&
               Objects.equals(zabbixConf, ClusterConfiguration.zabbixConf) &&
               Objects.equals(cloud9, ClusterConfiguration.cloud9) &&
               Objects.equals(ganglia, ClusterConfiguration.ganglia) &&
               Objects.equals(extNfsShares, ClusterConfiguration.extNfsShares) &&
               Objects.equals(sshUser, ClusterConfiguration.sshUser) &&
               Objects.equals(nfs, ClusterConfiguration.nfs) &&
               Objects.equals(region, ClusterConfiguration.region) &&
               Objects.equals(user, ClusterConfiguration.user) &&
               Objects.equals(localDNSLookup, ClusterConfiguration.localDNSLookup) &&
               Objects.equals(workerInstances, ClusterConfiguration.workerInstances);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subnet, workspace, sshPublicKeys, ports, oge, availabilityZone, network, mode, localFS, nfsShares, theia, useSpotInstances, masterMounts, useMasterAsCompute, zabbix, debugRequests, ansibleGalaxyRoles, useMasterWithPublicIp, ansibleRoles, slurm, masterInstance, zabbixConf, cloud9, ganglia, extNfsShares, sshUser, nfs, region, user, localDNSLookup, workerInstances);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ClusterConfiguration {\n");
        sb.append("    subnet: ").append(toIndentedString(subnet)).append("\n");
        sb.append("    workspace: ").append(toIndentedString(workspace)).append("\n");
        sb.append("    sshPublicKeys: ").append(toIndentedString(sshPublicKeys)).append("\n");
        sb.append("    ports: ").append(toIndentedString(ports)).append("\n");
        sb.append("    oge: ").append(toIndentedString(oge)).append("\n");
        sb.append("    availabilityZone: ").append(toIndentedString(availabilityZone)).append("\n");
        sb.append("    network: ").append(toIndentedString(network)).append("\n");
        sb.append("    mode: ").append(toIndentedString(mode)).append("\n");
        sb.append("    localFS: ").append(toIndentedString(localFS)).append("\n");
        sb.append("    nfsShares: ").append(toIndentedString(nfsShares)).append("\n");
        sb.append("    theia: ").append(toIndentedString(theia)).append("\n");
        sb.append("    useSpotInstances: ").append(toIndentedString(useSpotInstances)).append("\n");
        sb.append("    masterMounts: ").append(toIndentedString(masterMounts)).append("\n");
        sb.append("    useMasterAsCompute: ").append(toIndentedString(useMasterAsCompute)).append("\n");
        sb.append("    zabbix: ").append(toIndentedString(zabbix)).append("\n");
        sb.append("    debugRequests: ").append(toIndentedString(debugRequests)).append("\n");
        sb.append("    ansibleGalaxyRoles: ").append(toIndentedString(ansibleGalaxyRoles)).append("\n");
        sb.append("    useMasterWithPublicIp: ").append(toIndentedString(useMasterWithPublicIp)).append("\n");
        sb.append("    ansibleRoles: ").append(toIndentedString(ansibleRoles)).append("\n");
        sb.append("    slurm: ").append(toIndentedString(slurm)).append("\n");
        sb.append("    masterInstance: ").append(toIndentedString(masterInstance)).append("\n");
        sb.append("    zabbixConf: ").append(toIndentedString(zabbixConf)).append("\n");
        sb.append("    cloud9: ").append(toIndentedString(cloud9)).append("\n");
        sb.append("    ganglia: ").append(toIndentedString(ganglia)).append("\n");
        sb.append("    extNfsShares: ").append(toIndentedString(extNfsShares)).append("\n");
        sb.append("    sshUser: ").append(toIndentedString(sshUser)).append("\n");
        sb.append("    nfs: ").append(toIndentedString(nfs)).append("\n");
        sb.append("    region: ").append(toIndentedString(region)).append("\n");
        sb.append("    user: ").append(toIndentedString(user)).append("\n");
        sb.append("    localDNSLookup: ").append(toIndentedString(localDNSLookup)).append("\n");
        sb.append("    workerInstances: ").append(toIndentedString(workerInstances)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

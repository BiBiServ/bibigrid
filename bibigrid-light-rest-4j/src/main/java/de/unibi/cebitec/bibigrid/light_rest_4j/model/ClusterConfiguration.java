package de.unibi.cebitec.bibigrid.light_rest_4j.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterConfiguration {

    private String mode;
    private String subnet;
    private Object masterInstance;
    private Object openstackCredentials;
    private java.util.List<String> sshPublicKeys;
    private String sshUser;
    private String keypair;
    private String region;
    private String availabilityZone;
    private java.util.List<Object> workerInstances;

    public ClusterConfiguration () {
    }

    @JsonProperty("mode")
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    @JsonProperty("subnet")
    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    @JsonProperty("masterInstance")
    public Object getMasterInstance() {
        return masterInstance;
    }

    public void setMasterInstance(Object masterInstance) {
        this.masterInstance = masterInstance;
    }

    @JsonProperty("openstackCredentials")
    public Object getOpenstackCredentials() {
        return openstackCredentials;
    }

    public void setOpenstackCredentials(Object openstackCredentials) {
        this.openstackCredentials = openstackCredentials;
    }

    @JsonProperty("sshPublicKeys")
    public java.util.List<String> getSshPublicKeys() {
        return sshPublicKeys;
    }

    public void setSshPublicKeys(java.util.List<String> sshPublicKeys) {
        this.sshPublicKeys = sshPublicKeys;
    }

    @JsonProperty("sshUser")
    public String getSshUser() {
        return sshUser;
    }

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
    }

    @JsonProperty("keypair")
    public String getKeypair() {
        return keypair;
    }

    public void setKeypair(String keypair) {
        this.keypair = keypair;
    }

    @JsonProperty("region")
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @JsonProperty("availabilityZone")
    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
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

        return Objects.equals(mode, ClusterConfiguration.mode) &&
               Objects.equals(subnet, ClusterConfiguration.subnet) &&
               Objects.equals(masterInstance, ClusterConfiguration.masterInstance) &&
               Objects.equals(openstackCredentials, ClusterConfiguration.openstackCredentials) &&
               Objects.equals(sshPublicKeys, ClusterConfiguration.sshPublicKeys) &&
               Objects.equals(sshUser, ClusterConfiguration.sshUser) &&
               Objects.equals(keypair, ClusterConfiguration.keypair) &&
               Objects.equals(region, ClusterConfiguration.region) &&
               Objects.equals(availabilityZone, ClusterConfiguration.availabilityZone) &&
               Objects.equals(workerInstances, ClusterConfiguration.workerInstances);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, subnet, masterInstance, openstackCredentials, sshPublicKeys, sshUser, keypair, region, availabilityZone, workerInstances);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ClusterConfiguration {\n");
        sb.append("    mode: ").append(toIndentedString(mode)).append("\n");
        sb.append("    subnet: ").append(toIndentedString(subnet)).append("\n");
        sb.append("    masterInstance: ").append(toIndentedString(masterInstance)).append("\n");
        sb.append("    openstackCredentials: ").append(toIndentedString(openstackCredentials)).append("\n");
        sb.append("    sshPublicKeys: ").append(toIndentedString(sshPublicKeys)).append("\n");
        sb.append("    sshUser: ").append(toIndentedString(sshUser)).append("\n");
        sb.append("    keypair: ").append(toIndentedString(keypair)).append("\n");
        sb.append("    region: ").append(toIndentedString(region)).append("\n");
        sb.append("    availabilityZone: ").append(toIndentedString(availabilityZone)).append("\n");
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

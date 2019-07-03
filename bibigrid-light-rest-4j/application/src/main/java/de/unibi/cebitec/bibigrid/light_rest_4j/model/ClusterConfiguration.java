package de.unibi.cebitec.bibigrid.light_rest_4j.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterConfiguration {

    private String mode;
    private String subnet;
    private Object masterInstance;
    private Object access;
    private String nfsShares;
    private String theia;
    private Object masterMounts;
    private Object slaveInstances;

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

    @JsonProperty("access")
    public Object getAccess() {
        return access;
    }

    public void setAccess(Object access) {
        this.access = access;
    }

    @JsonProperty("nfsShares")
    public String getNfsShares() {
        return nfsShares;
    }

    public void setNfsShares(String nfsShares) {
        this.nfsShares = nfsShares;
    }

    @JsonProperty("theia")
    public String getTheia() {
        return theia;
    }

    public void setTheia(String theia) {
        this.theia = theia;
    }

    @JsonProperty("masterMounts")
    public Object getMasterMounts() {
        return masterMounts;
    }

    public void setMasterMounts(Object masterMounts) {
        this.masterMounts = masterMounts;
    }

    @JsonProperty("slaveInstances")
    public Object getSlaveInstances() {
        return slaveInstances;
    }

    public void setSlaveInstances(Object slaveInstances) {
        this.slaveInstances = slaveInstances;
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
               Objects.equals(access, ClusterConfiguration.access) &&
               Objects.equals(nfsShares, ClusterConfiguration.nfsShares) &&
               Objects.equals(theia, ClusterConfiguration.theia) &&
               Objects.equals(masterMounts, ClusterConfiguration.masterMounts) &&
               Objects.equals(slaveInstances, ClusterConfiguration.slaveInstances);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, subnet, masterInstance, access, nfsShares, theia, masterMounts, slaveInstances);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ClusterConfiguration {\n");
        sb.append("    mode: ").append(toIndentedString(mode)).append("\n");
        sb.append("    subnet: ").append(toIndentedString(subnet)).append("\n");
        sb.append("    masterInstance: ").append(toIndentedString(masterInstance)).append("\n");
        sb.append("    access: ").append(toIndentedString(access)).append("\n");
        sb.append("    nfsShares: ").append(toIndentedString(nfsShares)).append("\n");
        sb.append("    theia: ").append(toIndentedString(theia)).append("\n");
        sb.append("    masterMounts: ").append(toIndentedString(masterMounts)).append("\n");
        sb.append("    slaveInstances: ").append(toIndentedString(slaveInstances)).append("\n");
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

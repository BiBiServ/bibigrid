package de.unibi.cebitec.bibigrid.light_rest_4j.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Authorization {

    private String mode;
    private String subnet;
    private Object openstackCredentials;
    private byte sshPublicKeyFile;
    private String sshUser;
    private String keypair;
    private String region;
    private String availabilityZone;

    public Authorization () {
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

    @JsonProperty("openstackCredentials")
    public Object getOpenstackCredentials() {
        return openstackCredentials;
    }

    public void setOpenstackCredentials(Object openstackCredentials) {
        this.openstackCredentials = openstackCredentials;
    }

    @JsonProperty("sshPublicKeyFile")
    public byte getSshPublicKeyFile() {
        return sshPublicKeyFile;
    }

    public void setSshPublicKeyFile(byte sshPublicKeyFile) {
        this.sshPublicKeyFile = sshPublicKeyFile;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Authorization Authorization = (Authorization) o;

        return Objects.equals(mode, Authorization.mode) &&
               Objects.equals(subnet, Authorization.subnet) &&
               Objects.equals(openstackCredentials, Authorization.openstackCredentials) &&
               Objects.equals(sshPublicKeyFile, Authorization.sshPublicKeyFile) &&
               Objects.equals(sshUser, Authorization.sshUser) &&
               Objects.equals(keypair, Authorization.keypair) &&
               Objects.equals(region, Authorization.region) &&
               Objects.equals(availabilityZone, Authorization.availabilityZone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, subnet, openstackCredentials, sshPublicKeyFile, sshUser, keypair, region, availabilityZone);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Authorization {\n");
        sb.append("    mode: ").append(toIndentedString(mode)).append("\n");
        sb.append("    subnet: ").append(toIndentedString(subnet)).append("\n");
        sb.append("    openstackCredentials: ").append(toIndentedString(openstackCredentials)).append("\n");
        sb.append("    sshPublicKeyFile: ").append(toIndentedString(sshPublicKeyFile)).append("\n");
        sb.append("    sshUser: ").append(toIndentedString(sshUser)).append("\n");
        sb.append("    keypair: ").append(toIndentedString(keypair)).append("\n");
        sb.append("    region: ").append(toIndentedString(region)).append("\n");
        sb.append("    availabilityZone: ").append(toIndentedString(availabilityZone)).append("\n");
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

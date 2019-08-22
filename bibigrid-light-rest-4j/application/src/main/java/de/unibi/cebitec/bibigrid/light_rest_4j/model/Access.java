package de.unibi.cebitec.bibigrid.light_rest_4j.model;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Access {

    private byte[] credentialsFile;
    private byte[] sshPublicKeyFile;
    private String sshUser;
    private String keypair;
    private byte[] sshPrivateKeyFile;
    private String region;
    private String availabilityZone;

    public Access () {
    }

    @JsonProperty("credentialsFile")
    public byte[] getCredentialsFile() {
        return credentialsFile;
    }

    public void setCredentialsFile(byte[] credentialsFile) {
        this.credentialsFile = credentialsFile;
    }

    @JsonProperty("sshPublicKeyFile")
    public byte[] getSshPublicKeyFile() {
        return sshPublicKeyFile;
    }

    public void setSshPublicKeyFile(byte[] sshPublicKeyFile) {
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

    @JsonProperty("sshPrivateKeyFile")
    public byte[] getSshPrivateKeyFile() {
        return sshPrivateKeyFile;
    }

    public void setSshPrivateKeyFile(byte[] sshPrivateKeyFile) {
        this.sshPrivateKeyFile = sshPrivateKeyFile;
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

        Access Access = (Access) o;

        return Arrays.equals(credentialsFile, Access.credentialsFile) &&
               Arrays.equals(sshPublicKeyFile, Access.sshPublicKeyFile) &&
               Objects.equals(sshUser, Access.sshUser) &&
               Objects.equals(keypair, Access.keypair) &&
               Arrays.equals(sshPrivateKeyFile, Access.sshPrivateKeyFile) &&
               Objects.equals(region, Access.region) &&
               Objects.equals(availabilityZone, Access.availabilityZone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(credentialsFile), Arrays.hashCode(sshPublicKeyFile), sshUser, keypair, Arrays.hashCode(sshPrivateKeyFile), region, availabilityZone);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Access {\n");
        sb.append("    credentialsFile: ").append(toIndentedString(credentialsFile)).append("\n");
        sb.append("    sshPublicKeyFile: ").append(toIndentedString(sshPublicKeyFile)).append("\n");
        sb.append("    sshUser: ").append(toIndentedString(sshUser)).append("\n");
        sb.append("    keypair: ").append(toIndentedString(keypair)).append("\n");
        sb.append("    sshPrivateKeyFile: ").append(toIndentedString(sshPrivateKeyFile)).append("\n");
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

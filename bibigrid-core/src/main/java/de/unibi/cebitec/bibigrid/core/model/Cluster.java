package de.unibi.cebitec.bibigrid.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cluster, consisting of instances, network (network, subnet, placementGroup) and
 * firewall rules (security group).
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class Cluster {
    private final String clusterId;

    private String masterInstance;
    private List<String> slaveInstances = new ArrayList<>();

    private String network;
    private String subnet;

    private String placementGroup; // AWS

    private String publicIp;
    private String securityGroup;
    private String keyName;
    private String user;

    private String started;

    public Cluster(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getMasterInstance() {
        return masterInstance;
    }

    public void setMasterInstance(String masterInstance) {
        this.masterInstance = masterInstance;
    }

    public String getPlacementGroup() {
        return placementGroup;
    }

    public void setPlacementGroup(String placementGroup) {
        this.placementGroup = placementGroup;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public String getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup;
    }

    public List<String> getSlaveInstances() {
        return slaveInstances;
    }

    public void setSlaveInstances(List<String> slaveInstances) {
        this.slaveInstances = slaveInstances;
    }

    public void addSlaveInstance(String id) {
        slaveInstances.add(id);
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getStarted() {
        return started;
    }

    public void setStarted(String started) {
        this.started = started;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }
}

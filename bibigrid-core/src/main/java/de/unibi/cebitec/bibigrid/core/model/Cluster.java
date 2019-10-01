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

    private Instance masterInstance;
    private List<Instance> workerInstances = new ArrayList<>();

    private Network network;
    private Subnet subnet;

    private String placementGroup; // AWS

    private String publicIp;
    private String privateIp;
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

    public Instance getMasterInstance() {
        return masterInstance;
    }

    public void setMasterInstance(Instance masterInstance) {
        this.masterInstance = masterInstance;
    }

    public String getPlacementGroup() {
        return placementGroup;
    }

    public void setPlacementGroup(String placementGroup) {
        this.placementGroup = placementGroup;
    }

    public Subnet getSubnet() {
        return subnet;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }

    public String getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup;
    }

    public List<Instance> getWorkerInstances() {
        return workerInstances;
    }

    public void setWorkerInstances(List<Instance> workerInstances) {
        this.workerInstances = workerInstances;
    }

    public void addWorkerInstance(Instance instance) {
        workerInstances.add(instance);
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

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPrivateIp() {
        return publicIp;
    }

    public void setPrivateIp(String publicIp) {
        this.publicIp = publicIp;
    }
}

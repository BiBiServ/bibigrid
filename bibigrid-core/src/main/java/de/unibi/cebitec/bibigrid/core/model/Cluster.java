package de.unibi.cebitec.bibigrid.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a cluster, consisting of instances, network (network, subnet, placementGroup) and
 * firewall rules (security group).
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class Cluster implements Comparable<Cluster> {
    private static final Logger LOG = LoggerFactory.getLogger(Cluster.class);
    private final String clusterId;

    private Instance masterInstance;
    private List<Instance> workerInstances = new ArrayList<>();
    private List<Instance> deletedInstances = new ArrayList<>();

    private Network network;
    private Subnet subnet;

    private String placementGroup; // AWS

    private String publicIp;
    private String privateIp;
    private String securityGroup;
    private String keyName;
    private String user;

    private String availabilityZone;

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
        masterInstance.setMaster(true);
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

    /**
     * Returns a orderd list of worker instances
     * @return ordered list of worker instances
     */
    public List<Instance> getWorkerInstances() {
        Collections.sort(workerInstances);
        return workerInstances;
    }

    /**
     * Returns a ordered list of worker instances of given batch.
     * @param batchIndex idx of worker configuration
     * @return ordered list of instances of specified worker configuration idx
     */
    public List<Instance> getWorkerInstances(int batchIndex) {
        List<Instance> workers = new ArrayList<>();
        for (Instance worker : getWorkerInstances()) {
            if (worker.getBatchIndex() == batchIndex) {
                workers.add(worker);
            }
        }
        Collections.sort(workerInstances);
        return workers;
    }

    public void setWorkerInstances(List<Instance> instances) {
        this.workerInstances = instances;
    }

    /**
     * Todo doc
     * @param instances
     */
    public void addWorkerInstances(List<Instance> instances) {
        for (Instance instance : instances) {
            this.addWorkerInstance(instance);
        }
    }

    /**
     * Todo doc
     * @param instance
     */
    public void addWorkerInstance(Instance instance) {
        workerInstances.add(instance);
    }

    /**
     * Todo doc
     * @param instance
     */
    public void removeWorkerInstance(Instance instance) {
        workerInstances.remove(instance);
    }

    public List<Instance> getDeletedInstances() {
        return deletedInstances;
    }

    public void setDeletedInstances(List<Instance> deletedInstances) {
        this.deletedInstances = deletedInstances;
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

        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    /**
     * Clusters should be sorted by user name, following launch (started).
     * @param cluster other cluster to compare with this
     * @return negative, equal or positive when less than, equal to, or greater than the other clusters values
     */
    @Override
    public int compareTo(Cluster cluster) {
        if (user.equals(cluster.getUser())) {
            return started.compareTo(cluster.getStarted());
        } else {
            return user.compareTo(cluster.getUser());
        }
    }
}

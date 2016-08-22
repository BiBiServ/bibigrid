package de.unibi.cebitec.bibigrid.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a cluster, consisting of instances, network (router,net,subnet, placementgroup and vpc) and firewall rules (security group)
 * 
 * 
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class Cluster {

    private String masterinstance;
    private List<String> slaveinstances = new ArrayList<>();
    
    private String router; // OpenStack
    private String net; // OpenStack
    private String subnet; 
    
    private String placementgroup; // AWS
    private String vpc; //AWS
    private String securitygroup;
    private String keyname;
    private String user;
    
    private String started = "unknown";

    public String getMasterinstance() {
        return masterinstance;
    }

    public void setMasterinstance(String masterinstance) {
        this.masterinstance = masterinstance;
    }

    public String getPlacementgroup() {
        return placementgroup;
    }

    public void setPlacementgroup(String placementgroup) {
        this.placementgroup = placementgroup;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public String getVpc() {
        return vpc;
    }

    public void setVpc(String vpc) {
        this.vpc = vpc;
    }

    public String getSecuritygroup() {
        return securitygroup;
    }

    public void setSecuritygroup(String securitygroup) {
        this.securitygroup = securitygroup;
    }

    public List<String> getSlaveinstances() {
        return slaveinstances;
    }

    public void setSlaveinstances(List<String> slaveinstances) {
        this.slaveinstances = slaveinstances;
    }

    public void addSlaveInstance(String id) {
        slaveinstances.add(id);
    }

    public String getKeyname() {
        return keyname;
    }

    public void setKeyname(String keyname) {
        this.keyname = keyname;
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

    public String getRouter() {
        return router;
    }

    public void setRouter(String router) {
        this.router = router;
    }

    public String getNet() {
        return net;
    }

    public void setNet(String net) {
        this.net = net;
    }
    
    
    

}

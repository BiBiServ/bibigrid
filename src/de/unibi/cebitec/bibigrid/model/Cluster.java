/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jkrueger
 */
public class Cluster {

    private String masterinstance;
    private List<String> slaveinstances = new ArrayList<>();
    private String placementgroup;
    private String subnet;
    private String vpc;
    private String securitygroup;
    private String keyname;

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

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.model;

/**
 *
 * @author jsteiner
 */
public class OpenStackCredentials {

    private String tenantName;
    private String username;
    private String password;
    private String endpoint;

    public OpenStackCredentials() {

    }

    public OpenStackCredentials(String tenantName, String username, String password, String endpoint) {
        this.tenantName = tenantName;
        this.username = username;
        this.password = password;
        this.endpoint = endpoint;
    }

    public String getTenantName() {
        return tenantName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

}

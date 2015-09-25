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

    public OpenStackCredentials(String tenantName, String username, String password) {
        this.tenantName = tenantName;
        this.username = username;
        this.password = password;
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

}

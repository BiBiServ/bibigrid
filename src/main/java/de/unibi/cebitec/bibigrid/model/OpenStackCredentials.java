package de.unibi.cebitec.bibigrid.model;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class OpenStackCredentials {
    private String tenantName;
    private String tenantID;
    private String username;
    private String password;
    private String endpoint;
    private String domain;
    private String tenantDomain;

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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getTenantDomain() {
        return tenantDomain == null ? domain : tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }
}

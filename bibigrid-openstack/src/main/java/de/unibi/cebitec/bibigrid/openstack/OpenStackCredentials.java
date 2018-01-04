package de.unibi.cebitec.bibigrid.openstack;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
final class OpenStackCredentials {
    private String tenantName;
    private String username;
    private String password;
    private String endpoint;
    private String domain;
    private String tenantDomain;

    String getUsername() {
        return username;
    }

    void setUsername(String username) {
        this.username = username;
    }

    String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    String getTenantName() {
        return tenantName;
    }

    void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    String getEndpoint() {
        return endpoint;
    }

    void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    String getDomain() {
        return domain;
    }

    void setDomain(String domain) {
        this.domain = domain;
    }

    String getTenantDomain() {
        return tenantDomain == null ? domain : tenantDomain;
    }

    void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }
}

package de.unibi.cebitec.bibigrid.openstack;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class OpenStackCredentials {
    public OpenStackCredentials() {
    }

    private String tenantName;
    private String username;
    private String password;
    private String endpoint;
    private String domain;
    private String tenantDomain;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password != null ? password.trim() : null;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName != null ? tenantName.trim() : null;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint != null ? endpoint.trim() : null;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain != null ? domain.trim() : null;
    }

    public String getTenantDomain() {
        return tenantDomain == null ? domain : tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain != null ? tenantDomain.trim() : null;
    }
}

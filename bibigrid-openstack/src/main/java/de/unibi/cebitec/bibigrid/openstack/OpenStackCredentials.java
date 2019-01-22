package de.unibi.cebitec.bibigrid.openstack;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class OpenStackCredentials {
    public OpenStackCredentials() {
    }

    private String projectName;
    private String username;
    private String password;
    private String endpoint;
    private String domain;
    private String projectDomain;

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
        return projectName;
    }

    public void setTenantName(String tenantName) {
        this.projectName = tenantName != null ? tenantName.trim() : null;
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
        return projectDomain == null ? domain : projectDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.projectDomain = tenantDomain != null ? tenantDomain.trim() : null;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectDomain() {
        return projectDomain;
    }

    public void setProjectDomain(String projectDomain) {
        this.projectDomain = projectDomain;
    }
}

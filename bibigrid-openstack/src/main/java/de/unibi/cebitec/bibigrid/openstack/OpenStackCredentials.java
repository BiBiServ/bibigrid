package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de, jkrueger(at)cebitec.uni-bielefeld.de
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class OpenStackCredentials {

    protected static final Logger LOG = LoggerFactory.getLogger(OpenStackCredentials.class);

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

    @Deprecated
    public String getTenantName() {
        return projectName;
    }

    @Deprecated
    public void setTenantName(String tenantName) {
        LOG.warn("Properties \"tenantName\" is deprecated, use \"projectName\" instead.");
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

    @Deprecated
    public String getTenantDomain() {
        return projectDomain == null ? domain : projectDomain;
    }

    @Deprecated
    public void setTenantDomain(String tenantDomain) {
        LOG.warn("Properties \"tenantDomain\" is deprecated, use \"projectDomain\" instead.");
        this.projectDomain = tenantDomain != null ? tenantDomain.trim() : null;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectDomain() {
        return projectDomain == null ? domain : projectDomain;
    }

    public void setProjectDomain(String projectDomain) {
        this.projectDomain = projectDomain;
    }
}

package de.unibi.cebitec.bibigrid.openstack;

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

    private String project;
    private String projectId;
    private String username;
    private String password;
    private String endpoint;
    private String userDomain;
    private String userDomainId;
    private String projectDomain;
    private String projectDomainId;
    private String region;

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

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint != null ? endpoint.trim() : null;
    }

    // project

    public String getProject() { return project; }

    public void setProject(String project) { this.project = project; }

    // project ID

    public String getProjectId() { return projectId; }

    public void setProjectId(String projectId) { this.projectId = projectId; }

    // user domain

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = (userDomain != null) ? userDomain.trim() : null;
    }

    // user domain id

    public String getUserDomainId() { return userDomainId; }

    public void setUserDomainId(String userDomainId) { this.userDomainId = userDomainId; }

    // project domain

    public String getProjectDomain() { return projectDomain; }

    public void setProjectDomain(String projectDomain) {
        this.projectDomain = projectDomain;
    }

    // project domain id

    public String getProjectDomainId() { return projectDomainId; }

    public void setProjectDomainId(String projectDomainId) {
        this.projectDomainId = projectDomainId;
    }

    // region
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("\tproject         : "+project+"\n");
        sb.append("\tprojectId       : "+projectId+"\n");
        sb.append("\tusername        : "+username+"\n");
        sb.append("\tpassword        : *********\n");
        sb.append("\tendpoint        : "+endpoint+"\n");
        sb.append("\tuserDomain      : "+userDomain+"\n");
        sb.append("\tuserDomainId    : "+userDomainId+"\n");
        sb.append("\tprojectDomain   : "+projectDomain+"\n");
        sb.append("\tprojectDomainId : "+projectDomainId+"\n");
        sb.append("\tregion          : "+region+"\n");
        return sb.toString();
    }
}

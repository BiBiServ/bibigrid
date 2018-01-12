package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ConfigurationGoogleCloud extends Configuration {
    private String googleProjectId;
    private String googleImageProjectId;
    private String googleCredentialsFile;

    String getGoogleProjectId() {
        return googleProjectId;
    }

    void setGoogleProjectId(String googleProjectId) {
        this.googleProjectId = googleProjectId;
    }

    public String getGoogleImageProjectId() {
        return googleImageProjectId;
    }

    public void setGoogleImageProjectId(String googleImageProjectId) {
        this.googleImageProjectId = googleImageProjectId;
    }

    String getGoogleCredentialsFile() {
        return googleCredentialsFile;
    }

    void setGoogleCredentialsFile(String googleCredentialsFile) {
        this.googleCredentialsFile = googleCredentialsFile;
    }
}

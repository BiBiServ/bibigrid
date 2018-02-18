package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ConfigurationGoogleCloud extends Configuration {
    private String googleProjectId;
    private String googleImageProjectId;
    private String googleCredentialsFile;

    public ConfigurationGoogleCloud() {
    }

    public String getGoogleProjectId() {
        return googleProjectId;
    }

    public void setGoogleProjectId(String googleProjectId) {
        this.googleProjectId = googleProjectId.trim();
    }

    public String getGoogleImageProjectId() {
        return googleImageProjectId;
    }

    public void setGoogleImageProjectId(String googleImageProjectId) {
        this.googleImageProjectId = googleImageProjectId.trim();
    }

    public String getGoogleCredentialsFile() {
        return googleCredentialsFile;
    }

    public void setGoogleCredentialsFile(String googleCredentialsFile) {
        this.googleCredentialsFile = googleCredentialsFile.trim();
    }
}

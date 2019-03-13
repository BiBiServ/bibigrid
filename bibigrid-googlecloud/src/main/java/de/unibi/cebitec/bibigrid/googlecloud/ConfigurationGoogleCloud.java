package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de, jkrueger(at)cebitec.uni-bielefeld.de
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ConfigurationGoogleCloud extends Configuration {
    private String googleProjectId;
    private String googleImageProjectId;
    private boolean useSpotInstances;

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

    public boolean isUseSpotInstances() {
        return useSpotInstances;
    }

    public void setUseSpotInstances(boolean useSpotInstances) {
        this.useSpotInstances = useSpotInstances;
    }
}

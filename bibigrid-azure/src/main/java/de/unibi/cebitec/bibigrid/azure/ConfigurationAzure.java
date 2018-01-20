package de.unibi.cebitec.bibigrid.azure;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ConfigurationAzure extends Configuration {
    private String azureCredentialsFile;

    public String getAzureCredentialsFile() {
        return azureCredentialsFile;
    }

    public void setAzureCredentialsFile(String azureCredentialsFile) {
        this.azureCredentialsFile = azureCredentialsFile;
    }
}

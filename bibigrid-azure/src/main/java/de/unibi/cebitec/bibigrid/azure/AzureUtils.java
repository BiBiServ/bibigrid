package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ImageReference;
import de.unibi.cebitec.bibigrid.core.model.Configuration;

import java.io.File;
import java.io.IOException;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
final class AzureUtils {
    static Azure getComputeService(Configuration config) {
        try {
            ConfigurationAzure azureConfig = (ConfigurationAzure) config;
            return Azure.authenticate(new File(azureConfig.getAzureCredentialsFile())).withDefaultSubscription();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param imageId Example: canonical/UbuntuServer/16.04-LTS/latest
     */
    static ImageReference getImage(Azure compute, Configuration config, String imageId) {
        String[] parts = imageId.split("/");
        String provider = parts[0];
        String offer = parts[1];
        String sku = parts[2];
        String version = parts[3];
        return compute.virtualMachineImages().getImage(config.getRegion(), provider, offer, sku, version).imageReference();
    }
}

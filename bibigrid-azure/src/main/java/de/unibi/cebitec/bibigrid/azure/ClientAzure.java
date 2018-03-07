package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ImageReference;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ClientAzure extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientAzure.class);

    private Azure internalClient;

    ClientAzure(ConfigurationAzure config) throws ClientConnectionFailedException {
        try {
            internalClient = Azure.authenticate(new File(config.getAzureCredentialsFile())).withDefaultSubscription();
            LOG.info("Azure connection established.");
        } catch (IOException e) {
            throw new ClientConnectionFailedException("Failed to connect azure client.", e);
        }
    }

    Azure getInternal() {
        return internalClient;
    }

    /**
     * @param imageId Example: canonical/UbuntuServer/16.04-LTS/latest
     */
    ImageReference getImage(Configuration config, String imageId) {
        String[] parts = imageId.split("/");
        String provider = parts[0];
        String offer = parts[1];
        String sku = parts[2];
        String version = parts[3];
        return internalClient.virtualMachineImages().getImage(config.getRegion(), provider, offer, sku, version).imageReference();
    }
}

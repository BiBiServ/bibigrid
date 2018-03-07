package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ImageReference;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ClientAzure extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientAzure.class);

    private final ConfigurationAzure config;
    private Azure internalClient;

    ClientAzure(ConfigurationAzure config) throws ClientConnectionFailedException {
        this.config = config;
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

    @Override
    public Network getNetworkByName(String networkName) {
        for (com.microsoft.azure.management.network.Network network : internalClient.networks().list()) {
            if (network.name().equals(networkName)) {
                return new NetworkAzure(network);
            }
        }
        return null;
    }

    @Override
    public Network getNetworkById(String networkId) {
        com.microsoft.azure.management.network.Network network = internalClient.networks().getById(networkId);
        return network != null ? new NetworkAzure(network) : null;
    }

    @Override
    public Subnet getSubnetByName(String subnetName) {
        for (com.microsoft.azure.management.network.Network network : internalClient.networks().list()) {
            // Only check the networks that are in the specified region.
            if (network.regionName().equalsIgnoreCase(config.getRegion())) {
                for (Map.Entry<String, com.microsoft.azure.management.network.Subnet> entry : network.subnets().entrySet()) {
                    if (entry.getKey().equals(subnetName)) {
                        return new SubnetAzure(entry.getValue());
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Subnet getSubnetById(String subnetId) {
        for (com.microsoft.azure.management.network.Network network : internalClient.networks().list()) {
            // Only check the networks that are in the specified region.
            if (network.regionName().equalsIgnoreCase(config.getRegion())) {
                for (Map.Entry<String, com.microsoft.azure.management.network.Subnet> entry : network.subnets().entrySet()) {
                    if (entry.getValue().inner().id().equals(subnetId)) {
                        return new SubnetAzure(entry.getValue());
                    }
                }
            }
        }
        return null;
    }

    @Override
    public InstanceImage getImageByName(String imageName) {
        return getImageById(imageName);
    }

    /**
     * @param imageId Example: canonical/UbuntuServer/16.04-LTS/latest
     */
    @Override
    public InstanceImage getImageById(String imageId) {
        String[] parts = imageId.split("/");
        String provider = parts[0];
        String offer = parts[1];
        String sku = parts[2];
        String version = parts[3];
        ImageReference image = internalClient.virtualMachineImages().getImage(config.getRegion(), provider, offer,
                sku, version).imageReference();
        return image != null ? new InstanceImageAzure(image) : null;
    }
}

package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ImageReference;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.NotImplementedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.NotYetSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of abstract client for Microsoft Azure
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 *         jkrueger(at)cebitec.uni-bielefeld.de
 */
class ClientAzure extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientAzure.class);

    private Configuration config;
    private Azure internalClient;

    ClientAzure(Configuration config) throws ClientConnectionFailedException {
        this.config = config;
        authenticate();
    }

    Azure getInternal() {
        return internalClient;
    }

    @Override
    public void authenticate() throws ClientConnectionFailedException {
        try {
            internalClient = Azure.authenticate(new File(config.getCredentialsFile())).withDefaultSubscription();
            LOG.info("Azure connection established.");
        } catch (IOException e) {
            throw new ClientConnectionFailedException("Failed to connect azure client.", e);
        }
    }

    @Override
    public List<Network> getNetworks() {
        return internalClient.networks().list().stream().map(NetworkAzure::new).collect(Collectors.toList());
    }

    @Override
    public Network getNetworkByName(String networkName) {
        return getNetworkByIdOrName(networkName);
    }



    @Override
    public Network getNetworkById(String networkId) {
        return getNetworkByIdOrName(networkId);
    }

    @Override
    public Network getNetworkByIdOrName(String net) {
        for (com.microsoft.azure.management.network.Network network : internalClient.networks().list()) {
            if (network.name().equals(net) || network.id().equals(net)) {
                return new NetworkAzure(network);
            }
        }
        return null;
    }

    @Override
    public Network getDefaultNetwork() {
        return null;
    }

    @Override
    public List<Subnet> getSubnets() {
        return internalClient.networks().list().stream()
                .flatMap(network -> network.subnets().values().stream())
                .map(SubnetAzure::new).collect(Collectors.toList());
    }

    @Override
    public List<String> getKeypairNames() {
        throw new NotImplementedException();
    }

    @Override
    public Subnet getSubnetByName(String subnetName) {
        return getSubnetByIdOrName(subnetName);
    }

    @Override
    public Subnet getSubnetById(String subnetId) {
        return getSubnetByIdOrName(subnetId);
    }

    @Override
    public Subnet getSubnetByIdOrName(String snet)  {
        for (com.microsoft.azure.management.network.Network network : internalClient.networks().list()) {
            // Only check the networks that are in the specified region.
            if (network.regionName().equalsIgnoreCase(config.getRegion())) {
                for (Map.Entry<String, com.microsoft.azure.management.network.Subnet> entry : network.subnets().entrySet()) {

                    if (entry.getValue().inner().id().equals(snet) || entry.getValue().inner().name().equals(snet)) {
                        return new SubnetAzure(entry.getValue());
                    }
                }
            }
        }
        return null;
    }

    @Override
    public InstanceImage getImageByName(String imageName) {
        return getImageByIdOrName(imageName);
    }


    @Override
    public InstanceImage getImageById(String imageId) { return getImageByIdOrName(imageId); }


    /**
     * @param img Example: canonical/UbuntuServer/16.04-LTS/latest
     */
    @Override
    public InstanceImage getImageByIdOrName(String img) {
        String[] parts = img.split("/");
        String provider = parts[0];
        String offer = parts[1];
        String sku = parts[2];
        String version = parts[3];
        ImageReference image = internalClient.virtualMachineImages().getImage(config.getRegion(), provider, offer,
                sku, version).imageReference();
        return image != null ? new InstanceImageAzure(image) : null;
    }


    @Override
    public Snapshot getSnapshotByName(String snapshotName) { return getSnapshotByIdOrName(snapshotName); }

    @Override
    public Snapshot getSnapshotById(String snapshotId) { return getSnapshotByIdOrName(snapshotId); }

    @Override
    public Snapshot getSnapshotByIdOrName(String snap)  {
        for (com.microsoft.azure.management.compute.Snapshot snapshot : internalClient.snapshots().list()) {
            if (snapshot != null && (snapshot.name().equals(snap) || snapshot.id().equals(snap))) {
                return new SnapshotAzure(snapshot);
            }
        }
        return null;
    }

    @Override
    public ServerGroup getServerGroupByIdOrName(String serverGroup) throws NotYetSupportedException {
        throw new NotYetSupportedException("Server groups are currently not supported by BiBigrid Azure.");
    }
}

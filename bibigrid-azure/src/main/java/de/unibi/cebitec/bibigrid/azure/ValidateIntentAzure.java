package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ImageReference;
import com.microsoft.azure.management.compute.Snapshot;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.*;

import java.util.Map;

/**
 * Implementation of the general ValidateIntent interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentAzure extends ValidateIntent {
    private final ConfigurationAzure config;
    private final Azure compute;

    ValidateIntentAzure(final Client client, final ConfigurationAzure config) {
        super(client, config);
        this.config = config;
        compute = ((ClientAzure) client).getInternal();
    }

    @Override
    protected boolean connect() {
        return true;
    }

    @Override
    protected InstanceImage getImage(Configuration.InstanceConfiguration instanceConfiguration) {
        ImageReference image = ((ClientAzure) client).getImage(config, instanceConfiguration.getImage());
        return image != null ? new InstanceImageAzure(image) : null;
    }

    @Override
    protected boolean checkSnapshot(String snapshotId) {
        if (snapshotId.contains(":")) {
            snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
        }
        for (Snapshot snapshot : compute.snapshots().list()) {
            if (snapshot != null && snapshot.name().equals(snapshotId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Network getNetwork(String networkName) {
        for (com.microsoft.azure.management.network.Network network : compute.networks().list()) {
            if (network.name().equals(networkName)) {
                return new NetworkAzure(network);
            }
        }
        return null;
    }

    @Override
    protected Subnet getSubnet(String subnetName) {
        for (com.microsoft.azure.management.network.Network network : compute.networks().list()) {
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
}
package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.Snapshot;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.*;

import java.io.IOException;

/**
 * Implementation of the general ValidateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentGoogleCloud extends ValidateIntent {
    private final ConfigurationGoogleCloud config;
    private Compute compute;

    ValidateIntentGoogleCloud(Client client, final ConfigurationGoogleCloud config) {
        super(client, config);
        this.config = config;
        compute = ((ClientGoogleCloud) client).getInternal();
    }

    @Override
    protected boolean connect() {
        return true;
    }

    @Override
    protected InstanceImage getImage(Configuration.InstanceConfiguration instanceConfiguration) {
        Image image = GoogleCloudUtils.getImage(compute, config.getGoogleImageProjectId(), instanceConfiguration.getImage());
        return image != null ? new InstanceImageGoogleCloud(image) : null;
    }

    @Override
    protected boolean checkSnapshot(String snapshotId) {
        if (snapshotId.contains(":")) {
            snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
        }
        Snapshot snapshot = GoogleCloudUtils.getSnapshot(compute, config.getGoogleProjectId(), snapshotId);
        return snapshot != null && snapshot.getName().equals(snapshotId);
    }

    @Override
    protected Network getNetwork(String networkName) {
        try {
            return new NetworkGoogleCloud(compute.networks().get(config.getGoogleProjectId(), networkName).execute());
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    protected Subnet getSubnet(String subnetName) {
        try {
            return new SubnetGoogleCloud(compute.subnetworks().get(config.getGoogleProjectId(), config.getRegion(),
                    subnetName).execute());
        } catch (IOException ignored) {
        }
        return null;
    }
}
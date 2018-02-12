package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.Snapshot;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.InstanceImage;

/**
 * Implementation of the general ValidateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentGoogleCloud extends ValidateIntent {
    private final ConfigurationGoogleCloud config;
    private Compute compute;

    ValidateIntentGoogleCloud(final ConfigurationGoogleCloud config) {
        super(config);
        this.config = config;
    }

    @Override
    protected boolean connect() {
        compute = GoogleCloudUtils.getComputeService(config);
        return compute != null;
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
}
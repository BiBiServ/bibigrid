package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.model.Snapshot;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.*;

/**
 * Implementation of the general ValidateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentGoogleCloud extends ValidateIntent {
    private final ConfigurationGoogleCloud config;

    ValidateIntentGoogleCloud(Client client, final ConfigurationGoogleCloud config) {
        super(client, config);
        this.config = config;
    }

    @Override
    protected boolean checkSnapshot(String snapshotId) {
        if (snapshotId.contains(":")) {
            snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
        }
        Snapshot snapshot = GoogleCloudUtils.getSnapshot(((ClientGoogleCloud) client).getInternal(),
                config.getGoogleProjectId(), snapshotId);
        return snapshot != null && snapshot.getName().equals(snapshotId);
    }
}
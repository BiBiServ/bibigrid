package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.compute.Snapshot;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.*;

/**
 * Implementation of the general ValidateIntent interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentAzure extends ValidateIntent {
    ValidateIntentAzure(final Client client, final ConfigurationAzure config) {
        super(client, config);
    }

    @Override
    protected boolean checkSnapshot(String snapshotId) {
        if (snapshotId.contains(":")) {
            snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
        }
        for (Snapshot snapshot : ((ClientAzure) client).getInternal().snapshots().list()) {
            if (snapshot != null && snapshot.name().equals(snapshotId)) {
                return true;
            }
        }
        return false;
    }
}
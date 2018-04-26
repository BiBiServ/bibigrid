package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.core.model.Snapshot;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class SnapshotGoogleCloud extends Snapshot {
    private final com.google.api.services.compute.model.Snapshot internalSnapshot;

    SnapshotGoogleCloud(com.google.api.services.compute.model.Snapshot internalSnapshot) {
        this.internalSnapshot = internalSnapshot;
    }

    @Override
    public String getId() {
        return internalSnapshot.getId().toString();
    }

    @Override
    public String getName() {
        return internalSnapshot.getName();
    }

    com.google.api.services.compute.model.Snapshot getInternal() {
        return internalSnapshot;
    }
}

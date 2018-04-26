package de.unibi.cebitec.bibigrid.azure;

import de.unibi.cebitec.bibigrid.core.model.Snapshot;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class SnapshotAzure extends Snapshot {
    private final com.microsoft.azure.management.compute.Snapshot internalSnapshot;

    SnapshotAzure(com.microsoft.azure.management.compute.Snapshot internalSnapshot) {
        this.internalSnapshot = internalSnapshot;
    }

    @Override
    public String getId() {
        return internalSnapshot.id();
    }

    @Override
    public String getName() {
        return internalSnapshot.name();
    }

    com.microsoft.azure.management.compute.Snapshot getInternal() {
        return internalSnapshot;
    }
}

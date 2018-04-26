package de.unibi.cebitec.bibigrid.aws;

import de.unibi.cebitec.bibigrid.core.model.Snapshot;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class SnapshotAWS extends Snapshot {
    private final com.amazonaws.services.ec2.model.Snapshot internalSnapshot;

    SnapshotAWS(com.amazonaws.services.ec2.model.Snapshot internalSnapshot) {
        this.internalSnapshot = internalSnapshot;
    }

    @Override
    public String getId() {
        return internalSnapshot.getSnapshotId();
    }

    @Override
    public String getName() {
        return internalSnapshot.getSnapshotId();
    }

    com.amazonaws.services.ec2.model.Snapshot getInternal() {
        return internalSnapshot;
    }
}

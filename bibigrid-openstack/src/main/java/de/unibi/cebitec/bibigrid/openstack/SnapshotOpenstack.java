package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Snapshot;
import org.openstack4j.model.storage.block.Volume;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class SnapshotOpenstack extends Snapshot {
    // TODO: for all others we use VolumeSnapshot
    private final Volume internalSnapshot;

    SnapshotOpenstack(Volume internalSnapshot) {
        this.internalSnapshot = internalSnapshot;
    }

    @Override
    public String getId() {
        return internalSnapshot.getId();
    }

    @Override
    public String getName() {
        return internalSnapshot.getName();
    }

    Volume getInternal() {
        return internalSnapshot;
    }
}

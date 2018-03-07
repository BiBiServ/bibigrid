package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.*;
import org.openstack4j.model.storage.block.Volume;

/**
 * Implementation of the general ValidateIntent interface for an Openstack based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentOpenstack extends ValidateIntent {
    ValidateIntentOpenstack(Client client, final ConfigurationOpenstack config) {
        super(client, config);
    }

    @Override
    protected boolean checkSnapshot(String snapshotId) {
        if (snapshotId.contains(":")) {
            snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
        }
        Volume snapshot = ((ClientOpenstack) client).getInternal().blockStorage().volumes().get(snapshotId);
        return snapshot != null && snapshot.getId().equals(snapshotId);
    }
}

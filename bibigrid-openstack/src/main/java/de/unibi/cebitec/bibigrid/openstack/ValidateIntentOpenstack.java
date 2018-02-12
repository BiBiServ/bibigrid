package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.InstanceImage;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.storage.block.Volume;

/**
 * Implementation of the general ValidateIntent interface for an Openstack based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentOpenstack extends ValidateIntent {
    private final ConfigurationOpenstack config;
    private OSClient os;

    ValidateIntentOpenstack(final ConfigurationOpenstack config) {
        super(config);
        this.config = config;
    }

    @Override
    protected boolean connect() {
        os = OpenStackUtils.buildOSClient(config);
        return os != null;
    }

    @Override
    protected InstanceImage getImage(Configuration.InstanceConfiguration instanceConfiguration) {
        Image image = os.compute().images().get(instanceConfiguration.getImage());
        return image != null ? new InstanceImageOpenstack(image) : null;
    }

    @Override
    protected boolean checkSnapshot(String snapshotId) {
        if (snapshotId.contains(":")) {
            snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
        }
        Volume snapshot = os.blockStorage().volumes().get(snapshotId);
        return snapshot != null && snapshot.getId().equals(snapshotId);
    }
}

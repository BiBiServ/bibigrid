package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.InstanceImage;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the general ValidateIntent interface for an Openstack based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentOpenstack extends ValidateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ValidateIntentOpenstack.class);
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
    protected boolean checkSnapshot(String snapshotId) throws Exception {
        // TODO
        return false;
    }
}

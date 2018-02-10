package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Snapshot;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * Implementation of the general ValidateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentGoogleCloud extends ValidateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ValidateIntentGoogleCloud.class);
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
    protected boolean checkImage(Configuration.InstanceConfiguration instanceConfiguration) {
        return GoogleCloudUtils.getImage(compute, config.getGoogleImageProjectId(), instanceConfiguration.getImage()) != null;
    }

    @Override
    protected boolean checkSnapshot(String snapshotId) {
        if (snapshotId.contains(":")) {
            snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
        }
        Snapshot snapshot = GoogleCloudUtils.getSnapshot(compute, config.getGoogleImageProjectId(), snapshotId);
        if (snapshot == null || !snapshot.getName().equals(snapshotId)) {
            LOG.error("Snapshot {} could not be found.", snapshotId);
            return false;
        }
        LOG.info(V, "Snapshot {} found.", snapshotId);
        return true;
    }
}
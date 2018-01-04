package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.cloud.compute.Compute;
import com.google.cloud.compute.Image;
import com.google.cloud.compute.Snapshot;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;
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
    protected boolean checkImages() {
        boolean allCheck = true;
        boolean foundMaster = false;
        boolean foundSlave = false;
        String projectId = GoogleCloudUtils.getProjectIdForImage(config.getMasterImage(), config.getGoogleProjectId());
        for (Image img : compute.listImages(projectId).iterateAll()) {
            String name = img.getImageId().getImage();
            foundMaster = foundMaster || name.equals(config.getMasterImage());
            foundSlave = foundSlave || name.equals(config.getSlaveImage());
        }
        if (foundSlave && foundMaster) {
            LOG.info(I, "Master and Slave images have been found.");
        } else {
            LOG.error("Master and Slave images could not be found.");
            allCheck = false;
        }
        return allCheck;
    }

    @Override
    protected boolean checkSnapshot(String snapshotId) throws Exception {
        if (snapshotId.contains(":")) {
            snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
        }
        Snapshot snapshot = compute.getSnapshot(snapshotId);
        if (snapshot == null || !snapshot.exists() || !snapshot.getSnapshotId().getSnapshot().equals(snapshotId)) {
            LOG.error("Snapshot {} could not be found.", snapshotId);
            return false;
        }
        LOG.info(V, "Snapshot {} found.", snapshotId);
        return true;
    }
}
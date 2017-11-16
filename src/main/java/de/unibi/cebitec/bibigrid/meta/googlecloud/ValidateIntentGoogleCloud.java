package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.cloud.compute.Compute;
import com.google.cloud.compute.Image;
import com.google.cloud.compute.Snapshot;
import de.unibi.cebitec.bibigrid.meta.ValidateIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;

/**
 * Implementation of the general ValidateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentGoogleCloud implements ValidateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ValidateIntentGoogleCloud.class);
    private final Configuration conf;

    ValidateIntentGoogleCloud(final Configuration conf) {
        this.conf = conf;
    }

    public boolean validate() {
        LOG.info("Validating config file...");
        Compute compute = GoogleCloudUtils.getComputeService(conf);
        if (compute == null) {
            LOG.error("API connection not successful. Please check your configuration.");
            return false;
        }
        if (checkImages(compute)) {
            LOG.info(I, "Image check has been successful.");
        } else {
            LOG.error("Failed to check images.");
            return false;
        }
        if (checkSnapshots(compute)) {
            LOG.info(I, "Snapshot check has been successful.");
        } else {
            LOG.error("One or more snapshots could not be found.");
            return false;
        }
        LOG.info(I, "You can now start your cluster.");
        return true;
    }

    private boolean checkImages(final Compute compute) {
        LOG.info("Checking images...");
        boolean allCheck = true;
        boolean foundMaster = false;
        boolean foundSlave = false;
        for (Image img : compute.listImages().iterateAll()) {
            String name = img.getImageId().getImage();
            foundMaster = foundMaster || name.equals(conf.getMasterImage());
            foundSlave = foundSlave || name.equals(conf.getSlaveImage());
        }
        if (foundSlave && foundMaster) {
            LOG.info(I, "Master and Slave images have been found.");
        } else {
            LOG.error("Master and Slave images could not be found.");
            allCheck = false;
        }
        return allCheck;
    }

    private boolean checkSnapshots(final Compute compute) {
        LOG.info("Checking snapshots...");
        boolean allcheck = true;
        List<String> snapShotList = new ArrayList<>(conf.getMasterMounts().keySet());
        snapShotList.addAll(conf.getSlaveMounts().keySet());
        for (String e : snapShotList) { //snapshot ids have to be checked individually to find out which one is missing or malformed.
            try {
                if (e.contains(":")) {
                    e = e.substring(0, e.indexOf(":"));
                }
                Snapshot snapshot = compute.getSnapshot(e);
                if (snapshot == null || !snapshot.exists()) {
                    LOG.error("Snapshot {} could not be found.", e);
                    allcheck = false;
                    continue;
                }
                if (snapshot.getSnapshotId().getSnapshot().equals(e)) {
                    LOG.info(V, "{} found.", e);
                }
            } catch (Exception f) {
                LOG.error("Snapshot {} could not be found.", e);
                allcheck = false;
            }
        }
        return allcheck;
    }
}
package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.cloud.compute.Compute;
import com.google.cloud.compute.Image;
import com.google.cloud.compute.Snapshot;
import de.unibi.cebitec.bibigrid.meta.ValidateIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;

import java.util.ArrayList;
import java.util.List;

import static de.unibi.cebitec.bibigrid.ctrl.ValidationIntent.log;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;

/**
 * Implementation of the general ValidateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentGoogleCloud implements ValidateIntent {
    private final Configuration conf;

    public ValidateIntentGoogleCloud(final Configuration conf) {
        this.conf = conf;
    }

    public boolean validate() {
        log.info("Validating config file...");
        Compute compute = GoogleCloudUtils.getComputeService(conf);
        if (compute == null) {
            log.error("API connection not successful. Please check your configuration.");
            return false;
        }
        if (checkImages(compute)) {
            log.info(I, "Image check has been successful.");
        } else {
            log.error("Failed to check images.");
            return false;
        }
        if (checkSnapshots(compute)) {
            log.info(I, "Snapshot check has been successful.");
        } else {
            log.error("One or more snapshots could not be found.");
            return false;
        }
        log.info(I, "You can now start your cluster.");
        return true;
    }

    private boolean checkImages(final Compute compute) {
        log.info("Checking images...");
        boolean allCheck = true;
        boolean foundMaster = false;
        boolean foundSlave = false;
        for (Image img : compute.listImages().iterateAll()) {
            String name = img.getImageId().getImage();
            foundMaster = foundMaster || name.equals(conf.getMasterImage());
            foundSlave = foundSlave || name.equals(conf.getSlaveImage());
        }
        if (foundSlave && foundMaster) {
            log.info(I, "Master and Slave images have been found.");
        } else {
            log.error("Master and Slave images could not be found.");
            allCheck = false;
        }
        return allCheck;
    }

    private boolean checkSnapshots(final Compute compute) {
        log.info("Checking snapshots...");
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
                    log.error("Snapshot {} could not be found.", e);
                    allcheck = false;
                    continue;
                }
                if (snapshot.getSnapshotId().getSnapshot().equals(e)) {
                    log.info(V, "{} found.", e);
                }
            } catch (Exception f) {
                log.error("Snapshot {} could not be found.", e);
                allcheck = false;
            }
        }
        return allcheck;
    }
}
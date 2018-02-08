package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.InstanceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;

/**
 * Validation of the configuration file with the selected cloud provider.
 * <p/>
 * The validation steps are as follows:
 * <ol>
 * <li>Check connection can be established</li>
 * <li>Check images are available</li>
 * <li>Check snapshots are available</li>
 * </ol>
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class ValidateIntent implements Intent {
    private static final Logger LOG = LoggerFactory.getLogger(ValidateIntent.class);
    private final Configuration config;

    protected ValidateIntent(Configuration config) {
        this.config = config;
    }

    public boolean validate() {
        LOG.info("Validating config file...");
        boolean success = true;
        if (!connect()) {
            LOG.error("API connection not successful. Please check your configuration.");
            // If not even the connection can be established, the next steps won't be necessary
            return false;
        }
        LOG.info("Checking images...");
        if (checkImages()) {
            LOG.info(I, "Image check has been successful.");
        } else {
            LOG.error("Failed to check images.");
            success = false;
        }
        LOG.info("Checking snapshots...");
        if (checkSnapshots()) {
            LOG.info(I, "Snapshot check has been successful.");
        } else {
            LOG.error("One or more snapshots could not be found.");
            success = false;
        }
        if (success) {
            LOG.info(I, "You can now start your cluster.");
        } else {
            LOG.error("There were one or more errors. Please adjust your configuration.");
        }
        return success;
    }

    protected abstract boolean connect();

    private boolean checkImages() {
        boolean foundMaster = checkImage(config.getMasterInstance());
        if (!foundMaster) {
            LOG.error("Failed to find master image ({}).", config.getMasterInstance().getImage());
        }
        boolean foundAllSlaves = true;
        for (Configuration.InstanceConfiguration instanceConfiguration : config.getSlaveInstances()) {
            if (!checkImage(instanceConfiguration)) {
                foundAllSlaves = false;
                LOG.error("Failed to find slave image ({}).", instanceConfiguration.getImage());
            }
        }
        if (!foundAllSlaves || !foundMaster) {
            LOG.error("Master and Slave images could not be found.");
            return false;
        }
        // Checking if both are hvm or paravirtual types
        boolean allSlavesClusterInstances = config.getSlaveInstances().stream().allMatch(
                x -> x.getProviderType().isClusterInstance());
        final InstanceType masterClusterType = config.getMasterInstance().getProviderType();
        if (masterClusterType.isClusterInstance() != allSlavesClusterInstances) {
            LOG.error("If cluster instances are used please create a homogeneous group.");
            return false;
        } else if (masterClusterType.isClusterInstance()) {
            // If master instance is a cluster instance check if the types are the same
            if (config.getSlaveInstances().stream().anyMatch(x -> masterClusterType != x.getProviderType())) {
                LOG.error("If cluster instances are used please create a homogeneous group.");
                return false;
            }
        }
        LOG.info(I, "Master and Slave images have been found.");
        return true;
    }

    protected abstract boolean checkImage(Configuration.InstanceConfiguration instanceConfiguration);

    private boolean checkSnapshots() {
        boolean allCheck = true;
        List<String> snapshotIds = new ArrayList<>();
        if (config.getMasterMounts() != null) {
            for (Configuration.MountPoint mount : config.getMasterMounts()) {
                snapshotIds.add(mount.getSource());
            }
        }
        if (config.getSlaveMounts() != null) {
            for (Configuration.MountPoint mount : config.getSlaveMounts()) {
                snapshotIds.add(mount.getSource());
            }
        }
        // snapshot ids have to be checked individually to find out which one is missing or malformed.
        for (String snapshotId : snapshotIds) {
            try {
                boolean snapshotFound = checkSnapshot(snapshotId);
                allCheck = allCheck && snapshotFound;
            } catch (Exception ex) {
                LOG.error("Snapshot {} could not be found.", snapshotId);
                allCheck = false;
            }
        }
        return allCheck;
    }

    protected abstract boolean checkSnapshot(String snapshotId) throws Exception;
}

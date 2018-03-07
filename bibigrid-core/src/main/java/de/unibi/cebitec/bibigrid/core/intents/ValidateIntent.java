package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * Validation of the configuration file with the selected cloud provider.
 * <p/>
 * The validation steps are as follows:
 * <ol>
 * <li>Check connection can be established</li>
 * <li>Check images are available</li>
 * <li>Check snapshots are available</li>
 * <li>Check network and subnet are available</li>
 * </ol>
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class ValidateIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(ValidateIntent.class);
    protected final Client client;
    private final Configuration config;

    protected ValidateIntent(Client client, Configuration config) {
        this.client = client;
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
        LOG.info("Checking instance types...");
        if (checkInstanceTypes()) {
            LOG.info(I, "Instance type check has been successful.");
        } else {
            LOG.error("Failed to check instance types.");
            success = false;
        }
        LOG.info("Checking snapshots...");
        if (checkSnapshots()) {
            LOG.info(I, "Snapshot check has been successful.");
        } else {
            LOG.error("One or more snapshots could not be found.");
            success = false;
        }
        LOG.info("Checking network...");
        if (checkNetwork()) {
            LOG.info(I, "Network check has been successful.");
        } else {
            LOG.error("Failed to check network.");
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
        Map<Configuration.InstanceConfiguration, InstanceImage> typeImageMap = new HashMap<>();
        InstanceImage masterImage = getImage(config.getMasterInstance());
        if (masterImage == null) {
            LOG.error("Failed to find master image ({}).", config.getMasterInstance().getImage());
        } else {
            typeImageMap.put(config.getMasterInstance(), masterImage);
        }
        for (Configuration.InstanceConfiguration instanceConfiguration : config.getSlaveInstances()) {
            InstanceImage slaveImage = getImage(instanceConfiguration);
            if (slaveImage == null) {
                LOG.error("Failed to find slave image ({}).", instanceConfiguration.getImage());
            } else {
                typeImageMap.put(instanceConfiguration, slaveImage);
            }
        }
        if (typeImageMap.size() != config.getSlaveInstances().size() + 1) {
            LOG.error("Master and Slave images could not be found.");
            return false;
        }
        LOG.info(I, "Master and Slave images have been found.");
        boolean success = true;
        for (InstanceImage image : typeImageMap.values()) {
            if (!checkProviderImageProperties(image)) {
                success = false;
            }
        }
        for (Map.Entry<Configuration.InstanceConfiguration, InstanceImage> entry : typeImageMap.entrySet()) {
            if (!checkInstanceTypeImageCombination(entry.getKey(), entry.getValue())) {
                success = false;
            }
        }
        return success;
    }

    protected abstract InstanceImage getImage(Configuration.InstanceConfiguration instanceConfiguration);

    protected boolean checkProviderImageProperties(InstanceImage image) {
        return true;
    }

    private boolean checkInstanceTypeImageCombination(Configuration.InstanceConfiguration instanceConfiguration,
                                                      InstanceImage image) {
        boolean success = true;
        if (instanceConfiguration.getProviderType().getMaxDiskSpace() < image.getMinDiskSpace()) {
            LOG.error("The image {} needs more disk space than the instance type {} provides.",
                    instanceConfiguration.getImage(), instanceConfiguration.getProviderType().getValue());
            success = false;
        }
        if (instanceConfiguration.getProviderType().getMaxRam() < image.getMinRam()) {
            LOG.error("The image {} needs more memory than the instance type {} provides.",
                    instanceConfiguration.getImage(), instanceConfiguration.getProviderType().getValue());
            success = false;
        }
        return success;
    }

    private boolean checkInstanceTypes() {
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
        return true;
    }

    private boolean checkSnapshots() {
        boolean allCheck = true;
        List<String> snapshotIds = new ArrayList<>();
        if (config.getMasterMounts() != null) {
            for (Configuration.MountPoint mount : config.getMasterMounts()) {
                snapshotIds.add(mount.getSource());
            }
        }
        // snapshot ids have to be checked individually to find out which one is missing or malformed.
        for (String snapshotId : snapshotIds) {
            if (checkSnapshot(snapshotId)) {
                LOG.info(V, "Snapshot '{}' found.", snapshotId);
            } else {
                LOG.error("Snapshot '{}' could not be found.", snapshotId);
                allCheck = false;
            }
        }
        return allCheck;
    }

    protected abstract boolean checkSnapshot(String snapshotId);

    private boolean checkNetwork() {
        boolean result = true;
        if (config.getNetwork() != null && config.getNetwork().length() > 0) {
            Network network = client.getNetworkByName(config.getNetwork());
            // If the network could not be found, try if the user provided a network id instead of the name.
            if (network == null) {
                network = client.getNetworkById(config.getNetwork());
            }
            if (network != null) {
                LOG.info(V, "Network '{}' found.", config.getNetwork());
            } else {
                LOG.error("Network '{}' could not be found.", config.getNetwork());
                result = false;
            }
        }
        if (config.getSubnet() != null && config.getSubnet().length() > 0) {
            Subnet subnet = client.getSubnetByName(config.getSubnet());
            // If the subnet could not be found, try if the user provided a subnet id instead of the name.
            if (subnet == null) {
                subnet = client.getSubnetById(config.getSubnet());
            }
            if (subnet != null) {
                LOG.info(V, "Subnet '{}' found.", config.getSubnet());
            } else {
                LOG.error("Subnet '{}' could not be found.", config.getSubnet());
                result = false;
            }
        }
        return result;
    }
}

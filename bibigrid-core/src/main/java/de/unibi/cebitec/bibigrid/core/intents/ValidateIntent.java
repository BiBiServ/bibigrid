package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.NotYetSupportedException;
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
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ValidateIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(ValidateIntent.class);
    protected final Client client;
    protected final Configuration config;

    public ValidateIntent(final Client client, final Configuration config) {
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
            LOG.info(V, "Image check has been successful.");
        } else {
            LOG.error("Failed to check images.");
            success = false;
        }
        LOG.info("Checking instance types...");
        if (checkInstanceTypes()) {
            LOG.info(V, "Instance type check has been successful.");
        } else {
            LOG.error("Failed to check instance types.");
            success = false;
        }
        LOG.info("Checking snapshots/volumes...");
        if (checkSnapshots()) {
            LOG.info(V, "Snapshot/Volume check has been successful.");
        } else {
            LOG.error("One or more snapshots/volumes could not be found.");
            success = false;
        }
        LOG.info("Checking network...");
        if (checkNetwork()) {
            LOG.info(V, "Network check has been successful.");
        } else {
            LOG.error("Failed to check network.");
            success = false;
        }
        LOG.info("Checking servergroup...");
        if (checkServerGroup()) {
            LOG.info(V,"Server group check has been successful.");
        } else {
            LOG.error("Failed to check server group.");
            success = false;
        }

        return success;
    }

    /**
     * Run additional connection checks on the client. The client throws an exception if connecting failed before
     * the intent is started.
     */
    protected boolean connect() {
        return true;
    }

    private boolean checkImages() {
        Map<Configuration.InstanceConfiguration, InstanceImage> typeImageMap = new HashMap<>();
        try {
            InstanceImage masterImage = client.getImageByIdOrName(config.getMasterInstance().getImage());
            if (masterImage == null) {
                LOG.error("Failed to find master image ({}).", config.getMasterInstance().getImage());
            } else {
                typeImageMap.put(config.getMasterInstance(), masterImage);
            }
        } catch (NotYetSupportedException e) {
            LOG.error(e.getMessage());
        }
        try {
            for (Configuration.InstanceConfiguration instanceConfiguration : config.getWorkerInstances()) {
                InstanceImage workerImage = client.getImageByIdOrName(instanceConfiguration.getImage());
                if (workerImage == null) {
                    LOG.error("Failed to find worker image ({}).", instanceConfiguration.getImage());
                } else {
                    typeImageMap.put(instanceConfiguration, workerImage);
                }
            }
        } catch (NotYetSupportedException e) {
            LOG.error(e.getMessage());
        }
        if (typeImageMap.size() != config.getWorkerInstances().size() + 1) {
            LOG.error("Master and Worker images could not be found.");
            return false;
        }
        LOG.info(V, "Master and Worker images have been found.");
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
        boolean allWorkersClusterInstances = config.getWorkerInstances().stream().allMatch(
                x -> x.getProviderType().isClusterInstance());
        final InstanceType masterClusterType = config.getMasterInstance().getProviderType();
        if (masterClusterType.isClusterInstance() != allWorkersClusterInstances) {
            LOG.error("If cluster instances are used please create a homogeneous group.");
            return false;
        } else if (masterClusterType.isClusterInstance()) {
            // If master instance is a cluster instance check if the types are the same
            if (config.getWorkerInstances().stream().anyMatch(x -> masterClusterType != x.getProviderType())) {
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
            if (snapshotId.contains(":")) {
                snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
            }
            try {
                Snapshot snapshot = client.getSnapshotByIdOrName(snapshotId);
                if (snapshot == null) {
                    LOG.error("Snapshot/Volume '{}' could not be found.", snapshotId);
                    allCheck = false;
                } else {
                    LOG.info(V, "Snapshot/Volume '{}' found.", snapshotId);
                }
            } catch (NotYetSupportedException e) {
                LOG.error(e.getMessage());
                allCheck = false;
            }
        }
        return allCheck;
    }

    private boolean checkNetwork() {
        boolean result = true;
        if (config.getNetwork() != null && config.getNetwork().length() > 0) {
            try {
                Network network = client.getNetworkByIdOrName(config.getNetwork());
                // If the network could not be found, try if the user provided a network id instead of the name.
                if (network == null) {
                    LOG.error("Network '{}' could not be found.", config.getNetwork());
                    result = false;
                } else {
                    LOG.info(V, "Network '{}' found.", config.getNetwork());
                    config.setNetwork(network.getId());
                }
            } catch (NotYetSupportedException e) {
                LOG.error(e.getMessage());
                result = false;
            }
        }
        if (config.getSubnet() != null && config.getSubnet().length() > 0) {
            try {
                Subnet subnet = client.getSubnetByIdOrName(config.getSubnet());
                if (subnet == null) {
                    LOG.error("Subnet '{}' could not be found.", config.getSubnet());
                    result = false;
                } else {
                    LOG.info(V, "Subnet '{}' found.", config.getSubnet());
                    config.setSubnet(subnet.getId());  // use id
                }
            } catch (NotYetSupportedException e){
                LOG.error(e.getMessage());
                result = false;
            }
        }
        return result;
    }

    private boolean checkServerGroup() {
        boolean result = true;
        if (config.getServerGroup() != null && !config.getServerGroup().isEmpty()) {
            try {
                ServerGroup serverGroup = client.getServerGroupByIdOrName(config.getServerGroup());
                if (serverGroup == null) {
                    LOG.error("ServerGroup '{}' could not be found.", config.getServerGroup());
                    result = false;
                } else {
                    LOG.info(V, "ServerGroup '{}' found.", config.getServerGroup());
                    config.setServerGroup(serverGroup.getId()); // use id
                }
            } catch (NotYetSupportedException e){
                LOG.warn(e.getMessage());
            }
        }
        return result;
    }
}

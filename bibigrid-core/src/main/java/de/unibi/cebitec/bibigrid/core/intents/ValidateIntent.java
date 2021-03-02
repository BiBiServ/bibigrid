package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.NotYetSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public abstract class ValidateIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(ValidateIntent.class);
    protected final Client client;
    protected final Configuration config;

    public ValidateIntent(final Client client, final Configuration config) {
        this.client = client;
        this.config = config;
    }

    private StringBuilder validateResponse = new StringBuilder();

    /**
     *  The validateResponse attribute is used to save the possible causes of a misconfigured configuration
     *  and make it accessible to other classes, e.g. the validate- or create-Controller for bibigrid REST API
     *  (found in bibigrid-light-rest-4j module where the cause of a misconfigured configuration needs to be
     *  sent back to the user via json-body and not only printed to console.
     * @return String containing possible error messages.
     */
    public String getValidateResponse() {
        return validateResponse.toString();
    }

    /**
     * Validates a given configuration and fills validatesResponse attribute with possible error messages.
     * @return True in case of successful validation, false otherwise.
     */
    public boolean validate() {
        // reset validateResponse attribute
        validateResponse = new StringBuilder();
        LOG.info("Validating config file...");
        boolean success = true;
        if (!connect()) {
            // If not even the connection can be established, the next steps won't be necessary
            String msg = "API connection not successful. Please check your configuration.";
            LOG.error(msg);
            validateResponse.append(msg).append("\n");
            return false;
        }
        LOG.info("Checking images...");
        if (checkImages()) {
            LOG.info(V, "Image check has been successful.");
        } else {
            String msg = "Failed to check images.";
            LOG.error(msg);
            validateResponse.append(msg).append("\n");
            success = false;
        }
        LOG.info("Checking instance types...");
        if (checkInstanceTypes()) {
            LOG.info(V, "Instance type check has been successful.");
        } else {
            String msg = "Failed to check instance types.";
            LOG.error(msg);
            validateResponse.append(msg).append("\n");
            success = false;
        }
        LOG.info("Checking snapshots/volumes...");
        if (checkSnapshots()) {
            LOG.info(V, "Snapshot/Volume check has been successful.");
        } else {
            String msg = "One or more snapshots/volumes could not be found.";
            LOG.error(msg);
            validateResponse.append(msg).append("\n");
            success = false;
        }
        LOG.info("Checking network...");
        if (checkNetwork()) {
            LOG.info(V, "Network check has been successful.");
        } else {
            String msg = "Failed to check network.";
            LOG.error(msg);
            validateResponse.append(msg).append("\n");
            success = false;
        }
        LOG.info("Checking servergroup...");
        if (checkServerGroup()) {
            LOG.info(V,"Server group check has been successful.");
        } else {
            String msg = "Failed to check server group.";
            LOG.error(msg);
            validateResponse.append(msg).append("\n");
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

    /**
     * Checks, if all images can be accessed by client and fulfill resource requirements.
     * @return true, if images are accessible for all instances and fulfill resource requirements for each
     */
    private boolean checkImages() {
        Map<Configuration.InstanceConfiguration, InstanceImage> typeImageMap = new HashMap<>();
        try {
            InstanceImage masterImage = client.getImageByIdOrName(config.getMasterInstance().getImage());
            if (masterImage == null) {
                String msg = "Failed to find master image: " + config.getMasterInstance().getImage();
                LOG.error(msg);
                validateResponse.append(msg).append("\n");
            } else {
                typeImageMap.put(config.getMasterInstance(), masterImage);
            }
        } catch (NotYetSupportedException e) {
            LOG.error(e.getMessage());
            validateResponse.append(e.getMessage()).append("\n");
        }
        try {
            for (Configuration.InstanceConfiguration instanceConfiguration : config.getWorkerInstances()) {
                InstanceImage workerImage = client.getImageByIdOrName(instanceConfiguration.getImage());
                if (workerImage == null) {
                    String msg = "Failed to find worker image: "+instanceConfiguration.getImage();
                    LOG.error(msg);
                    validateResponse.append(msg).append("\n");
                } else {
                    typeImageMap.put(instanceConfiguration, workerImage);
                }
            }
        } catch (NotYetSupportedException e) {
            LOG.error(e.getMessage());
            validateResponse.append(e.getMessage()).append("\n");
        }
        // Image has to be appended to map for worker instances + master instance
        if (typeImageMap.size() != config.getWorkerInstances().size() + 1) {
            String msg = "Master and Worker images could not be found.";
            LOG.error(msg);
            validateResponse.append(msg).append("\n");
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

    /**
     * Checks, if sufficient resources for the image are provided by instanceType.
     * @param instanceConfiguration configuration of current checked instance
     * @param image used image
     * @return true, if enough resources available
     */
    private boolean checkInstanceTypeImageCombination(Configuration.InstanceConfiguration instanceConfiguration,
                                                      InstanceImage image) {
        boolean success = true;
        long instanceMaxDiskSpace = instanceConfiguration.getProviderType().getMaxDiskSpace();
        long instanceMaxRam = instanceConfiguration.getProviderType().getMaxRam();
        if (instanceMaxDiskSpace < image.getMinDiskSpace() || instanceMaxRam < image.getMinRam()) {
            String msg  = "The image " + instanceConfiguration.getImage()
                    + "needs more disk space than the instance type "
                    + instanceConfiguration.getProviderType().getValue() + "provides.";
            LOG.error(msg);
            validateResponse.append(msg).append("\n");
            success = false;
        }
        return success;
    }

    /**
     * TODO ? clusterInstance seems to be always false, set in e.g. InstanceTypeOpenstack and cannot be reset
     *
     * Checks, if master instanceType is cluster instance and if so, if worker instances are the same.
     * Need to be a homogenous group
     * @return true, if master is cluster instance and all workers are OR
     * if master is cluster instance and no worker has the same provider type
     */
    private boolean checkInstanceTypes() {
        boolean allWorkersClusterInstances = config.getWorkerInstances().stream().allMatch(
                x -> x.getProviderType().isClusterInstance());
        final InstanceType masterClusterType = config.getMasterInstance().getProviderType();
        if (masterClusterType.isClusterInstance() != allWorkersClusterInstances) {
            String msg = "If cluster instances are used please create a homogeneous group.";
            LOG.error(msg);
            validateResponse.append(msg).append("\n");
            return false;
        } else if (masterClusterType.isClusterInstance()) {
            // If master instance is a cluster instance check if the types are the same
            if (config.getWorkerInstances().stream().anyMatch(x -> masterClusterType != x.getProviderType())) {
                String msg = "If cluster instances are used please create a homogeneous group.";
                LOG.error(msg);
                validateResponse.append(msg).append("\n");
                return false;
            }
        }
        return true;
    }

    /**
     * Checks volumes / snapshots / -ids given by master mounts in config.
     * @return true, if all provided snapshots can be accessed and no problems occurred
     */
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
                    String msg = "Snapshot/Volume '" + snapshotId + "' could not be found.";
                    LOG.error(msg);
                    validateResponse.append(msg).append("\n");
                    allCheck = false;
                } else {
                    LOG.info(V, "Snapshot/Volume '{}' found.", snapshotId);
                }
            } catch (NotYetSupportedException e) {
                LOG.error(e.getMessage());
                validateResponse.append(e.getMessage()).append("\n");
                allCheck = false;
            }
        }
        return allCheck;
    }

    /**
     * Checks, if accessible network or subnet provided in config.
     * @return true, if any provided network / subnet in config is accessible by client
     */
    private boolean checkNetwork() {
        boolean result = true;
        if (config.getNetwork() != null && config.getNetwork().length() > 0) {
            try {
                Network network = client.getNetworkByIdOrName(config.getNetwork());
                if (network == null) {
                    String msg = "Network '" + config.getNetwork() + "' could not be found.";
                    LOG.error(msg);
                    validateResponse.append(msg).append("\n");
                    result = false;
                } else {
                    LOG.info(V, "Network '{}' found.", config.getNetwork());
                    config.setNetwork(network.getId());
                }
            } catch (NotYetSupportedException e) {
                LOG.error(e.getMessage());
                validateResponse.append(e.getMessage());
                result = false;
            }
        }
        if (config.getSubnet() != null && config.getSubnet().length() > 0) {
            try {
                Subnet subnet = client.getSubnetByIdOrName(config.getSubnet());
                if (subnet == null) {
                    String msg = "Subnet '" + config.getSubnet() + "' could not be found.";
                    LOG.error(msg);
                    validateResponse.append(msg).append("\n");
                    result = false;
                } else {
                    LOG.info(V, "Subnet '{}' found.", config.getSubnet());
                    config.setSubnet(subnet.getId());  // use id
                }
            } catch (NotYetSupportedException e){
                LOG.error(e.getMessage());
                validateResponse.append(e.getMessage());
                result = false;
            }
        }
        return result;
    }

    /**
     * Checks, if accessible server group provided in config.
     * @return true, if any server group in config is accessible by client
     */
    private boolean checkServerGroup() {
        boolean result = true;
        if (config.getServerGroup() != null && !config.getServerGroup().isEmpty()) {
            try {
                ServerGroup serverGroup = client.getServerGroupByIdOrName(config.getServerGroup());
                if (serverGroup == null) {
                    String msg = "ServerGroup '" + config.getServerGroup() + "' could not be found.";
                    LOG.error(msg);
                    validateResponse.append(msg).append("\n");
                    result = false;
                } else {
                    LOG.info(V, "ServerGroup '{}' found.", config.getServerGroup());
                    config.setServerGroup(serverGroup.getId()); // use id
                }
            } catch (NotYetSupportedException e){
                validateResponse.append(e.getMessage());
            }
        }
        return result;
    }

    /**
     * Checks, if quotas exceeded before creating instances.
     * @param instanceTypes instanceType and no. of required instances to calculate quota limits
     * @return true, if quotas exceeded -> No creating instances allowed / possible
     */
    public abstract boolean checkQuotasExceeded(Map<InstanceType, Integer> instanceTypes);
}

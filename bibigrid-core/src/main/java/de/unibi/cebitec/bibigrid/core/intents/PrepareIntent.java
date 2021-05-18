package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public abstract class PrepareIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(PrepareIntent.class);
    private static final String IMAGE_PREFIX = "bibigrid-image-";
    protected static final String IMAGE_SOURCE_LABEL = "bibigrid-image-source";

    protected final Configuration config;

    protected PrepareIntent(ProviderModule providerModule, Client client, Configuration config) {
        this.config = config;
    }

    /**
     * Prepare cluster images for the cluster with id in configuration.
     * TODO No usage?
     *
     * @return Return true in case of success, false otherwise
     */
    public boolean prepare(final CreateCluster cluster) {
        Instance masterInstance = cluster.cluster.getMasterInstance();
        List<Instance> workerInstances = cluster.cluster.getWorkerInstances();
        String clusterId = cluster.cluster.getClusterId();
        LOG.info("Stopping {} instances...", workerInstances.size());
        stopInstance(masterInstance);
        for (Instance instance : workerInstances) {
            if (!stopInstance(instance)) {
                return false;
            }
        }
        LOG.info("Waiting for instances to shutdown...");
        waitForInstanceShutdown(masterInstance);
        for (Instance instance : workerInstances) {
            waitForInstanceShutdown(instance);
        }
        LOG.info("Creating images...");
        LOG.info("Creating master instance image...");
        boolean success = true;
        createImageFromInstance(masterInstance, IMAGE_PREFIX + "master-" + clusterId);
        List<String> alreadyCreatedWorkerImages = new ArrayList<>();
        for (Instance instance : workerInstances) {
            String imageType = instance.getConfiguration().getImage();
            if (!alreadyCreatedWorkerImages.contains(imageType)) {
                alreadyCreatedWorkerImages.add(imageType);
                String workerImageName = IMAGE_PREFIX + "worker-" + clusterId + "-" + alreadyCreatedWorkerImages.size();
                if (!createImageFromInstance(instance, workerImageName)) {
                    success = false;
                }
            }
        }
        return success;
    }

    protected abstract boolean stopInstance(Instance instance);

    protected abstract void waitForInstanceShutdown(Instance instance);

    protected abstract boolean createImageFromInstance(Instance instance, String imageName);
}

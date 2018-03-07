package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.Operation;
import de.unibi.cebitec.bibigrid.core.intents.PrepareIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class PrepareIntentGoogleCloud extends PrepareIntent {
    private static final Logger LOG = LoggerFactory.getLogger(PrepareIntentGoogleCloud.class);

    private final ConfigurationGoogleCloud config;
    private final Compute compute;

    PrepareIntentGoogleCloud(ProviderModule providerModule, Client client, ConfigurationGoogleCloud config) {
        super(providerModule, client, config);
        this.config = config;
        compute = ((ClientGoogleCloud) client).getInternal();
    }

    @Override
    protected boolean stopInstance(Instance instance) {
        try {
            compute.instances().stop(config.getGoogleProjectId(), config.getAvailabilityZone(), instance.getName()).execute();
        } catch (IOException e) {
            LOG.error("Failed to stop instance '{}'. {}", instance.getName(), e);
            return false;
        }
        return true;
    }

    @Override
    protected void waitForInstanceShutdown(Instance instance) {
        com.google.api.services.compute.model.Instance nativeInstance = ((InstanceGoogleCloud) instance).getInternal();
        do {
            nativeInstance = GoogleCloudUtils.reload(compute, config, nativeInstance);
            String status = nativeInstance.getStatus();
            LOG.info(V, "Status of instance '{}': {}", instance.getName(), status);
            if (status.equals("TERMINATED")) {
                break;
            } else {
                LOG.info(V, "...");
                sleep(10);
            }
        } while (true);
    }

    @Override
    protected boolean createImageFromInstance(Instance instance, String imageName) {
        Image image = new Image();
        image.setName(imageName);
        Map<String, String> imageLabels = new HashMap<>();
        imageLabels.put(IMAGE_SOURCE_LABEL, instance.getConfiguration().getImage());
        image.setLabels(imageLabels);
        AttachedDisk bootDisk = null;
        for (AttachedDisk disk : ((InstanceGoogleCloud) instance).getInternal().getDisks()) {
            if (disk.getBoot()) {
                bootDisk = disk;
                break;
            }
        }
        if (bootDisk == null) {
            LOG.error("Failed to get boot disk for instance '{}'.", instance.getName());
            return false;
        }
        image.setSourceDisk(bootDisk.getSource());
        try {
            Operation operation = compute.images().insert(config.getGoogleProjectId(), image).execute();
            GoogleCloudUtils.waitForOperation(compute, config, operation);
        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to create image '{}' from instance '{}'. {}", imageName, instance.getName(), e);
            return false;
        }
        return true;
    }
}

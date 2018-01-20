package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.azure.CreateClusterEnvironmentAzure.RESOURCE_GROUP_PREFIX;

/**
 * Implementation of the general TerminateIntent interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class TerminateIntentAzure implements TerminateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntentAzure.class);

    private final ConfigurationAzure config;

    TerminateIntentAzure(final ConfigurationAzure config) {
        this.config = config;
    }

    public boolean terminate() {
        final Azure compute = AzureUtils.getComputeService(config);
        LOG.info("Terminating cluster with ID: {}", config.getClusterId());
        try {
            // Terminating the resource group deletes all associated resources, too.
            if (compute != null) {
                compute.resourceGroups().deleteByName(RESOURCE_GROUP_PREFIX + config.getClusterId());
            }
        } catch (Exception e) {
            LOG.error("Failed to delete resource group {}", e);
        }
        LOG.info("Cluster '{}' terminated!", config.getClusterId());
        return true;
    }
}
package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.azure.CreateClusterEnvironmentAzure.RESOURCE_GROUP_PREFIX;

/**
 * Implementation of the general TerminateIntent interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class TerminateIntentAzure extends TerminateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntentAzure.class);

    TerminateIntentAzure(ProviderModule providerModule, Client client, Configuration config) {
        super(providerModule, client, config);
    }

    @Override
    protected boolean terminateCluster(Cluster cluster) {
        final Azure compute = ((ClientAzure) client).getInternal();
        try {
            // Terminating the resource group deletes all associated resources, too.
            if (compute != null) {
                compute.resourceGroups().deleteByName(RESOURCE_GROUP_PREFIX + cluster.getClusterId());
            }
        } catch (Exception e) {
            LOG.error("Failed to delete resource group. {}", e);
            return false;
        }
        return true;
    }
}
package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class TerminateIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntent.class);
    private final ProviderModule providerModule;
    protected final Client client;
    private final Configuration config;

    protected TerminateIntent(ProviderModule providerModule, Client client, Configuration config) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
    }

    /**
     * Terminate all clusters with the specified ids.
     *
     * @return Return true in case of success, false otherwise
     */
    public boolean terminate() {
        final Map<String, Cluster> clusters = providerModule.getListIntent(client, config).getList();
        boolean success = true;
        for (String clusterId : config.getClusterIds()) {
            final Cluster cluster = clusters.get(clusterId);
            if (cluster == null) {
                LOG.warn("No cluster with ID '{}' found.", clusterId);
                success = false;
                continue;
            }
            LOG.info("Terminating cluster with ID '{}'.", clusterId);
            if (terminateCluster(cluster)) {
                LOG.info("Cluster '{}' terminated!", clusterId);
            } else {
                LOG.info("Failed to terminate cluster '{}'!", clusterId);
                success = false;
            }
        }
        return success;
    }

    protected abstract boolean terminateCluster(Cluster cluster);
}

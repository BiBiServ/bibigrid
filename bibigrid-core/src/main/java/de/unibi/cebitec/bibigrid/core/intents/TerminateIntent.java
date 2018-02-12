package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class TerminateIntent implements Intent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntent.class);
    private final Configuration config;
    private final ProviderModule providerModule;

    protected TerminateIntent(ProviderModule providerModule, Configuration config) {
        this.config = config;
        this.providerModule = providerModule;
    }

    /**
     * Terminate all clusters with the specified ids.
     *
     * @return Return true in case of success, false otherwise
     */
    public boolean terminate() {
        final Map<String, Cluster> clusters = providerModule.getListIntent(config).getList();
        boolean success = true;
        for (String clusterId : config.getClusterIds()) {
            final Cluster cluster = clusters.get(clusterId);
            if (cluster == null) {
                LOG.warn("No cluster with ID '{}' found.", clusterId);
                success = false;
                continue;
            }
            LOG.info("Terminating cluster with ID '{}'.", clusterId);
            if (!terminateCluster(cluster)) {
                LOG.info("Failed to terminate cluster '{}'!", clusterId);
                success = false;
            } else {
                LOG.info("Cluster '{}' terminated!", clusterId);
            }
        }
        return success;
    }

    protected abstract boolean terminateCluster(Cluster cluster);
}

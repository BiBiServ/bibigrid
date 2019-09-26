package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class TerminateIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntent.class);
    private final ProviderModule providerModule;
    protected final Client client;
    private final Configuration config;
    private String terminateResponse = "Internal server error!";

    public String getTerminateResponse() {
        return terminateResponse;
    }

    public void setTerminateResponse(String terminateResponse) {
        this.terminateResponse = terminateResponse;
    }

    protected TerminateIntent(ProviderModule providerModule, Client client, Configuration config) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
    }

    /**
     * Terminate all clusters with the specified ids.
     * Optional a user can be specified to terminate all of its clusters.
     *
     * @return Return true in case of success, false otherwise
     */
    public boolean terminate() {
        final Map<String, Cluster> clusters = providerModule.getListIntent(client, config).getList();
        boolean success = true;
        List<String> toRemove = new ArrayList<>();
        for (String clusterId : config.getClusterIds()) {
            // if '-t user-id'
            boolean isUser = false;
            for (Cluster c : clusters.values()) {
                if (clusterId.equals(c.getUser())) {
                    toRemove.add(c.getClusterId());
                    isUser = true;
                }
            }
            if (!isUser) {
                final Cluster cluster = clusters.get(clusterId);
                if (cluster == null) {
                    LOG.warn("No cluster with ID '{}' found.", clusterId);
                    terminateResponse ="No cluster with ID '"+clusterId+"' found.";
                    success = false;
                } else {
                    toRemove.add(clusterId);
                }
            }
        }
        for (String clusterId : toRemove) {
            LOG.info("Terminating cluster with ID '{}' ...", clusterId);
            final Cluster cluster = clusters.get(clusterId);
            if (terminateCluster(cluster)) {
                LOG.info("Cluster '{}' terminated!", clusterId);
                terminateResponse = "Cluster '"+clusterId+"' terminated!";
            } else {
                LOG.info("Failed to terminate cluster '{}'!", clusterId);
                terminateResponse = "Failed to terminate cluster '"+clusterId+"'!";
                success = false;
            }
        }
        return success;
    }

    protected abstract boolean terminateCluster(Cluster cluster);
}

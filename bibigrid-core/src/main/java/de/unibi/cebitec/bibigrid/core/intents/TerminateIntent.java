package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    /*
    The terminateResponse attribute is used to save the response of the terminate intent and make it accessible to other
    classes such as the terminate Controller of the bibigrid REST API where the terminate response needs to be sent back
    to the user via json-body and not only printed to console.
    */
    private String terminateResponse = "Internal server error!";

    protected TerminateIntent(ProviderModule providerModule, Client client, Configuration config) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
    }

    public String getTerminateResponse() {
        return terminateResponse;
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
                delete_Key(cluster);
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

    private void delete_Key(Cluster cluster) {
        try {
            Path p = Paths.get(Configuration.KEYS_DIR + System.getProperty("file.separator") + cluster.getKeyName());
            Files.delete(p);
            LOG.info("Private key {} deleted.",p.toString());
        } catch (IOException e) {
            //  ignore exception
        }

        try {
            Path p = Paths.get(Configuration.KEYS_DIR + System.getProperty("file.separator") + cluster.getKeyName() + ".pub");
            Files.delete(p);
            LOG.info("Public key {} deleted.",p.toString());
        } catch (IOException e) {
            //  ignore exception
        }

    }
}

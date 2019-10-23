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

    protected TerminateIntent(ProviderModule providerModule, Client client, Configuration config) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
    }

    /**
     * Terminates all clusters with the specified ids or from a specified user.
     * @return true in case of success, false otherwise
     */
    public boolean terminate() {
        final Map<String, Cluster> clusters = providerModule.getListIntent(client, config).getList();
        boolean success = true;
        List<Cluster> toRemove = new ArrayList<>();
        for (String clusterId : config.getClusterIds()) {
            // if '-t user-id'
            boolean isUser = false;
            for (Cluster c : clusters.values()) {
                if (clusterId.equals(c.getUser())) {
                    toRemove.add(c);
                    isUser = true;
                }
            }
            if (!isUser) {
                final Cluster cluster = clusters.get(clusterId);
                if (cluster == null) {
                    LOG.error("No cluster with ID '{}' found.", clusterId);
                    success = false;
                } else {
                    toRemove.add(cluster);
                }
            }
        }
        for (Cluster cluster: toRemove) {
            String clusterID = cluster.getClusterId();
            LOG.info("Terminating cluster with ID '{}' ...", clusterID);
            if (terminateCluster(cluster)) {
                delete_Key(cluster);
                LOG.info("Cluster '{}' terminated!", clusterID);
            } else {
                LOG.error("Failed to terminate cluster '{}'!", clusterID);
                success = false;
            }
        }
        return success;
    }

    /**
     * Terminates current instances in case of an error while setup.
     * @return true in case of success, false otherwise
     */
    public boolean terminateCurrentInstances() {
        return true;
    }

    /**
     * Terminates all clusters started by specified user.
     * @param user name of user
     * @return true in case of success, false otherwise
     */
    private boolean terminateUserInstances(String user) {
        return true;
    }

    /**
     * Terminates single worker instance.
     * @param cluster specified cluster
     * @return true, if worker instance terminated successfully
     */
    protected abstract boolean terminateWorker(Cluster cluster);

    /**
     * Terminates the whole cluster.
     * @param cluster specified cluster
     * @return true, if terminate successful
     */
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

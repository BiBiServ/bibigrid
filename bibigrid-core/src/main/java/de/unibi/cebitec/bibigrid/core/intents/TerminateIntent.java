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
     * @param parameters user-ids or cluster-ids
     */
    public void terminate(String[] parameters) {
        final Map<String, Cluster> clusters = providerModule.getListIntent(client, config).getList();
        int nr = 0;
        while (nr < parameters.length) {
            if (parameters[nr].equals("worker")) {
                if (++nr < parameters.length) {
                    Cluster workerCluster = clusters.get(parameters[nr]);
                    terminateWorker(workerCluster);
                } else {
                    LOG.error("Missing clusterId for worker termination.");
                }
                nr++;
            } else {
                terminate(parameters[nr]);
            }
        }
    }

    /**
     * Terminates the clusters with the specified id or specified user.
     * @param parameter user-id or cluster-id
     */
    public void terminate(String parameter) {
        final Map<String, Cluster> clusters = providerModule.getListIntent(client, config).getList();
        List<Cluster> toRemove = new ArrayList<>();
        Cluster provided = clusters.get(parameter);
        if (provided != null) {
            toRemove.add(provided);
        } else {
            // check if '-t user-id'
            for (Cluster cluster : clusters.values()) {
                if (parameter.equals(cluster.getUser())) {
                    toRemove.add(cluster);
                }
            }
        }

        if (toRemove.isEmpty()) {
            LOG.error("No cluster with ID '{}' found.", parameter);
            return;
        }

        for (Cluster cluster: toRemove) {
            String clusterId = cluster.getClusterId();
            LOG.info("Terminating cluster with ID '{}' ...", clusterId);
            if (terminateCluster(cluster)) {
                delete_Key(cluster);
                LOG.info("Cluster '{}' terminated!", clusterId);
            } else {
                LOG.error("Cluster '{}' could not be terminated successfully.", clusterId);
            }
        }
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

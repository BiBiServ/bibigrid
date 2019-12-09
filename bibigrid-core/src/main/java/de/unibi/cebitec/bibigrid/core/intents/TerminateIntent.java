package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.*;
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
 * Intent to process termination of cluster or worker instances.
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
        for (String clusterId : parameters) {
            terminate(clusterId);
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
     * Scale down cluster by terminating specified worker instances.
     * @param clusterId Id of specified cluster
     * @param workerBatch idx of worker configuration
     * @param count nr of worker instances to be terminated
     */
    public void terminateInstances(String clusterId, int workerBatch, int count) {
        final Map<String, Cluster> clusters = providerModule.getListIntent(client, config).getList();
        Cluster cluster = clusters.get(clusterId);
        List<Instance> workers = cluster.getWorkerInstances(workerBatch);
        LOG.warn(workers.toString());
        if (workers.isEmpty() || workers.size() < count) {
            LOG.error("Could not find {} " + (count == 1 ? "worker" : "workers") + " with specified workerBatch in cluster.", count);
            return;
        }

        for (int i = 0; i < count; i++) {
            Instance worker = workers.get(i);
            if (terminateWorker(worker)) {
                LOG.info("Worker '{}' terminated!", worker.getId());
            } else {
                LOG.error("Worker '{}' could not be terminated successfully.", worker.getId());
            }
        }
    }

    /**
     * Terminates single worker instance.
     * @param worker specified worker instance
     * @return true, if worker instance terminated successfully
     */
    protected abstract boolean terminateWorker(Instance worker);

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

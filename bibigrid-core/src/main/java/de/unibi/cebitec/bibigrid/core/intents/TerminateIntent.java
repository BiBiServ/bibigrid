package de.unibi.cebitec.bibigrid.core.intents;

import com.jcraft.jsch.JSchException;
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

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;

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
    public boolean terminate(String[] parameters) {
        for (String clusterId : parameters) {
            if (!terminate(clusterId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Terminates the clusters with the specified id or specified user.
     * @param parameter user-id or cluster-id
     */
    public boolean terminate(String parameter) {
        LoadClusterConfigurationIntent loadIntent = providerModule.getLoadClusterConfigurationIntent(client, config);
        loadIntent.loadClusterConfiguration();
        final Map<String, Cluster> clusterMap = loadIntent.getClusterMap();
        if (clusterMap.isEmpty()) {
            return false;
        }
        List<Cluster> toRemove = new ArrayList<>();
        if (clusterMap.containsKey(parameter)) {
            Cluster provided = clusterMap.get(parameter);
            toRemove.add(provided);
        } else {
            // check if '-t user-id'
            for (Cluster cluster : clusterMap.values()) {
                if (parameter.equals(cluster.getUser())) {
                    toRemove.add(cluster);
                }
            }
        }
        LOG.info("Terminate given parameter {}", parameter);

        if (toRemove.isEmpty()) {
            LOG.error("No cluster with ID '{}' found.", parameter);
            return false;
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
        return true;
    }

    /**
     * Scale down cluster by terminating specified worker instances.
     * @param clusterId Id of specified cluster
     * @param workerBatch idx of worker configuration
     * @param count nr of worker instances to be terminated
     */
    public void terminateInstances(String clusterId, int workerBatch, int count) {
        LoadClusterConfigurationIntent loadIntent = providerModule.getLoadClusterConfigurationIntent(client, config);
        loadIntent.loadClusterConfiguration();
        Cluster cluster = loadIntent.getCluster(clusterId);
        if (cluster == null) {
            return;
        }
        List<Instance> workers = cluster.getWorkerInstances(workerBatch);
        if (workers.isEmpty() || workers.size() < count) {
            LOG.error("Could not find {} " + (count == 1 ? "worker" : "workers") + " with specified workerBatch in cluster.", count);
            return;
        }

        List<Instance> terminateList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            terminateList.add(workers.get(workers.size() - 1 - i));
        }

        List<Instance> failed = new ArrayList<>();
        for (Instance worker : terminateList) {
            if (terminateWorker(worker)) {
                LOG.info("Worker '{}' terminated!", worker.getName());
                cluster.removeWorkerInstance(worker);
            } else {
                failed.add(worker);
                LOG.error("Worker '{}' could not be terminated successfully.", worker.getName());
            }
        }
        try {
            AnsibleConfig.updateAnsibleWorkerLists(config, cluster, providerModule.getBlockDeviceBase());
        } catch (JSchException sshError) {
            failed.addAll(terminateList);
            LOG.error("Update may not be finished properly due to a connection error.");
            sshError.printStackTrace();
        } catch (IOException ioError) {
            failed.addAll(terminateList);
            LOG.error("Update may not be finished properly due to a KeyPair error.");
            ioError.printStackTrace();
        }
        if (failed.isEmpty()) {
            LOG.info(I, "{} instances have been successfully terminated from cluster {}.", terminateList.size(), cluster.getClusterId());
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

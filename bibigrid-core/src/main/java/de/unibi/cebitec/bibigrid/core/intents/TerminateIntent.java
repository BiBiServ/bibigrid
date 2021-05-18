package de.unibi.cebitec.bibigrid.core.intents;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.Scale;
import de.unibi.cebitec.bibigrid.core.util.ShellScriptCreator;
import de.unibi.cebitec.bibigrid.core.util.SshFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;

/**
 * Intent to process termination of cluster or worker instances.
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class TerminateIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntent.class);
    private final ProviderModule providerModule;
    protected final Client client;
    final Configuration config;
    final Map<String, Cluster> clusterMap;

    /*
    The terminateResponse attribute is used to save the response of the terminate intent and make it accessible to other
    classes such as the terminate Controller of the bibigrid REST API where the terminate response needs to be sent back
    to the user via json-body and not only printed to console.
    */
    private String terminateResponse = "Internal server error!";

    protected TerminateIntent(ProviderModule providerModule,
                              Client client,
                              Configuration config,
                              Map<String, Cluster> clusterMap) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
        this.clusterMap = clusterMap;
    }

    public String getTerminateResponse() {
        return terminateResponse;
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
     * Terminates specified cluster.
     * @param cluster cluster to be terminated
     * @return true, if cluster termination successful.
     */
    public boolean terminate(Cluster cluster) {
        String clusterId = cluster.getClusterId();
        if (terminateCluster(cluster)) {
            deleteClusterKeyPair(cluster);
            terminateResponse = "Cluster '" + clusterId + "' terminated!";
            LOG.info(I, "Cluster '{}' terminated!", clusterId);
            return true;
        } else {
            terminateResponse = "Failed to terminate cluster '" + clusterId + "'!";
            LOG.error("Cluster '{}' could not be terminated successfully.", clusterId);
            return false;
        }
    }

    /**
     * Terminates the clusters with the specified id or specified user.
     * @param parameter user-id or cluster-id
     */
    public boolean terminate(String parameter) {
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
            terminateResponse ="No cluster with ID '" + parameter + "' found.";
            LOG.error("No cluster with ID '{}' found.", parameter);
            return false;
        }

        boolean success = true;
        for (Cluster cluster: toRemove) {
            String clusterId = cluster.getClusterId();
            LOG.info("Terminating cluster with ID '{}' ...", clusterId);
            if (!terminateCluster(cluster)) success = false;
        }
        return success;
    }

    /**
     * Scale down cluster by terminating specified worker instances.
     * @param clusterId Id of specified cluster
     * @param workerBatch idx of worker configuration
     * @param count nr of worker instances to be terminated
     */
    public void terminateInstances(String clusterId, int workerBatch, int count) {
        LoadClusterConfigurationIntent loadIntent = providerModule.getLoadClusterConfigurationIntent(config);
        loadIntent.loadClusterConfiguration(clusterId);
        Cluster cluster = loadIntent.getCluster(clusterId);
        if (cluster == null) {
            return;
        }
        List<Instance> workers = cluster.getWorkerInstances(workerBatch);
        if (workers.isEmpty() || workers.size() < count) {
            if (count == 1) {
                LOG.error("Could not terminate worker with specified workerBatch in cluster.\n"
                        + "There is currently no worker node running with workerBatch {}.", workerBatch);
            } else {
                LOG.error("Could not terminate {} workers with specified workerBatch in cluster.\n"
                        + "There are currently {} worker nodes running with workerBatch {}.", count, workers.size(), workerBatch);
            }
            return;
        }
        LOG.info("Terminate {} workers for batch {} ...", count, workerBatch);
        List<Instance> terminateList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            terminateList.add(workers.get(workers.size() - 1 - i));
        }
        cluster.setDeletedInstances(terminateList);

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
        if (!SshFactory.pollSshPortIsAvailable(cluster.getMasterInstance().getPublicIp())) {
            return;
        }
        try {
            config.getClusterKeyPair().setName(CreateCluster.PREFIX + cluster.getClusterId());
            config.getClusterKeyPair().load();
            Session sshSession = SshFactory.createSshSession(
                    config.getSshUser(),
                    config.getClusterKeyPair(),
                    cluster.getMasterInstance().getPublicIp());
            sshSession.connect();
            AnsibleConfig.updateAnsibleWorkerLists(sshSession, config, cluster, providerModule);
            SshFactory.executeScript(sshSession, ShellScriptCreator.executeScaleTasksOnMaster(Scale.down));
            sshSession.disconnect();
        } catch (JSchException sshError) {
            failed.addAll(terminateList);
            LOG.error("Update may not be finished properly due to a connection error.");
            sshError.printStackTrace();
        } catch (IOException ioError) {
            failed.addAll(terminateList);
            LOG.error("Update may not be finished properly due to a KeyPair error.");
            ioError.printStackTrace();
        } catch (ConfigurationException ce) {
            failed.addAll(terminateList);
            LOG.error("Update may not be finished properly due to a Configuration error.");
            ce.printStackTrace();
        }
        if (failed.isEmpty()) {
            if (terminateList.size() == 1) {
                LOG.info(I, "{} instance has been successfully terminated from cluster {}.", terminateList.size(), cluster.getClusterId());
            } else {
                LOG.info(I, "{} instances have been successfully terminated from cluster {}.", terminateList.size(), cluster.getClusterId());
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

    /**
     * Deletes Private and public key automatically generated by BiBiGrid in key dir.
     * @param cluster deleted cluster
     */
    private void deleteClusterKeyPair(Cluster cluster) {
        boolean success = true;
        Path keyPath = Paths.get(Configuration.KEYS_DIR + System.getProperty("file.separator") + cluster.getKeyName());;
        try {
            Files.delete(keyPath);
            LOG.info("Private key {} deleted.", keyPath.toString());
        } catch (IOException e) {
            success = false;
            //  ignore exception
        }

        try {
            Path p = Paths.get(Configuration.KEYS_DIR + System.getProperty("file.separator") + cluster.getKeyName() + ".pub");
            Files.delete(p);
            LOG.info("Public key {} deleted.",p.toString());
        } catch (IOException e) {
            success = false;
            //  ignore exception
        }
        if (!success)
            LOG.error("An error occurred during key deletion for cluster {}. Please remove the remains in {} directory.", cluster.getClusterId(), keyPath);
    }
}

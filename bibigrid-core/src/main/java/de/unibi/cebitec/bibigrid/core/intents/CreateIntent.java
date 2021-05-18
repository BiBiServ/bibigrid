package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.Constant;
import de.unibi.cebitec.bibigrid.core.DataBase;
<<<<<<< HEAD
import de.unibi.cebitec.bibigrid.core.model.Client;
=======
>>>>>>> Fixed rest api createCluster components
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.Status;
import de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;


/**
 * CreateIntent for starting a cluster.
 *
 *
 *
 */
public class CreateIntent implements Runnable{

    private static final Logger LOG = LoggerFactory.getLogger(CreateIntent.class);

    private ProviderModule module;
    private Configuration config;
    private CreateCluster cluster;
    private DataBase db = DataBase.getDataBase();

    public CreateIntent(ProviderModule module, Configuration config) {
        this.module = module;
        this.config = config;
        String clusterId = null;
        cluster = module.getCreateIntent(config, clusterId);
<<<<<<< HEAD
        db.status.put(cluster.getCluster().getClusterId(), new Status(Status.CODE.Preparing));
    }

    public String getClusterId(){
        return cluster.getCluster().getClusterId();
=======
        db.status.put(cluster.cluster.getClusterId(),new Status(Status.CODE.Preparing));
    }

    public String getClusterId(){
        return cluster.cluster.getClusterId();
>>>>>>> Fixed rest api createCluster components
    }

    public void create(){
        try {
            // configure environment
<<<<<<< HEAD
            db.status.put(cluster.getCluster().getClusterId(), new Status(Status.CODE.Configuring, "Configuring environment."));
=======
            db.status.put(cluster.cluster.getClusterId(), new Status(Status.CODE.Configuring, "Configuring environment."));
>>>>>>> Fixed rest api createCluster components
            cluster.createClusterEnvironment()
                    .createNetwork()
                    .createSubnet()
                    .createSecurityGroup()
                    .createKeyPair()
                    .createPlacementGroup();
<<<<<<< HEAD
            db.status.put(cluster.getCluster().getClusterId(), new Status(Status.CODE.Configuring, "Configuring instances."));
            boolean success = cluster.configureClusterInstances();
            db.status.put(cluster.getCluster().getClusterId(), new Status(Status.CODE.Creating));
=======
            db.status.put(cluster.cluster.getClusterId(), new Status(Status.CODE.Configuring, "Configuring instances."));
            cluster.configureClusterMasterInstance()
                    .configureClusterWorkerInstance();
            db.status.put(cluster.cluster.getClusterId(), new Status(Status.CODE.Creating));
>>>>>>> Fixed rest api createCluster components
            // configure cluster
            success = success && cluster.launchClusterInstances();
            if (success) {
<<<<<<< HEAD
                db.status.put(cluster.getCluster().getClusterId(), new Status(Status.CODE.Running));
            } else {
                /*  In DEBUG mode keep partial configured cluster running, otherwise clean it up */
                if (Configuration.DEBUG) {
                    db.status.put(cluster.getCluster().getClusterId(), new Status(Status.CODE.Error, Constant.KEEP));
                } else {
                    db.status.put(cluster.getCluster().getClusterId(), new Status(Status.CODE.Error, Constant.ABORT_WITH_INSTANCES_RUNNING));
                    Map<String, Cluster> clusterMap = new HashMap<>();
                    clusterMap.put(cluster.getCluster().getClusterId(), cluster.getCluster());
                    TerminateIntent cleanupIntent = module.getTerminateIntent(config, clusterMap);
                    cleanupIntent.terminate(cluster.getCluster().getClusterId());
=======
                db.status.put(cluster.cluster.getClusterId(), new Status(Status.CODE.Running));
            } else {
                /*  In DEBUG mode keep partial configured cluster running, otherwise clean it up */
                if (Configuration.DEBUG) {
                    db.status.put(cluster.cluster.getClusterId(), new Status(Status.CODE.Error, Constant.KEEP));
                } else {
                    db.status.put(cluster.cluster.getClusterId(), new Status(Status.CODE.Error, Constant.ABORT_WITH_INSTANCES_RUNNING));
                    Map<String, Cluster> clusterMap = new HashMap<>();
                    clusterMap.put(cluster.cluster.getClusterId(), cluster.cluster);
                    TerminateIntent cleanupIntent = module.getTerminateIntent(config, clusterMap);
                    cleanupIntent.terminate(cluster.cluster.getClusterId());
>>>>>>> Fixed rest api createCluster components
                }
            }

        } catch (ConfigurationException ex) {
<<<<<<< HEAD
            db.status.put(cluster.getCluster().getClusterId(),new Status(Status.CODE.Error,"Failed to create cluster. "+ex.getMessage()));
=======
            db.status.put(cluster.cluster.getClusterId(),new Status(Status.CODE.Error,"Failed to create cluster. "+ex.getMessage()));
>>>>>>> Fixed rest api createCluster components
            // print stacktrace only in verbose mode
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                LOG.error("Failed to create cluster. {} {}", ex.getMessage(), ex);
            }
        }

    }

    @Override
    public void run() {
        try {
            // we have to do authenticate again,
            // if authentication was done in a separate thread.
            MDC.put("cluster", getClusterId());
            module.client.authenticate();
            create();
        } catch (ClientConnectionFailedException ex) {
<<<<<<< HEAD
            db.status.put(cluster.getCluster().getClusterId(),new Status(Status.CODE.Error,"Client connection failed. "+ex.getMessage()));
=======
            db.status.put(cluster.cluster.getClusterId(),new Status(Status.CODE.Error,"Client connection failed. "+ex.getMessage()));
>>>>>>> Fixed rest api createCluster components
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                LOG.error("Client connection failed. {} {}", ex.getMessage(), ex);
            }

        }
    }
}

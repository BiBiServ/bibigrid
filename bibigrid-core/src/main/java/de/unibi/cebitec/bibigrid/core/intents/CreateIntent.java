package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.Constant;
import de.unibi.cebitec.bibigrid.core.DataBase;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.Status;
import de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter;
import org.graalvm.compiler.core.common.util.CompilationAlarm_OptionDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    private Client client;
    private CreateCluster cluster;
    private DataBase db = DataBase.getDataBase();


    public CreateIntent(ProviderModule module, Configuration config, Client client) {
        this.module = module;
        this.config = config;
        this.client = client;
        cluster = module.getCreateIntent(client, config);
        db.status.put(cluster.clusterId,new Status(Status.CODE.Preparing));
    }

    public String getClusterId(){
        return cluster.clusterId;
    }

    public void create(){
        try {
            // configure environment
            db.status.put(cluster.clusterId, new Status(Status.CODE.Configuring, "Configuring environment."));
            cluster.createClusterEnvironment()
                    .createNetwork()
                    .createSubnet()
                    .createSecurityGroup()
                    .createKeyPair()
                    .createPlacementGroup();
            db.status.put(cluster.clusterId, new Status(Status.CODE.Configuring, "Configuring instances."));
            cluster.configureClusterMasterInstance()
                    .configureClusterWorkerInstance();
            db.status.put(cluster.clusterId, new Status(Status.CODE.Creating));
            // configure cluster
            boolean success = cluster
                    .launchClusterInstances(false); //ToDo: Remove prepare flag permanently
            if (success) {
                db.status.put(cluster.clusterId, new Status(Status.CODE.Running));
            } else {
                /*  In DEBUG mode keep partial configured cluster running, otherwise clean it up */
                if (Configuration.DEBUG) {
                    db.status.put(cluster.clusterId, new Status(Status.CODE.Error, Constant.KEEP));
                } else {
                    db.status.put(cluster.clusterId, new Status(Status.CODE.Error, Constant.ABORT_WITH_INSTANCES_RUNNING));
                    TerminateIntent cleanupIntent = module.getTerminateIntent(client, config);
                    cleanupIntent.terminate();
                }
            }

        } catch (ConfigurationException ex) {
            db.status.put(cluster.clusterId,new Status(Status.CODE.Error,"Failed to create cluster. "+ex.getMessage()));
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
            cluster.client.authenticate();
        } catch (ClientConnectionFailedException ex) {
            db.status.put(cluster.clusterId,new Status(Status.CODE.Error,"Client connection failed. "+ex.getMessage()));
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                LOG.error("Client connection failed. {} {}", ex.getMessage(), ex);
            }

        }
    }
}

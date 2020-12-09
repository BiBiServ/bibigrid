package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.DataBase;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;

import de.unibi.cebitec.bibigrid.core.util.Status;
import de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static de.unibi.cebitec.bibigrid.core.model.IntentMode.SCALE_DOWN;
import static de.unibi.cebitec.bibigrid.core.model.IntentMode.SCALE_UP;

public class ScaleWorkerIntent implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(CreateIntent.class);

    private final CreateCluster cluster;
    private final int batchIndex;
    private final int count;
    private final ProviderModule module;
    private final Configuration config;
    private final String scaling;
    private final DataBase db = DataBase.getDataBase();


    public ScaleWorkerIntent(ProviderModule module, Configuration config, String clusterId, int batchIndex, int count, String scaling) {
        this.batchIndex = batchIndex;
        this.module = module;
        this.count = count;
        this.config = config;
        this.scaling = scaling;
        cluster = module.getCreateIntent(config, clusterId);

    }

    public String getClusterId() {
        return cluster.clusterId;
    }

    private void scaleDown() {
        db.status.put(cluster.clusterId, new Status(Status.CODE.Scale_Down, "Scaling down the cluster by " + count + " worker!"));
        module.getTerminateIntent(config)
                .terminateInstances(cluster.clusterId, batchIndex, count);
        db.status.put(cluster.clusterId, new Status(Status.CODE.Running));

    }

    private void scaleUp() {
        db.status.put(cluster.clusterId, new Status(Status.CODE.Scale_Up, "Scaling up the cluster by " + count + " worker!"));
        cluster.createWorkerInstances(batchIndex, count);
        db.status.put(cluster.clusterId, new Status(Status.CODE.Running));
    }


    @Override
    public void run() {
        try {
            // we have to do authenticate again,
            // if authentication was done in a separate thread.
            MDC.put("cluster", getClusterId());
            module.client.authenticate();
            if (scaling.equals(SCALE_DOWN.getShortParam()) || scaling.equals(SCALE_DOWN.getLongParam())) {
                scaleDown();
            } else if (scaling.equals(SCALE_UP.getShortParam()) || scaling.equals(SCALE_UP.getLongParam())) {
                scaleUp();
            }


        } catch (ClientConnectionFailedException ex) {

            if (VerboseOutputFilter.SHOW_VERBOSE) {
                LOG.error("Client connection failed. {} {}", ex.getMessage(), ex);
            }

        }
    }
}

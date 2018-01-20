package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Snapshot;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * Implementation of the general ValidateIntent interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentAzure extends ValidateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ValidateIntentAzure.class);
    private final ConfigurationAzure config;
    private Azure compute;

    ValidateIntentAzure(final ConfigurationAzure config) {
        super(config);
        this.config = config;
    }

    @Override
    protected boolean connect() {
        compute = AzureUtils.getComputeService(config);
        return compute != null;
    }

    @Override
    protected boolean checkImages() {
        boolean allCheck = true;
        boolean foundMaster = AzureUtils.getImage(compute, config, config.getMasterImage()) != null;
        boolean foundSlave = AzureUtils.getImage(compute, config, config.getSlaveImage()) != null;
        if (foundSlave && foundMaster) {
            LOG.info(I, "Master and Slave images have been found.");
        } else {
            LOG.error("Master and Slave images could not be found.");
            allCheck = false;
        }
        return allCheck;
    }

    @Override
    protected boolean checkSnapshot(String snapshotId) {
        if (snapshotId.contains(":")) {
            snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
        }
        for (Snapshot snapshot : compute.snapshots().list()) {
            if (snapshot != null && snapshot.name().equals(snapshotId)) {
                LOG.info(V, "Snapshot {} found.", snapshotId);
                return true;
            }
        }
        LOG.error("Snapshot {} could not be found.", snapshotId);
        return false;
    }
}
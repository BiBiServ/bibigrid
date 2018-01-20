package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implementation of the general ListIntent interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ListIntentAzure extends ListIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ListIntentAzure.class);
    private final ConfigurationAzure config;

    ListIntentAzure(final ConfigurationAzure config) {
        this.config = config;
    }

    @Override
    protected void searchClusterIfNecessary() {
        if (clusterMap != null) {
            return;
        }
        clusterMap = new HashMap<>();
        Azure compute = AzureUtils.getComputeService(config);
        if (compute == null)
            return;
        PagedList<VirtualMachine> instancePage = compute.virtualMachines().list();
        instancePage.forEach(this::checkInstance);
    }

    private void checkInstance(VirtualMachine instance) {
        // check for cluster ID
        String clusterId = instance.tags().getOrDefault("bibigrid-id", null);
        if (clusterId == null)
            return;
        String name = instance.tags().getOrDefault("name", null);
        String user = instance.tags().getOrDefault("user", null);
        long creationTimestamp = Long.parseLong(instance.tags().getOrDefault("creation", "0"));
        Cluster cluster = clusterMap.containsKey(clusterId) ? clusterMap.get(clusterId) : new Cluster();

        // Check whether master or slave instance
        if (name != null && name.contains("master-")) {
            if (cluster.getMasterInstance() == null) {
                cluster.setMasterInstance(instance.computerName());
                cluster.setStarted(dateFormatter.format(new Date(creationTimestamp)));
            } else {
                LOG.error("Detect two master instances ({},{}) for cluster '{}' ",
                        cluster.getMasterInstance(), instance.computerName(), clusterId);
                System.exit(1);
            }
        } else {
            cluster.addSlaveInstance(instance.computerName());
        }
        /*
        //keyname - should be always the same for all instances of one cluster
        String keyName = ""; // TODO: getKeyName
        if (cluster.getKeyName() != null) {
            if (!cluster.getKeyName().equals(keyName)) {
                LOG.error("Detect two different keynames ({},{}) for cluster '{}'",
                        cluster.getKeyName(), keyName, clusterId);
            }
        } else {
            cluster.setKeyName(keyName);
        }
        */
        // user - should be always the same for all instances of one cluster
        if (user != null) {
            if (cluster.getUser() == null) {
                cluster.setUser(user);
            } else if (!cluster.getUser().equals(user)) {
                LOG.error("Detect two different users ({},{}) for cluster '{}'",
                        cluster.getUser(), user, clusterId);
            }
        }
        clusterMap.put(clusterId, cluster);
    }
}
package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.FirewallList;
import com.google.api.services.compute.model.Operation;
import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the general TerminateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class TerminateIntentGoogleCloud implements TerminateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntentGoogleCloud.class);

    private final ConfigurationGoogleCloud config;

    TerminateIntentGoogleCloud(final ConfigurationGoogleCloud config) {
        this.config = config;
    }

    public boolean terminate() {
        final Compute compute = GoogleCloudUtils.getComputeService(config);
        if (compute == null) {
            return false;
        }
        final Map<String, Cluster> clusters = new ListIntentGoogleCloud(config).getList();
        boolean success = true;
        for (String clusterId : config.getClusterIds()) {
            final Cluster cluster = clusters.get(clusterId);
            if (cluster == null) {
                LOG.warn("No cluster with id {} found.", clusterId);
                success = false;
                continue;
            }
            LOG.info("Terminating cluster with ID: {}", clusterId);
            terminateInstances(compute, cluster);
            terminateNetwork(compute, clusterId);
            LOG.info("Cluster '{}' terminated!", clusterId);
        }
        return success;
    }

    private void terminateInstances(final Compute compute, final Cluster cluster) {
        final String zone = config.getAvailabilityZone();
        List<String> instances = cluster.getSlaveInstances();
        if (cluster.getMasterInstance() != null) {
            instances.add(cluster.getMasterInstance());
        }
        if (instances.size() > 0) {
            LOG.info("Wait for {} instances to shut down. This can take a while, so please be patient!", instances.size());
            for (String i : instances) {
                try {
                    Operation operation = compute.instances().delete(config.getGoogleProjectId(), zone, i).execute();
                    GoogleCloudUtils.waitForOperation(compute, config, operation);
                } catch (Exception e) {
                    LOG.error("Failed to delete instance {}. {}", i, e);
                }
            }
        }
    }

    private void terminateNetwork(final Compute compute, final String clusterId) {
        List<String> firewallsToRemove = new ArrayList<>();
        try {
            // Collect all firewall rules that were created for this cluster
            FirewallList list = compute.firewalls().list(config.getGoogleProjectId()).execute();
            for (Firewall firewall : list.getItems()) {
                if (firewall.getName().startsWith(CreateClusterEnvironment.SECURITY_GROUP_PREFIX + "rule") &&
                        firewall.getName().endsWith(clusterId)) {
                    firewallsToRemove.add(firewall.getName());
                }
            }
            // Sequentially remove all the firewall rules
            for (String firewallLink : firewallsToRemove) {
                compute.firewalls().delete(config.getGoogleProjectId(), firewallLink).execute();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.FirewallList;
import com.google.cloud.compute.Compute;
import com.google.cloud.compute.Instance;
import com.google.cloud.compute.InstanceId;
import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        final Cluster cluster = new ListIntentGoogleCloud(config).getList().get(config.getClusterId());
        if (cluster == null) {
            LOG.warn("No cluster with id {} found.", config.getClusterId());
            return false;
        }
        LOG.info("Terminating cluster with ID: {}", config.getClusterId());
        terminateInstances(compute, cluster);
        terminateNetwork(compute);
        LOG.info("Cluster '{}' terminated!", config.getClusterId());
        return true;
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
                Instance instance = compute.getInstance(InstanceId.of(zone, i));
                try {
                    instance.delete().waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void terminateNetwork(final Compute compute) {
        com.google.api.services.compute.Compute internalCompute = GoogleCloudUtils.getInternalCompute(compute);
        List<String> firewallsToRemove = new ArrayList<>();
        try {
            // Collect all firewall rules that were created for this cluster
            FirewallList list = internalCompute.firewalls().list(config.getGoogleProjectId()).execute();
            for (Firewall firewall : list.getItems()) {
                if (firewall.getName().startsWith(CreateClusterEnvironment.SECURITY_GROUP_PREFIX + "rule") &&
                        firewall.getName().endsWith(config.getClusterId())) {
                    firewallsToRemove.add(firewall.getName());
                }
            }
            // Sequentially remove all the firewall rules
            for (String firewallLink : firewallsToRemove) {
                internalCompute.firewalls().delete(config.getGoogleProjectId(), firewallLink).execute();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
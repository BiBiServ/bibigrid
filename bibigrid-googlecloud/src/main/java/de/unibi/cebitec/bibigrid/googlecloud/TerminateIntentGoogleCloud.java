package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.FirewallList;
import com.google.api.services.compute.model.Operation;
import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
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
public class TerminateIntentGoogleCloud extends TerminateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntentGoogleCloud.class);
    private final ConfigurationGoogleCloud config;

    TerminateIntentGoogleCloud(ProviderModule providerModule, Client client, ConfigurationGoogleCloud config) {
        super(providerModule, client, config);
        this.config = config;
    }

    @Override
    protected boolean terminateCluster(Cluster cluster) {
        final Compute compute = ((ClientGoogleCloud) client).getInternal();
        return terminateInstances(compute, cluster) && terminateNetwork(compute, cluster);
    }

    private boolean terminateInstances(final Compute compute, final Cluster cluster) {
        final String zone = config.getAvailabilityZone();
        List<Instance> instances = cluster.getWorkerInstances();
        if (cluster.getMasterInstance() != null) {
            instances.add(cluster.getMasterInstance());
        }
        boolean success = true;
        if (instances.size() > 0) {
            LOG.info("Wait for {} instances to shut down. This can take a while, so please be patient!", instances.size());
            for (Instance i : instances) {
                try {
                    Operation operation = compute.instances().delete(config.getGoogleProjectId(), zone, i.getName()).execute();
                    GoogleCloudUtils.waitForOperation(compute, config, operation);
                } catch (Exception e) {
                    LOG.error("Failed to delete instance '{}'. {}", i, e);
                    success = false;
                }
            }
        }
        return success;
    }

    private boolean terminateNetwork(final Compute compute, final Cluster cluster) {
        List<String> firewallsToRemove = new ArrayList<>();
        try {
            // Collect all firewall rules that were created for this cluster
            FirewallList list = compute.firewalls().list(config.getGoogleProjectId()).execute();
            for (Firewall firewall : list.getItems()) {
                if (firewall.getName().startsWith(CreateClusterEnvironment.SECURITY_GROUP_PREFIX + "rule") &&
                        firewall.getName().endsWith(cluster.getClusterId())) {
                    firewallsToRemove.add(firewall.getName());
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to get the firewall rules for cluster '{}'. {}", cluster.getClusterId(), e);
            return false;
        }
        // Sequentially remove all the firewall rules
        for (String firewallLink : firewallsToRemove) {
            try {
                compute.firewalls().delete(config.getGoogleProjectId(), firewallLink).execute();
            } catch (IOException e) {
                LOG.error("Failed to delete firewall rule '{}'. {}", firewallLink, e);
                return false;
            }
        }
        return true;
    }
}
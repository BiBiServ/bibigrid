package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.FirewallList;
import com.google.cloud.compute.Compute;
import com.google.cloud.compute.Instance;
import com.google.cloud.compute.InstanceId;
import de.unibi.cebitec.bibigrid.meta.TerminateIntent;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static de.unibi.cebitec.bibigrid.meta.googlecloud.CreateClusterGoogleCloud.SECURITY_GROUP_PREFIX;

/**
 * Implementation of the general TerminateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class TerminateIntentGoogleCloud implements TerminateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntentGoogleCloud.class);

    private final Configuration conf;

    TerminateIntentGoogleCloud(final Configuration conf) {
        this.conf = conf;
    }

    public boolean terminate() {
        final Compute compute = GoogleCloudUtils.getComputeService(conf);
        final Cluster cluster = new ListIntentGoogleCloud(conf).getList().get(conf.getClusterId());
        if (cluster == null) {
            LOG.warn("No cluster with id {} found.", conf.getClusterId());
            return false;
        }
        LOG.info("Terminating cluster with ID: {}", conf.getClusterId());
        terminateInstances(compute, cluster);
        terminateNetwork(compute);
        LOG.info("Cluster '{}' terminated!", conf.getClusterId());
        return true;
    }

    private void terminateInstances(final Compute compute, final Cluster cluster) {
        final String zone = conf.getAvailabilityZone();
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
            FirewallList list = internalCompute.firewalls().list(conf.getGoogleProjectId()).execute();
            for (Firewall firewall : list.getItems()) {
                if (firewall.getName().startsWith(SECURITY_GROUP_PREFIX + "rule") &&
                        firewall.getName().endsWith(conf.getClusterId())) {
                    firewallsToRemove.add(firewall.getName());
                }
            }
            // Sequentially remove all the firewall rules
            for (String firewallLink : firewallsToRemove) {
                internalCompute.firewalls().delete(conf.getGoogleProjectId(), firewallLink).execute();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
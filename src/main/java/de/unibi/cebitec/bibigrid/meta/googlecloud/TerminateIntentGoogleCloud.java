package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.cloud.compute.Compute;
import com.google.cloud.compute.Instance;
import com.google.cloud.compute.InstanceId;
import de.unibi.cebitec.bibigrid.meta.TerminateIntent;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of the general TerminateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class TerminateIntentGoogleCloud implements TerminateIntent {

    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntentGoogleCloud.class);

    private final Configuration conf;
    private final String zone;
    private final Compute compute;
    private final Cluster cluster;

    public TerminateIntentGoogleCloud(final Configuration conf) {
        this.conf = conf;
        zone = conf.getAvailabilityZone();
        compute = GoogleCloudUtils.getComputeService(conf);
        cluster = new ListIntentGoogleCloud(conf).getList().get(conf.getClusterId());
    }

    public boolean terminate() {
        if (cluster == null) {
            LOG.warn("No cluster with id {} found.", conf.getClusterId());
            return false;
        }
        LOG.info("Terminating cluster with ID: {}", conf.getClusterId());
        terminateInstances();
        terminateNetwork();
        // TODO: remove disks created from snapshots?
        LOG.info("Cluster '{}' terminated!", conf.getClusterId());
        return true;
    }

    private void terminateInstances() {
        List<String> instances = cluster.getSlaveinstances();
        if (cluster.getMasterinstance() != null) {
            instances.add(cluster.getMasterinstance());
        }
        if (instances.size() > 0) {
            LOG.info("Wait for {} instances to shut down. This can take a while, so please be patient!", instances.size());
            for (String i : instances) {
                Instance instance = compute.getInstance(InstanceId.of(zone, i));
                try {
                    instance.delete().waitFor();
                } catch (TimeoutException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void terminateNetwork() {
        // TODO
    }
}
package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.cloud.compute.Compute;
import com.google.cloud.compute.ComputeOptions;
import com.google.cloud.compute.Instance;
import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.model.Configuration;

import java.util.List;

/**
 * Implementation of the general CreateCluster interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class CreateClusterGoogleCloud implements CreateCluster<CreateClusterGoogleCloud, CreateClusterEnvironmentGoogleCloud> {
    private final Configuration conf;

    private Compute compute;
    private Instance masterInstance;
    private List<Instance> slaveInstances;
    private CreateClusterEnvironmentGoogleCloud environment;

    public CreateClusterGoogleCloud(final Configuration conf) {
        this.conf = conf;
    }

    public CreateClusterEnvironmentGoogleCloud createClusterEnvironment() {
        compute = GoogleCloudUtils.getComputeService(conf);
        // TODO: stub
        return environment = new CreateClusterEnvironmentGoogleCloud(this);
    }

    public CreateClusterGoogleCloud configureClusterMasterInstance() {
        // TODO: stub
        return this;
    }

    public CreateClusterGoogleCloud configureClusterSlaveInstance() {
        // TODO: stub
        return this;
    }

    public boolean launchClusterInstances() {
        // TODO: stub
        return false;
    }
}
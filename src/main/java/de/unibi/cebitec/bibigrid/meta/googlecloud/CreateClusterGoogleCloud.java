package de.unibi.cebitec.bibigrid.meta.googlecloud;

import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.model.Configuration;

/**
 * Implementation of the general CreateCluster interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class CreateClusterGoogleCloud implements CreateCluster<CreateClusterGoogleCloud, CreateClusterEnvironmentGoogleCloud> {
    public CreateClusterGoogleCloud(final Configuration conf) {
        // TODO: stub
    }

    public CreateClusterEnvironmentGoogleCloud createClusterEnvironment() {
        // TODO: stub
        return null;
    }

    public CreateClusterGoogleCloud configureClusterMasterInstance() {
        // TODO: stub
        return null;
    }

    public CreateClusterGoogleCloud configureClusterSlaveInstance() {
        // TODO: stub
        return null;
    }

    public boolean launchClusterInstances() {
        // TODO: stub
        return false;
    }
}
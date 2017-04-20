package de.unibi.cebitec.bibigrid.meta.googlecloud;

import de.unibi.cebitec.bibigrid.exception.ConfigurationException;
import de.unibi.cebitec.bibigrid.meta.CreateClusterEnvironment;

/**
 * Implementation of the general CreateClusterEnvironment interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class CreateClusterEnvironmentGoogleCloud implements CreateClusterEnvironment<CreateClusterEnvironmentGoogleCloud, CreateClusterGoogleCloud> {
    public CreateClusterEnvironmentGoogleCloud(final CreateClusterGoogleCloud cluster) {
        // TODO: stub
    }

    public CreateClusterEnvironmentGoogleCloud createVPC() throws ConfigurationException {
        // TODO: stub
        return null;
    }

    public CreateClusterEnvironmentGoogleCloud createSubnet() throws ConfigurationException {
        // TODO: stub
        return null;
    }

    public CreateClusterEnvironmentGoogleCloud createSecurityGroup() throws ConfigurationException {
        // TODO: stub
        return null;
    }

    public CreateClusterGoogleCloud createPlacementGroup() throws ConfigurationException {
        // TODO: stub
        return null;
    }
}
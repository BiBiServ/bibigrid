package de.unibi.cebitec.bibigrid.intents;

import de.unibi.cebitec.bibigrid.model.exceptions.ConfigurationException;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public interface CreateClusterEnvironment {
    /**
     * Api specific implementation of creating or choosing an existing
     * Virtual Private Cloud.
     *
     * @throws ConfigurationException
     */
    CreateClusterEnvironment createVPC() throws ConfigurationException;

    /**
     * Api specific implementation of creating or choosing a Subnet.
     *
     * @throws ConfigurationException
     */
    CreateClusterEnvironment createSubnet() throws ConfigurationException;

    /**
     * Api specific implementation of creating or choosing a SecurityGroup.
     *
     * @throws ConfigurationException
     */
    CreateClusterEnvironment createSecurityGroup() throws ConfigurationException;

    /**
     * Api specific implementation of creating or choosing a placement group.
     * Needs to be the <b>LAST</b> Environment configuration and returns an
     * CreateCluster implementing Instance to step to instance configuration.
     *
     * @throws ConfigurationException
     */
    CreateCluster createPlacementGroup() throws ConfigurationException;
}

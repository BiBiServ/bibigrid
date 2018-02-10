package de.unibi.cebitec.bibigrid.core.intents;

import com.jcraft.jsch.JSchException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.ClusterKeyPair;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class CreateClusterEnvironment {
    public static final String SECURITY_GROUP_PREFIX = CreateCluster.PREFIX + "sg-";
    public static final String SUBNET_PREFIX = CreateCluster.PREFIX + "subnet-";
    private final ClusterKeyPair keypair;

    protected CreateClusterEnvironment() throws ConfigurationException {
        try {
            // create KeyPair for cluster communication
            keypair = new ClusterKeyPair();
        } catch (JSchException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
    }

    /**
     * Api specific implementation of creating or choosing an existing
     * Virtual Private Cloud.
     *
     * @throws ConfigurationException
     */
    public abstract CreateClusterEnvironment createVPC() throws ConfigurationException;

    /**
     * Api specific implementation of creating or choosing a Subnet.
     *
     * @throws ConfigurationException
     */
    public abstract CreateClusterEnvironment createSubnet() throws ConfigurationException;

    /**
     * Api specific implementation of creating or choosing a SecurityGroup.
     *
     * @throws ConfigurationException
     */
    public abstract CreateClusterEnvironment createSecurityGroup() throws ConfigurationException;

    /**
     * Api specific implementation of creating or choosing a placement group.
     * Needs to be the <b>LAST</b> Environment configuration and returns an
     * CreateCluster implementing Instance to step to instance configuration.
     *
     * @throws ConfigurationException
     */
    public abstract CreateCluster createPlacementGroup() throws ConfigurationException;

    public ClusterKeyPair getKeypair() {
        return keypair;
    }
}

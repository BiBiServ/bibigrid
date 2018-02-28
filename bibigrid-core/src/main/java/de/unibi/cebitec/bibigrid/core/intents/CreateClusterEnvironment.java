package de.unibi.cebitec.bibigrid.core.intents;

import com.jcraft.jsch.JSchException;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Network;
import de.unibi.cebitec.bibigrid.core.model.Subnet;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.ClusterKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class CreateClusterEnvironment {
    private static final Logger LOG = LoggerFactory.getLogger(CreateClusterEnvironment.class);
    public static final String SECURITY_GROUP_PREFIX = CreateCluster.PREFIX + "sg-";
    public static final String SUBNET_PREFIX = CreateCluster.PREFIX + "subnet-";
    protected final CreateCluster cluster;
    private final ClusterKeyPair keypair;
    protected Subnet subnet;
    protected Network network;

    protected CreateClusterEnvironment(CreateCluster cluster) throws ConfigurationException {
        this.cluster = cluster;
        try {
            // create KeyPair for cluster communication
            keypair = new ClusterKeyPair();
        } catch (JSchException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
    }

    /**
     * Api specific implementation of creating or choosing an existing Network.
     *
     * @throws ConfigurationException Throws an exception if the creation of the network failed.
     */
    public CreateClusterEnvironment createNetwork() throws ConfigurationException {
        network = getNetworkOrDefault(getConfig().getNetwork());
        if (network == null) {
            throw new ConfigurationException("No suitable network found! Define a valid network id or default network.");
        }
        if (network.getCidr() != null) {
            LOG.info(V, "Use network '{}' with CIDR '{}'.", network.getId(), network.getCidr());
        } else {
            LOG.info(V, "Use Network '{}'.", network.getName());
        }
        return this;
    }

    protected abstract Network getNetworkOrDefault(String networkId);

    /**
     * Api specific implementation of creating or choosing a Subnet.
     *
     * @throws ConfigurationException Throws an exception if the creation of the subnet failed.
     */
    public abstract CreateClusterEnvironment createSubnet() throws ConfigurationException;

    /**
     * Api specific implementation of creating or choosing a SecurityGroup.
     *
     * @throws ConfigurationException Throws an exception if the creation of the security group failed.
     */
    public abstract CreateClusterEnvironment createSecurityGroup() throws ConfigurationException;

    /**
     * Api specific implementation of creating or choosing a placement group.
     * Needs to be the <b>LAST</b> Environment configuration and returns an
     * CreateCluster implementing Instance to step to instance configuration.
     *
     * @throws ConfigurationException Throws an exception if the creation of the placement group failed.
     */
    public CreateCluster createPlacementGroup() throws ConfigurationException {
        // By default not implemented.
        return cluster;
    }

    protected Configuration getConfig() {
        return cluster.getConfig();
    }

    public final ClusterKeyPair getKeypair() {
        return keypair;
    }

    public final Network getNetwork() {
        return network;
    }

    public final Subnet getSubnet() {
        return subnet;
    }
}

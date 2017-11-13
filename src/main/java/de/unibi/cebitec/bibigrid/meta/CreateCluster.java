package de.unibi.cebitec.bibigrid.meta;

/**
 * CreateCluster Interface must be implemented by all "real" CreateCluster classes and
 * provides the minimum of general functions for the environment, the configuration of
 * master and slave instances and launching the cluster.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public interface CreateCluster {
    /**
     * The environment creation procedure. For a successful environment creation
     * you will need an Environment-Instance which implements the
     * CreateClusterEnvironment interface.
     * <p>
     * <h1><u>Example</u></h1>
     * <p>
     * <br />
     * <h2> The default procedure (AWS) </h2>
     * Configuration conf;
     * <ol>
     * <li>
     * new CreateClusterAWS(conf).createClusterEnvironment()
     * </li>
     * <li>
     * &#09.createVPC()
     * </li>
     * <li>
     * &#09.createSubnet()
     * </li>
     * <li>
     * &#09.createSecurityGroup()
     * </li>
     * <li>
     * &#09.createPlacementGroup()
     * </li>
     * <li>
     * .configureClusterMasterInstance()
     * </li>
     * <li>
     * .launchClusterInstances()
     * </li>
     * </ol>
     */
    CreateClusterEnvironment createClusterEnvironment();

    /**
     * Configure and manage Master-instance to launch.
     */
    CreateCluster configureClusterMasterInstance();

    /**
     * Configure and manage Slave-instances to launch.
     */
    CreateCluster configureClusterSlaveInstance();

    /**
     * Start the configured cluster now.
     */
    boolean launchClusterInstances();
}

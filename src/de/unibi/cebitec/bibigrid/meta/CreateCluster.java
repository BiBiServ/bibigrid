/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta;

/**
 * CreateCluster Interface must be implemented by all "real" CreateCluster classes and 
 * provides the minimum of general functions for the environment, the configuration of
 * master and slave instances and launching the cluster.
 * 
 * 
 * @author Johannes Steiner <jsteiner(at)cebitec.uni-bielefeld.de>
 * @param <P> The Provider Instance P
 * @param <E> The Environment Instance E
 */
public interface CreateCluster<P, E> {

    /**
     * The environment creation procedure. For a successful environment creation
     * you will need an Environment-Instance which implements the
     * CreateClusterEnvironment interface.
     *
     * <h1><u>Example</u></h1>
     *
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
     *
     * @return
     */
    public E createClusterEnvironment();

    /**
     * Configure and manage Master-instance to launch.
     * @return Provider P
     */
    public P configureClusterMasterInstance();

    /**
     * Configure and manage Slave-instances to launch.
     * @return Provider P
     */
    public P configureClusterSlaveInstance();

    /**
     * Start the configurated cluster now.
     * @return true if success
     */
    public boolean launchClusterInstances();

}

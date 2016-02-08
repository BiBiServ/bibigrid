/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta;

/**
 *
 * @author jsteiner
 * @param <E> The Environment specific Instance E
 * @param <P> The Provider specific Instance P
 */
public interface CreateClusterEnvironment<E, P> {

    /**
     * Api specific implementation of creating or choosing an exisiting
     * Virtual Private Cloud.
     * @return Environment E
     */
    public E createVPC();

    /**
     * Api specific implementation of creating or choosing a Subnet.
     *
     * @return Environment E
     */
    public E createSubnet();

    /**
     * Api specific implementation of creating or choosing a SecurityGroup.
     *
     * @return Environment E
     */
    public E createSecurityGroup();

    /**
     * Api specific implementation of creating or choosing a placement group.
     * Needs to be the <b>LAST</b> Environment configuration and returns an
     * CreateCluster implementing Instance to step to instance configuration.
     *
     * @return Provider P
     */
    public P createPlacementGroup();
}

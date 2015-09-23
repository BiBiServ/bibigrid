/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.meta.CreateClusterEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jsteiner
 */
public class CreateClusterEnvironmentOpenstack
        implements CreateClusterEnvironment<CreateClusterEnvironmentOpenstack, CreateClusterOpenstack> {

    private CreateClusterOpenstack cluster;

    public static final Logger log = LoggerFactory.getLogger(CreateClusterEnvironmentOpenstack.class);

    public CreateClusterEnvironmentOpenstack(CreateClusterOpenstack cluster) {
        this.cluster = cluster;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createVPC() {
        log.info("VPC creation not implemented yet");
        return this;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createSubnet() {
        log.info("Subnet creation not implementd yet");
        return this;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createSecurityGroup() {
        log.info("SecurityGroup creation not implemented yet");
        return this;
    }

    @Override
    public CreateClusterOpenstack createPlacementGroup() {
        log.info("PlacementGroup creation not implemented yet");
        return cluster;
    }

}

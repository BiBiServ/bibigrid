/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import com.jcraft.jsch.JSchException;
import de.unibi.cebitec.bibigrid.meta.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.model.Port;
import de.unibi.cebitec.bibigrid.util.KEYPAIR;
import java.util.List;
import java.util.logging.Level;
import org.openstack4j.api.Builders;
import org.openstack4j.api.compute.ComputeSecurityGroupService;
import org.openstack4j.api.networking.NetworkService;
import org.openstack4j.api.networking.SecurityGroupService;
import org.openstack4j.api.networking.SubnetService;
import org.openstack4j.model.compute.IPProtocol;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.SecGroupExtension.Rule;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
//import org.jclouds.net.domain.IpProtocol;
//import org.jclouds.openstack.neutron.v2.features.SubnetApi;
//import org.jclouds.openstack.nova.v2_0.domain.Ingress;
//import org.jclouds.openstack.nova.v2_0.domain.SecurityGroupRule;
//import org.jclouds.openstack.nova.v2_0.domain.SecurityGroup;
//import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 *
 * @author Johannes Steiner, Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class CreateClusterEnvironmentOpenstack
        implements CreateClusterEnvironment<CreateClusterEnvironmentOpenstack, CreateClusterOpenstack> {

    private CreateClusterOpenstack cluster;

    private SecurityGroup sg;
     private SecGroupExtension sge;

    private KEYPAIR keypair;

    public static final Logger LOG = LoggerFactory.getLogger(CreateClusterEnvironmentOpenstack.class);

    public CreateClusterEnvironmentOpenstack(CreateClusterOpenstack cluster) {
        this.cluster = cluster;
        try {
            keypair = new KEYPAIR();
        } catch (JSchException ex) {
            LOG.error(ex.getMessage(),ex);  
        }
    }

    @Override
    public CreateClusterEnvironmentOpenstack createVPC() {
        LOG.info("Network creation not implemented yet");
        return this;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createSubnet() {

        SubnetService sns = cluster.getOs().networking().subnet();
        NetworkService ns = cluster.getOs().networking().network();
        
        
        
        
        // @ToDo
        if (cluster.getConfiguration().getNetworkname() == null) {
            cluster.getConfiguration().setNetworkname(ns.list().get(0).getName());
        }
        
        if (cluster.getConfiguration().getSubnetname() == null) {
            cluster.getConfiguration().setSubnetname(sns.list().get(0).getName());
        }
//        SubNet subnet 
//        subnetapi.create(new CreateSubnet());
        LOG.info("Subnet creation not implementd yet");
        return this;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createSecurityGroup() {
        ComputeSecurityGroupService csgs = cluster.getOs().compute().securityGroups();
        sge = csgs.create("sg-" + cluster.getClusterId(), "Security Group for cluster: " + cluster.getClusterId());
               
        
        csgs.createRule(Builders.secGroupRule()
                .parentGroupId(sge.getId())
                .protocol(IPProtocol.TCP)
                .cidr("0.0.0.0/0")
                .range(22, 22)
                .build());
        
        csgs.createRule(Builders.secGroupRule()
                .parentGroupId(sge.getId())
                .protocol(IPProtocol.TCP)
                .groupId(sge.getId())
                .range(1,65535)
                .build());
        
        /**
         * User selected Ports.
         */
        List<Port> ports = cluster.getConfiguration().getPorts();
        for (Port p : ports) {
//            SecurityGroupRule rule_user_ingress = Builders.securityGroupRule()
//                .securityGroupId(sg.getId())
//                .protocol("tcp")
//                .direction("ingress")
//                .portRangeMin(p.number)
//                .portRangeMax(p.number)
//                .remoteIpPrefix(p.iprange)
//                .build(); 
            csgs.createRule(Builders.secGroupRule()
                .parentGroupId(sge.getId())
                .protocol(IPProtocol.TCP)
                .cidr(p.iprange)
                .range(p.number,p.number)
                .build());
        }
        LOG.info("SecurityGroup (ID: {}) created.", sge.getName());
        return this;
    }

    @Override
    public CreateClusterOpenstack createPlacementGroup() {
        LOG.info("PlacementGroup creation not implemented yet");
        return cluster;
    }

    public KEYPAIR getKeypair() {
        return this.keypair;
    }

    public SecurityGroup getSecurityGroup() {
        return sg;
    }
    
    public SecGroupExtension getSecGroupExtension() {
        return sge;
    }

}

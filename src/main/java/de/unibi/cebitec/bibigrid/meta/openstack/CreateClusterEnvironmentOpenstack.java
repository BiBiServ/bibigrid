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

    private KEYPAIR keypair;

    public static final Logger log = LoggerFactory.getLogger(CreateClusterEnvironmentOpenstack.class);

    public CreateClusterEnvironmentOpenstack(CreateClusterOpenstack cluster) {
        this.cluster = cluster;
        try {
            keypair = new KEYPAIR();
        } catch (JSchException ex) {
            java.util.logging.Logger.getLogger(CreateClusterEnvironmentOpenstack.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public CreateClusterEnvironmentOpenstack createVPC() {
        log.info("Network creation not implemented yet");
        return this;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createSubnet() {

        SubnetService sns = cluster.getOs().networking().subnet();

//        SubNet subnet 
//        subnetapi.create(new CreateSubnet());
        log.info("Subnet creation not implementd yet");
        return this;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createSecurityGroup() {
        //ComputeSecurityGroupService csgs = cluster.getOs().compute().securityGroups();
        SecurityGroupService sgs = cluster.getOs().networking().securitygroup();
        sg = sgs.create(Builders.securityGroup()
                .id("sg-" + cluster.getClusterId())
                .description("Security Group for cluster: " + cluster.getClusterId())
                .build());

        /**
         * Standard Rules.
         */
        SecurityGroupRule rule_ssh = Builders.securityGroupRule()
                .securityGroupId(sg.getId())
                .protocol("tcp")
                .direction("ingress")
                .ethertype("IPv4")
                .portRangeMin(22)
                .portRangeMax(22)
                .remoteIpPrefix("0.0.0.0/0")
                .build();
        SecurityGroupRule rule_all_tcp_ingress = Builders.securityGroupRule()
                .securityGroupId(sg.getId())
                .protocol("tcp")
                .direction("ingress")
                .ethertype("IPv4")
                .portRangeMin(1)
                .portRangeMax(65535)
                .remoteIpPrefix(sg.getId())
                .build();
        SecurityGroupRule rule_all_tcp_egress = Builders.securityGroupRule()
                .securityGroupId(sg.getId())
                .protocol("tcp")
                .direction("egress")
                .ethertype("IPv4")
                .portRangeMin(1)
                .portRangeMax(65535)
                .remoteIpPrefix(sg.getId())
                .build();
        SecurityGroupRule rule_all_udp_ingress = Builders.securityGroupRule()
                .securityGroupId(sg.getId())
                .protocol("udp")
                .direction("ingress")
                .ethertype("IPv4")
                .portRangeMin(1)
                .portRangeMax(65535)
                .remoteIpPrefix(sg.getId())
                .build();
        SecurityGroupRule rule_all_udp_egress = Builders.securityGroupRule()
                .securityGroupId(sg.getId())
                .protocol("udp")
                .direction("egress")
                .ethertype("IPv4")
                .portRangeMin(1)
                .portRangeMax(65535)
                .remoteIpPrefix(sg.getId())
                .build();

        /**
         * User selected Ports.
         */
        List<Port> ports = cluster.getConfiguration().getPorts();
        for (Port p : ports) {
            SecurityGroupRule rule_user_ingress = Builders.securityGroupRule()
                .securityGroupId(sg.getId())
                .protocol("tcp")
                .direction("ingress")
                .portRangeMin(p.number)
                .portRangeMax(p.number)
                .remoteIpPrefix(p.iprange)
                .build(); 
        }
        log.info("SecurityGroup (ID: {}) created.", sg.getName());
        return this;
    }

    @Override
    public CreateClusterOpenstack createPlacementGroup() {
        log.info("PlacementGroup creation not implemented yet");
        return cluster;
    }

    public KEYPAIR getKeypair() {
        return this.keypair;
    }

    public SecurityGroup getSecurityGroup() {
        return sg;
    }

}

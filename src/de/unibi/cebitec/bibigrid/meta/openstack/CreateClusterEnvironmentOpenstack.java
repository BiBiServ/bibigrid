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
import org.jclouds.net.domain.IpProtocol;
import org.jclouds.openstack.nova.v2_0.domain.Ingress;
import org.jclouds.openstack.nova.v2_0.domain.SecurityGroupRule;
import org.jclouds.openstack.nova.v2_0.domain.SecurityGroup;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jsteiner
 */
public class CreateClusterEnvironmentOpenstack
        implements CreateClusterEnvironment<CreateClusterEnvironmentOpenstack, CreateClusterOpenstack> {
    
    private CreateClusterOpenstack cluster;
    
    private SecurityGroup securityGroup;
    
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
        SecurityGroupApi s = cluster.getSecurityGroupApi();
        securityGroup = s.createWithDescription("sg-" + cluster.getClusterId(), "Security Group for cluster: " + cluster.getClusterId());
        /**
         * Standard Rules.
         */
        SecurityGroupRule rule_ssh = s.createRuleAllowingCidrBlock(securityGroup.getId(), Ingress.builder().ipProtocol(IpProtocol.TCP).fromPort(22).toPort(22).build(), "0.0.0.0/0");
        SecurityGroupRule rule_all_tcp = s.createRuleAllowingSecurityGroupId(securityGroup.getId(), Ingress.builder().ipProtocol(IpProtocol.TCP).fromPort(1).toPort(65535).build(), securityGroup.getId());
        SecurityGroupRule rule_all_udp = s.createRuleAllowingSecurityGroupId(securityGroup.getId(), Ingress.builder().ipProtocol(IpProtocol.UDP).fromPort(1).toPort(65535).build(), securityGroup.getId());
        
        /**
         * User selected Ports.
         */
        List<Port> ports = cluster.getConfiguration().getPorts();
        for (Port p : ports) {
            s.createRuleAllowingCidrBlock(securityGroup.getId(), Ingress.builder().fromPort(p.number).toPort(p.number).ipProtocol(IpProtocol.TCP).build(), p.iprange);
        }
        log.info("SecurityGroup (ID: {}) created.", securityGroup.getName());
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
        return securityGroup;
    }
    
}

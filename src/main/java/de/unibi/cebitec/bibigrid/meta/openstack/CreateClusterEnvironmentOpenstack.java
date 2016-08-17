/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import com.jcraft.jsch.JSchException;
import de.unibi.cebitec.bibigrid.meta.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.ConfigurationException;
import de.unibi.cebitec.bibigrid.model.Port;
import de.unibi.cebitec.bibigrid.util.KEYPAIR;
import java.util.List;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeSecurityGroupService;
import org.openstack4j.api.networking.NetworkService;
import org.openstack4j.api.networking.SubnetService;
import org.openstack4j.model.compute.IPProtocol;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepare the cloud environment for an OpenStack cluster.
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

    private static final String ROUTERPREFIX = "router-";
    private static final String NETWORKPREFIX = "net-";
    private static final String SUBNETWORKPREFIX = "subnet-";
    
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
        // complete setup is done by createSubNet
        return this;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createSubnet() throws ConfigurationException{
        
        /*
        User can specify three parameters to define the network connection for a
        BiBiGrid cluster
        
        1) router
        2) network
        3) subnetwork
        
        
        */
        
        
        OSClient osc = cluster.getOs();
        Configuration cfg = cluster.getConfiguration();
        String clusterid = cluster.getClusterId();
        
        
        Router router = null;
        Network net = null;
        Subnet subnet = null;

        // if no router is given create a new one
        if (cfg.getRoutername() == null) {
            router = osc.networking().router().create(Builders.router()
            .name(ROUTERPREFIX+clusterid)
            .externalGateway(cfg.getGatewayname())
            .build());
        // otherwise use existing one
        } else {
            for (Router r : osc.networking().router().list()){
                if (r.getName().equals(cfg.getRoutername())) {
                    router =  r;
                    break;
                }
            }
            if (router == null) {
                throw new ConfigurationException("No Router with name '"+cfg.getRoutername()+"' found!");
            }
        }

        
        if (cfg.getSubnetname() != null) {
            //  Check if subnetname exists
            
            //  Determine netname if  
            
        } else  {
        
            if (cfg.getNetworkname() == null) {
                // create new network
                net = osc.networking().network().create(Builders.network()
                    .name("net-" + cluster.getClusterId())
                    .adminStateUp(true)
                    .build());
                
            } 
            // and create a new subnetwork
            subnet = osc.networking().subnet().create(Builders.subnet()
                    .name("subnet-"+cluster.getClusterId())
                    .network(net)
                    .ipVersion(IPVersionType.V4)
                    .cidr("10.10.10.0/24")
                    .build());

        }
        
        SubnetService sns = osc.networking().subnet();
        NetworkService ns = osc.networking().network();
        
        
        
        
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

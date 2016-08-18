package de.unibi.cebitec.bibiserv.bibigrid.test;

import static de.unibi.cebitec.bibigrid.StartUp.getCMDLineOptionGroup;
import static de.unibi.cebitec.bibigrid.StartUp.getCMDLineOptions;
import de.unibi.cebitec.bibigrid.ctrl.CommandLineValidator;
import de.unibi.cebitec.bibigrid.ctrl.CreateIntent;
import de.unibi.cebitec.bibigrid.ctrl.Intent;
import de.unibi.cebitec.bibigrid.meta.openstack.CreateClusterEnvironmentOpenstack;
import de.unibi.cebitec.bibigrid.meta.openstack.CreateClusterOpenstack;
import de.unibi.cebitec.bibigrid.util.SubNets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.networking.NetworkService;
import org.openstack4j.api.networking.PortService;
import org.openstack4j.api.networking.SubnetService;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.options.PortListOptions;
import org.openstack4j.openstack.networking.domain.NeutronSubnet;
import org.openstack4j.openstack.networking.domain.NeutronSubnet.Subnets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test if a SubnetTest could be created and deleted afterwards ...
 *
 *
 * @author Jan Krueger - jkrueger(at)cebietc.ubi-bielefeld.de
 */
public class SubnetTest {

    public static Intent intent;

    public static final Logger LOG = LoggerFactory.getLogger(SubnetTest.class);

    @BeforeClass
    public static void setup() {

        //String bibigridproperties = System.getProperty("bibigrid.properties");
        String bibigridproperties = "/Users/jkrueger/.bibigrid.properties.OS";

        if (bibigridproperties == null) {
            LOG.warn("To run JUnit test, you have to set system property \"bibigrid.properties\" to a valid OpenStack bibigrid.properties file!");
        } else {
            CommandLineParser cli = new DefaultParser();
            OptionGroup intentOptions = getCMDLineOptionGroup();
            Options cmdLineOptions = getCMDLineOptions(intentOptions);
            try {
                CommandLine cl = cli.parse(cmdLineOptions, new String[]{"-o", bibigridproperties, "-ch"});
                intent = new CreateIntent();
                CommandLineValidator validator = new CommandLineValidator(cl, intent);
                Assert.assertTrue("Configuration must be valid OS bibigrid configuration file", validator.validate());
            } catch (ParseException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    CreateClusterOpenstack cluster;
    CreateClusterEnvironmentOpenstack clusterenv;

   
    public SubnetTest() {

        if (intent != null) {
            cluster = new CreateClusterOpenstack(intent.getConfiguration());
            clusterenv = cluster.createClusterEnvironment();
        }
        LOG.info("init");

    }

    //@Test
    public void TestCreate() {
        LOG.info("test create");
        if (intent != null) {
            OSClient osc = cluster.getOs();

            
            
            

            
            NetworkService ns = osc.networking().network();
            SubnetService sns = osc.networking().subnet();
            
            // create new Network
            Network net = osc.networking().network().create(Builders.network()
                    .name("net-" + cluster.getClusterId())
                    .adminStateUp(true)
                    .build());

            
            // Create a new subnetwork inside
            Subnet subnet = osc.networking().subnet().create(Builders.subnet()
                    .name("subnet-"+cluster.getClusterId())
                    .network(net)
                    .ipVersion(IPVersionType.V4)
                    .cidr("10.10.10.0/24")
                    .build());
            
        }
        Assert.assertTrue(true);
    }
    
    @Test
    public void TestRouter(){
        LOG.info("test Router");
        if (intent != null) {
         
        }
        Assert.assertTrue(true);
    }

    //@Test
    public void TestDelete() {
        LOG.info("test Delete");
        if (intent != null) {

        }
        Assert.assertTrue(true);
    }

}

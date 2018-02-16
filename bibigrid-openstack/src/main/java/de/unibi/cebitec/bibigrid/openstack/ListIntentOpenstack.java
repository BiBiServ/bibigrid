package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;

import java.util.*;
import java.util.stream.Collectors;

import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.*;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;

/**
 * Implements ListIntent for Openstack.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ListIntentOpenstack extends ListIntent {
    private final OSClient os;

    ListIntentOpenstack(final ProviderModule providerModule, final ConfigurationOpenstack config) {
        super(providerModule, config);
        os = OpenStackUtils.buildOSClient(config);
    }

    @Override
    protected void searchClusterIfNecessary() {
        super.searchClusterIfNecessary();
        Cluster cluster;
        // Security Group
        for (SecGroupExtension sg : os.compute().securityGroups().list()) {
            String name = sg.getName();
            if (name != null && name.startsWith(CreateClusterEnvironment.SECURITY_GROUP_PREFIX)) {
                String[] t = name.split("-");
                cluster = getOrCreateCluster(t[t.length - 1]);
                cluster.setSecurityGroup(sg.getId());
            }
        }
        // Network
        for (Network network : os.networking().network().list()) {
            String name = network.getName();
            if (name != null && name.startsWith(CreateClusterEnvironmentOpenstack.NETWORK_PREFIX)) {
                String[] t = name.split("-");
                cluster = getOrCreateCluster(t[t.length - 1]);
                cluster.setNetwork(network.getId());
            }
        }
        // Subnet
        for (Subnet subnet : os.networking().subnet().list()) {
            String name = subnet.getName();
            if (name != null && name.startsWith(CreateClusterEnvironment.SUBNET_PREFIX)) {
                String[] t = name.split("-");
                cluster = getOrCreateCluster(t[t.length - 1]);
                cluster.setSubnet(subnet.getId());
            }
        }
        // Router
        for (Router router : os.networking().router().list()) {
            String name = router.getName();
            if (name != null && name.startsWith(CreateClusterEnvironmentOpenstack.ROUTER_PREFIX)) {
                String[] t = name.split("-");
                cluster = getOrCreateCluster(t[t.length - 1]);
                cluster.setRouter(router.getId());
            }
        }
    }

    @Override
    protected List<Instance> getInstances() {
        return os.compute().servers().list().stream().map(i -> new InstanceOpenstack(null, i)).collect(Collectors.toList());
    }

    @Override
    protected void loadInstanceConfiguration(Instance instance) {
        Server server = ((InstanceOpenstack) instance).getInternal();
        Configuration.InstanceConfiguration instanceConfiguration = new Configuration.InstanceConfiguration();
        Flavor flavor = server.getFlavor();
        if (flavor != null) {
            instanceConfiguration.setType(flavor.getName());
            try {
                instanceConfiguration.setProviderType(providerModule.getInstanceType(config, flavor.getName()));
            } catch (InstanceTypeNotFoundException ignored) {
            }
        }
        Image image = server.getImage();
        if (image != null) {
            instanceConfiguration.setImage(image.getName());
        }
        instance.setConfiguration(instanceConfiguration);
    }
}

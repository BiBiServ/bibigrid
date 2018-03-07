package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.InstanceImage;
import de.unibi.cebitec.bibigrid.core.model.Network;
import de.unibi.cebitec.bibigrid.core.model.Subnet;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Image;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ClientOpenstack extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientOpenstack.class);

    private final OSClient internalClient;

    ClientOpenstack(ConfigurationOpenstack config) throws ClientConnectionFailedException {
        try {
            OSFactory.enableHttpLoggingFilter(config.isDebugRequests());
            internalClient = config.getOpenstackCredentials().getDomain() != null ?
                    buildOSClientV3(config) :
                    buildOSClientV2(config);
            LOG.info("Openstack connection established.");
        } catch (Exception e) {
            throw new ClientConnectionFailedException("Failed to connect openstack client.", e);
        }
    }

    private static OSClient buildOSClientV2(ConfigurationOpenstack config) {
        OpenStackCredentials osc = config.getOpenstackCredentials();
        return OSFactory.builderV2()
                .endpoint(config.getOpenstackCredentials().getEndpoint())
                .credentials(osc.getUsername(), osc.getPassword())
                .tenantName(osc.getTenantName())
                .authenticate();
    }

    private static OSClient buildOSClientV3(ConfigurationOpenstack config) {
        OpenStackCredentials osc = config.getOpenstackCredentials();
        return OSFactory.builderV3()
                .endpoint(config.getOpenstackCredentials().getEndpoint())
                .credentials(osc.getUsername(), osc.getPassword(), Identifier.byName(osc.getDomain()))
                //.scopeToProject(Identifier.byName(osc.getTenantName()), Identifier.byName(osc.getDomain()))
                .scopeToProject(Identifier.byName(osc.getTenantName()), Identifier.byName(osc.getTenantDomain()))
                .authenticate();
    }

    OSClient getInternal() {
        return internalClient;
    }

    @Override
    public Network getNetworkByName(String networkName) {
        for (org.openstack4j.model.network.Network network : internalClient.networking().network().list()) {
            if (network.getName().equals(networkName)) {
                // TODO: get router
                return new NetworkOpenstack(network, null);
            }
        }
        return null;
    }

    @Override
    public Network getNetworkById(String networkId) {
        org.openstack4j.model.network.Network network = internalClient.networking().network().get(networkId);
        // TODO: get router
        return network != null ? new NetworkOpenstack(network, null) : null;
    }

    @Override
    public Subnet getSubnetByName(String subnetName) {
        for (org.openstack4j.model.network.Subnet subnet : internalClient.networking().subnet().list()) {
            if (subnet.getName().equals(subnetName)) {
                return new SubnetOpenstack(subnet);
            }
        }
        return null;
    }

    @Override
    public Subnet getSubnetById(String subnetId) {
        org.openstack4j.model.network.Subnet subnet = internalClient.networking().subnet().get(subnetId);
        return subnet != null ? new SubnetOpenstack(subnet) : null;
    }

    @Override
    public InstanceImage getImageByName(String imageName) {
        for (Image image : internalClient.compute().images().list()) {
            if (image.getName().equals(imageName)) {
                return new InstanceImageOpenstack(image);
            }
        }
        return null;
    }

    @Override
    public InstanceImage getImageById(String imageId) {
        Image image = internalClient.compute().images().get(imageId);
        return image != null ? new InstanceImageOpenstack(image) : null;
    }
}

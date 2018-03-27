package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ClientOpenstack extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientOpenstack.class);

    private final OSClient internalClient;

    ClientOpenstack(ConfigurationOpenstack config) throws ClientConnectionFailedException {
        OpenStackCredentials credentials = config.getOpenstackCredentials();
        try {
            OSFactory.enableHttpLoggingFilter(config.isDebugRequests());
            internalClient = credentials.getDomain() != null ?
                    buildOSClientV3(credentials) :
                    buildOSClientV2(credentials);
            LOG.info("Openstack connection established.");
        } catch (Exception e) {
            throw new ClientConnectionFailedException("Failed to connect openstack client.", e);
        }
    }

    private static OSClient buildOSClientV2(OpenStackCredentials credentials) {
        return OSFactory.builderV2()
                .endpoint(credentials.getEndpoint())
                .credentials(credentials.getUsername(), credentials.getPassword())
                .tenantName(credentials.getTenantName())
                .authenticate();
    }

    private static OSClient buildOSClientV3(OpenStackCredentials credentials) {
        return OSFactory.builderV3()
                .endpoint(credentials.getEndpoint())
                .credentials(credentials.getUsername(), credentials.getPassword(), Identifier.byName(credentials.getDomain()))
                //.scopeToProject(Identifier.byName(credentials.getTenantName()), Identifier.byName(credentials.getDomain()))
                .scopeToProject(Identifier.byName(credentials.getTenantName()), Identifier.byName(credentials.getTenantDomain()))
                .authenticate();
    }

    OSClient getInternal() {
        return internalClient;
    }

    @Override
    public List<Network> getNetworks() {
        // TODO: get router
        return internalClient.networking().network().list()
                .stream().map(n -> new NetworkOpenstack(n, null)).collect(Collectors.toList());
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
    public Network getDefaultNetwork() {
        return null;
    }

    @Override
    public List<Subnet> getSubnets() {
        return internalClient.networking().subnet().list()
                .stream().map(SubnetOpenstack::new).collect(Collectors.toList());
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

    @Override
    public Snapshot getSnapshotByName(String snapshotName) {
        for (Volume snapshot : internalClient.blockStorage().volumes().list()) {
            if (snapshot.getName().equals(snapshotName)) {
                return new SnapshotOpenstack(snapshot);
            }
        }
        return null;
    }

    @Override
    public Snapshot getSnapshotById(String snapshotId) {
        Volume snapshot = internalClient.blockStorage().volumes().get(snapshotId);
        return snapshot != null && snapshot.getId().equals(snapshotId) ? new SnapshotOpenstack(snapshot) : null;
    }
}

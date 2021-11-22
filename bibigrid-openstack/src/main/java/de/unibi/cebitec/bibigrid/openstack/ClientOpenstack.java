package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ServerGroupService;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of abstract class Client for Openstack cloud framework.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 * @author jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ClientOpenstack extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientOpenstack.class);

    private OSClient.OSClientV3 internalClient;
    private final ConfigurationOpenstack config;

    ClientOpenstack(ConfigurationOpenstack config) throws ClientConnectionFailedException {
        this.config = config;
        authenticate();
    }

    private static OSClient.OSClientV3 buildOSClientV3(OpenStackCredentials credentials) {
        if (Configuration.DEBUG) {
            LOG.info(credentials.toString());
        }
        Identifier project = getIdentifier(credentials.getProject(), credentials.getProjectId());
        Identifier userDomain = getIdentifier(credentials.getUserDomain(), credentials.getUserDomainId());
        Identifier projectDomain = getIdentifier(credentials.getProjectDomain(), credentials.getProjectDomainId());
        return OSFactory.builderV3()
                .endpoint(credentials.getEndpoint())
                .credentials(credentials.getUsername(), credentials.getPassword(), userDomain)
                .scopeToProject(project, projectDomain)
                .authenticate();
    }

    static Identifier getIdentifier(String name, String id) {
        if (name == null) {
            if (id == null) {
                LOG.error("No ProjectDomain provided. Cannot authenticate via Openstack API.");
            } else {
                return Identifier.byId(id);
            }
        } else {
            return Identifier.byName(name);
        }
        return null;
    }

    public OSClient.OSClientV3 getInternal() {
        return internalClient;
    }

    @Override
    public void authenticate() throws ClientConnectionFailedException {
        OpenStackCredentials credentials = config.getOpenstackCredentials();
        try {
            OSFactory.enableHttpLoggingFilter(config.isDebugRequests());
            /* openstack4j can't follow "redirect" responses, therefore
               we try to redirect to identity version 3 manually */
            try {
                internalClient = buildOSClientV3(credentials);
            } catch (ClientResponseException e) {
                if (!credentials.getEndpoint().endsWith("/v3")){
                    credentials.setEndpoint(credentials.getEndpoint()+"/v3");
                    LOG.warn("AUTH endpoint seems not to point to identity api v3. Try again with"+ credentials.getEndpoint());
                    internalClient = buildOSClientV3(credentials);
                } else {
                    throw new ClientResponseException(e.getMessage(),e.getStatus());
                }
            }
            // select region
            if (config.getRegion() != null && !config.getRegion().equals(credentials.getRegion())) {
                LOG.warn("General region option '{}' overwrites Openstack credentials configuration '{}'!",config.getRegion(),credentials.getRegion());
                internalClient.useRegion(config.getRegion());
            } else {
                internalClient.useRegion(credentials.getRegion());
            }
            LOG.info("Openstack connection established.");
        } catch (AuthenticationException e) {
            LOG.error(e.getMessage());
            if (Configuration.DEBUG) {
                e.printStackTrace();
            }
            throw new ClientConnectionFailedException(String.format("Connection failed: %s. " +
                    "Please make sure the supplied OpenStack credentials are valid.", e.getLocalizedMessage()), e);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            if (Configuration.DEBUG) {
                e.printStackTrace();
            }
            throw new ClientConnectionFailedException(String.format("Failed to connect openstack " +
                    "client: %s: %s", e.getClass().getSimpleName(), e.getLocalizedMessage()), e);
        }
    }

    @Override
    public List<Network> getNetworks() {
        return internalClient.networking().network().list()
                .stream().map(n -> new NetworkOpenstack(n, null)).collect(Collectors.toList());
    }

    @Override
    public Network getNetworkByName(String networkName) {
        for (org.openstack4j.model.network.Network network : internalClient.networking().network().list()) {
            if (network.getName().equals(networkName)) {
                return new NetworkOpenstack(network, null);
            }
        }
        return null;
    }

    @Override
    public Network getNetworkById(String networkId) {
        org.openstack4j.model.network.Network network = internalClient.networking().network().get(networkId);
        return network != null ? new NetworkOpenstack(network, null) : null;
    }

    @Override
    public Network getNetworkByIdOrName(String net) {
        for (org.openstack4j.model.network.Network network : internalClient.networking().network().list()) {
            if (network.getId().equals(net) || network.getName().equals(net)) {
                return new NetworkOpenstack(network, null);
            }
        }
        return null;
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
    public List<String> getKeypairNames() {
        return internalClient.compute().keypairs().list()
                .stream().map(org.openstack4j.model.compute.Keypair::getName).collect(Collectors.toList());
    }

    @Override
    public Subnet getSubnetByName(String subnetName) {return getSubnetByIdOrName(subnetName); }

    @Override
    public Subnet getSubnetById(String subnetId) { return getSubnetByIdOrName(subnetId); }

    @Override
    public Subnet getSubnetByIdOrName(String snet) {
        for (org.openstack4j.model.network.Subnet subnet : internalClient.networking().subnet().list()) {
            if (subnet.getName().equals(snet) || subnet.getId().equals(snet)) {
                return new SubnetOpenstack(subnet);
            }
        }
        return null;
    }

    @Override
    public InstanceImage getImageByName(String imageName) {return  getImageByIdOrName(imageName); }

    @Override
    public InstanceImage getImageById(String imageId) { return getImageByIdOrName(imageId);}

    @Override
    public InstanceImage getImageByIdOrName(String img) {
        for (Image image : internalClient.compute().images().list()) {
            if (image.getStatus() == Image.Status.ACTIVE
                    && (image.getName().equals(img) || image.getId().equals(img))) {
                return new InstanceImageOpenstack(image);
            }
        }
        return null;
    }

    @Override
    public Snapshot getSnapshotByName(String snapshotName) { return getSnapshotByIdOrName(snapshotName);}

    @Override
    public Snapshot getSnapshotById(String snapshotId) { return getSnapshotByIdOrName(snapshotId);}

    @Override
    public Snapshot getSnapshotByIdOrName(String s) {
        for (Volume snapshot : internalClient.blockStorage().volumes().list()) {
            if (snapshot.getId().equals(s) || snapshot.getName().equals(s)) {
                return new SnapshotOpenstack(snapshot);
            }
        }
        return null;
    }

    @Override
    public ServerGroup getServerGroupByIdOrName(String serverGroup) {
        ServerGroupService sgs = internalClient.compute().serverGroups();
        List<? extends org.openstack4j.model.compute.ServerGroup> sgl = sgs.list();
        for (org.openstack4j.model.compute.ServerGroup sg : sgl) {
            if (sg.getId().equals(serverGroup)  || sg.getName().equals(serverGroup)) {
                return new ServerGroupOpenstack(sg);
            }
        }
        return null;
    }



}

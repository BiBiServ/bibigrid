package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.Subnetwork;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.NotImplementedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.NotYetSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of abstract client for Google
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 * @author jkrueger(at)cebitec.uni-bielefeld.de
 */
class ClientGoogleCloud extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientGoogleCloud.class);

    private ConfigurationGoogleCloud config;
    private Compute internalClient;

    ClientGoogleCloud(ConfigurationGoogleCloud config) throws ClientConnectionFailedException {
        this.config = config;
        authenticate();
    }

    Compute getInternal() {
        return internalClient;
    }

    @Override
    public void authenticate() throws ClientConnectionFailedException {
        try {
            if (config.isDebugRequests()) {
                HttpRequestLogHandler.attachToCloudHttpTransport();
            }
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(config.getCredentialsFile()));
            if (credential.createScopedRequired()) {
                credential = credential.createScoped(Collections.singletonList(ComputeScopes.COMPUTE));
            }
            internalClient = new Compute.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential)
                    .setApplicationName(config.getGoogleProjectId()).build();
        } catch (Exception e) {
            throw new ClientConnectionFailedException("Failed to connect google compute client.", e);
        }
        try {
            internalClient.projects().get(config.getGoogleProjectId()).execute();
        } catch (IOException e) {
            throw new ClientConnectionFailedException("Failed to connect google compute client.", e);
        }
        LOG.info("Google compute connection established.");
    }

    @Override
    public List<Network> getNetworks() {
        try {
            return internalClient.networks().list(config.getGoogleProjectId()).execute().getItems()
                    .stream().map(NetworkGoogleCloud::new).collect(Collectors.toList());
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public Network getNetworkByName(String networkName) { return getNetworkByIdOrName(networkName);}

    @Override
    public Network getNetworkById(String networkId) { return getNetworkByIdOrName(networkId);}

    @Override
    public Network getNetworkByIdOrName(String net)  {
        try {
            for (com.google.api.services.compute.model.Network network :
                    internalClient.networks().list(config.getGoogleProjectId()).execute().getItems()) {
                if (network.getId().toString().equalsIgnoreCase(net) ||
                    network.getName().equals(net)) {
                    return new NetworkGoogleCloud(network);
                }
            }
        } catch (IOException ignored) {
        }
        return null;

    }

    @Override
    public Network getDefaultNetwork() {
        return getNetworkByName("default");
    }

    @Override
    public List<Subnet> getSubnets() {
        try {
            return internalClient.subnetworks().list(config.getGoogleProjectId(), config.getRegion()).execute().getItems()
                    .stream().map(SubnetGoogleCloud::new).collect(Collectors.toList());
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public List<String> getKeypairNames() {
        throw new NotImplementedException();
    }

    @Override
    public Subnet getSubnetByName(String subnetName) { return getSubnetByIdOrName(subnetName);}

    @Override
    public Subnet getSubnetById(String subnetId) { return getSubnetByIdOrName(subnetId); }

    @Override
    public Subnet getSubnetByIdOrName(String sub)  {
        try {
            for (Subnetwork subnet : internalClient.subnetworks().list(config.getGoogleProjectId(),
                    config.getRegion()).execute().getItems()) {
                if (subnet.getId().toString().equalsIgnoreCase(sub) ||
                    subnet.getName().equals(sub)) {
                    return new SubnetGoogleCloud(subnet);
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public InstanceImage getImageByName(String imageName) { return getImageByIdOrName(imageName);}

    @Override
    public InstanceImage getImageById(String imageId) { return getImageByIdOrName(imageId); }

    @Override
    public InstanceImage getImageByIdOrName(String img) {
        try {
            for (Image image : internalClient.images().list(config.getGoogleImageProjectId()).execute().getItems()) {
                if (image.getId().toString().equalsIgnoreCase(img) ||
                    image.getName().equals(img)) {
                    return new InstanceImageGoogleCloud(image);
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public Snapshot getSnapshotByName(String snapshotName) { return getSnapshotByIdOrName(snapshotName);}

    @Override
    public Snapshot getSnapshotById(String snapshotId) { return getSnapshotByIdOrName(snapshotId); }

    @Override
    public Snapshot getSnapshotByIdOrName(String snap){
        try {
            for (com.google.api.services.compute.model.Snapshot snapshot :
                    internalClient.snapshots().list(config.getGoogleProjectId()).execute().getItems()) {
                if (snapshot.getId().toString().equalsIgnoreCase(snap) ||
                    snapshot.getName().equals(snap)) {
                    return new SnapshotGoogleCloud(snapshot);
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public ServerGroup getServerGroupByIdOrName(String serverGroup) throws NotYetSupportedException {
        throw new NotYetSupportedException("Server groups are currently not supported by BiBigrid Google.");
    }
}

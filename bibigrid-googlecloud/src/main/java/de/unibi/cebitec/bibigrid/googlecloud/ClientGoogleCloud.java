package de.unibi.cebitec.bibigrid.googlecloud;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ClientGoogleCloud extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientGoogleCloud.class);

    private final ConfigurationGoogleCloud config;
    private Compute internalClient;

    ClientGoogleCloud(ConfigurationGoogleCloud config) throws ClientConnectionFailedException {
        this.config = config;
        try {
            if (config.isDebugRequests()) {
                HttpRequestLogHandler.attachToCloudHttpTransport();
            }
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(config.getGoogleCredentialsFile()));
            if (credential.createScopedRequired()) {
                credential = credential.createScoped(Collections.singletonList(ComputeScopes.COMPUTE));
            }
            internalClient = new Compute.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential)
                    .setApplicationName(config.getGoogleProjectId()).build();
            LOG.info("Google compute connection established.");
        } catch (Exception e) {
            throw new ClientConnectionFailedException("Failed to connect google compute client.", e);
        }
    }

    Compute getInternal() {
        return internalClient;
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
    public Network getNetworkByName(String networkName) {
        try {
            return new NetworkGoogleCloud(internalClient.networks().get(config.getGoogleProjectId(),
                    networkName).execute());
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public Network getNetworkById(String networkId) {
        try {
            for (com.google.api.services.compute.model.Network network :
                    internalClient.networks().list(config.getGoogleProjectId()).execute().getItems()) {
                if (network.getId().toString().equalsIgnoreCase(networkId)) {
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
    public Subnet getSubnetByName(String subnetName) {
        try {
            return new SubnetGoogleCloud(internalClient.subnetworks().get(config.getGoogleProjectId(),
                    config.getRegion(), subnetName).execute());
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public Subnet getSubnetById(String subnetId) {
        try {
            for (Subnetwork subnet : internalClient.subnetworks().list(config.getGoogleProjectId(),
                    config.getRegion()).execute().getItems()) {
                if (subnet.getId().toString().equalsIgnoreCase(subnetId)) {
                    return new SubnetGoogleCloud(subnet);
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public InstanceImage getImageByName(String imageName) {
        try {
            Image image = internalClient.images().get(config.getGoogleImageProjectId(), imageName).execute();
            return image != null ? new InstanceImageGoogleCloud(image) : null;
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public InstanceImage getImageById(String imageId) {
        try {
            for (Image image : internalClient.images().list(config.getGoogleImageProjectId()).execute().getItems()) {
                if (image.getId().toString().equalsIgnoreCase(imageId)) {
                    return new InstanceImageGoogleCloud(image);
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public Snapshot getSnapshotByName(String snapshotName) {
        try {
            com.google.api.services.compute.model.Snapshot snapshot =
                    internalClient.snapshots().get(config.getGoogleProjectId(), snapshotName).execute();
            return snapshot != null ? new SnapshotGoogleCloud(snapshot) : null;
        } catch (IOException ignored) {
        }
        return null;
    }

    @Override
    public Snapshot getSnapshotById(String snapshotId) {
        try {
            for (com.google.api.services.compute.model.Snapshot snapshot :
                    internalClient.snapshots().list(config.getGoogleProjectId()).execute().getItems()) {
                if (snapshot.getId().toString().equalsIgnoreCase(snapshotId)) {
                    return new SnapshotGoogleCloud(snapshot);
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}

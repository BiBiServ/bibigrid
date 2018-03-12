package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.InstanceImage;
import de.unibi.cebitec.bibigrid.core.model.Network;
import de.unibi.cebitec.bibigrid.core.model.Subnet;
import de.unibi.cebitec.bibigrid.core.model.Snapshot;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ClientAWS extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientAWS.class);

    private AmazonEC2 internalClient;

    ClientAWS(ConfigurationAWS config) throws ClientConnectionFailedException {
        try {
            String endpoint = "ec2." + config.getRegion() + ".amazonaws.com";
            internalClient = AmazonEC2Client.builder()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, config.getRegion()))
                    .withCredentials(new AWSStaticCredentialsProvider(config.getCredentials()))
                    .build();
            LOG.info("AWS connection established.");
        } catch (Exception e) {
            throw new ClientConnectionFailedException("Failed to connect AWS client.", e);
        }
    }

    AmazonEC2 getInternal() {
        return internalClient;
    }

    @Override
    public List<Network> getNetworks() {
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        DescribeVpcsResult result = internalClient.describeVpcs(request);
        return result.getVpcs().stream().map(NetworkAWS::new).collect(Collectors.toList());
    }

    @Override
    public Network getNetworkByName(String networkName) {
        return getNetworkById(networkName);
    }

    @Override
    public Network getNetworkById(String networkId) {
        DescribeVpcsRequest request = new DescribeVpcsRequest().withVpcIds(networkId);
        DescribeVpcsResult result = internalClient.describeVpcs(request);
        return result != null && result.getVpcs().size() > 0 ? new NetworkAWS(result.getVpcs().get(0)) : null;
    }

    @Override
    public Network getDefaultNetwork() {
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        DescribeVpcsResult result = internalClient.describeVpcs(request);
        for (Vpc network : result.getVpcs()) {
            if (network.isDefault()) {
                return new NetworkAWS(network);
            }
        }
        return null;
    }

    @Override
    public List<Subnet> getSubnets() {
        DescribeSubnetsRequest request = new DescribeSubnetsRequest();
        DescribeSubnetsResult result = internalClient.describeSubnets(request);
        return result.getSubnets().stream().map(SubnetAWS::new).collect(Collectors.toList());
    }

    @Override
    public Subnet getSubnetByName(String subnetName) {
        return getSubnetById(subnetName);
    }

    @Override
    public Subnet getSubnetById(String subnetId) {
        DescribeSubnetsRequest request = new DescribeSubnetsRequest().withSubnetIds(subnetId);
        DescribeSubnetsResult result = internalClient.describeSubnets(request);
        return result != null && result.getSubnets().size() > 0 ? new SubnetAWS(result.getSubnets().get(0)) : null;
    }

    @Override
    public InstanceImage getImageByName(String imageName) {
        try {
            DescribeImagesRequest request = new DescribeImagesRequest();
            DescribeImagesResult result = internalClient.describeImages(request);
            if (result.getImages() != null) {
                for (Image image : result.getImages()) {
                    if (image.getName().equals(imageName)) {
                        return new InstanceImageAWS(image);
                    }
                }
            }
        } catch (AmazonServiceException ignored) {
        }
        return null;
    }

    @Override
    public InstanceImage getImageById(String imageId) {
        try {
            DescribeImagesRequest request = new DescribeImagesRequest().withImageIds(imageId);
            DescribeImagesResult result = internalClient.describeImages(request);
            List<Image> images = result.getImages();
            if (images == null || images.size() == 0) {
                return null;
            }
            if (images.size() > 1) {
                LOG.warn("Multiple images found for id '{}'.", imageId);
            }
            return new InstanceImageAWS(images.get(0));
        } catch (AmazonServiceException ignored) {
        }
        return null;
    }

    @Override
    public Snapshot getSnapshotByName(String snapshotName) {
        return getSnapshotById(snapshotName);
    }

    @Override
    public Snapshot getSnapshotById(String snapshotId) {
        DescribeSnapshotsRequest snapshotRequest = new DescribeSnapshotsRequest().withSnapshotIds(snapshotId);
        DescribeSnapshotsResult snapshotResult = internalClient.describeSnapshots(snapshotRequest);
        List<com.amazonaws.services.ec2.model.Snapshot> snapshots = snapshotResult.getSnapshots();
        return snapshots.size() > 0 && snapshots.get(0).getSnapshotId().equals(snapshotId) ?
                new SnapshotAWS(snapshots.get(0)) : null;
    }
}

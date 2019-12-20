package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.Snapshot;
import de.unibi.cebitec.bibigrid.core.model.Subnet;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.NotYetSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of abstract class Client for AWS.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 * @author jkrueger(at)cebitec.uni-bielefeld.de
 */
class ClientAWS extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientAWS.class);

    private Configuration config;
    private AmazonEC2 internalClient;

    ClientAWS(Configuration config) throws ClientConnectionFailedException {
        this.config = config;
        authenticate();
    }

    AmazonEC2 getInternal() {
        return internalClient;
    }

    @Override
    public void authenticate() throws ClientConnectionFailedException {
        AWSCredentials credentials;
        try {
            credentials = new PropertiesCredentials(Paths.get(config.getCredentialsFile()).toFile());
        } catch (IOException | IllegalArgumentException e) {
            throw new ClientConnectionFailedException("AWS credentials file could not be loaded.", e);
        }
        try {
            String endpoint = "ec2." + config.getRegion() + ".amazonaws.com";
            internalClient = AmazonEC2Client.builder()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, config.getRegion()))
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .build();
            LOG.info("AWS connection established.");
        } catch (Exception e) {
            throw new ClientConnectionFailedException("Failed to connect AWS client.", e);
        }
    }

    @Override
    public List<Network> getNetworks() {
        DescribeVpcsRequest request = new DescribeVpcsRequest();
        DescribeVpcsResult result = internalClient.describeVpcs(request);
        return result.getVpcs().stream().map(NetworkAWS::new).collect(Collectors.toList());
    }

    @Override
    public Network getNetworkByName(String networkName) {
        return getNetworkByIdOrName(networkName);
    }

    @Override
    public Network getNetworkById(String networkId)  {
        return getNetworkByIdOrName(networkId);
    }

    @Override
    public Network getNetworkByIdOrName(String network) {
        DescribeVpcsRequest request = new DescribeVpcsRequest().withVpcIds(network);
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
    public List<String> getKeypairNames() {
        DescribeKeyPairsRequest request = new DescribeKeyPairsRequest();
        DescribeKeyPairsResult result = internalClient.describeKeyPairs(request);
        return result.getKeyPairs().stream().map(KeyPairInfo::getKeyName).collect(Collectors.toList());
    }

    @Override
    public Subnet getSubnetByName(String subnetName) {
        return getSubnetByIdOrName(subnetName);
    }

    @Override
    public Subnet getSubnetById(String subnetId)  {
        return getSubnetByIdOrName(subnetId);
    }

    @Override
    public Subnet getSubnetByIdOrName(String subnet) {
        DescribeSubnetsRequest request = new DescribeSubnetsRequest().withSubnetIds(subnet);
        DescribeSubnetsResult result = internalClient.describeSubnets(request);
        return result != null && result.getSubnets().size() > 0 ? new SubnetAWS(result.getSubnets().get(0)) : null;
    }

    @Override
    public InstanceImage getImageByName(String imageName) {
        return getImageByIdOrName(imageName);
    }

    @Override
    public InstanceImage getImageById(String imageId) {
        return getImageByIdOrName(imageId);
    }

    @Override
    public InstanceImage getImageByIdOrName(String img) {
        try {
            DescribeImagesRequest request = new DescribeImagesRequest();
            DescribeImagesResult result = internalClient.describeImages(request);
            if (result.getImages() != null) {
                for (Image image : result.getImages()) {
                    if (image.getName().equals(img) || image.getImageId().equals(img)) {
                        return new InstanceImageAWS(image);
                    }
                }
            }
        } catch (AmazonServiceException ignored) {
        }
        return null;
    }

    @Override
    public Snapshot getSnapshotByName(String snapshotName) {
        return getSnapshotByIdOrName(snapshotName);
    }

    @Override
    public Snapshot getSnapshotById(String snapshotId) {
        return getSnapshotByIdOrName(snapshotId);
    }

    @Override
    public Snapshot getSnapshotByIdOrName(String snap) {
        DescribeSnapshotsRequest snapshotRequest = new DescribeSnapshotsRequest().withSnapshotIds(snap);
        DescribeSnapshotsResult snapshotResult = internalClient.describeSnapshots(snapshotRequest);
        List<com.amazonaws.services.ec2.model.Snapshot> snapshots = snapshotResult.getSnapshots();
        return snapshots.size() > 0 && snapshots.get(0).getSnapshotId().equals(snap) ?
                new SnapshotAWS(snapshots.get(0)) : null;
    }

    @Override
    public ServerGroup getServerGroupByIdOrName(String serverGroup) throws NotYetSupportedException {
        throw new NotYetSupportedException("Server groups are currently not supported by BiBigrid AWS.");
    }
}

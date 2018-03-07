package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Network;
import de.unibi.cebitec.bibigrid.core.model.Subnet;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public Subnet getSubnetByName(String subnetName) {
        return getSubnetById(subnetName);
    }

    @Override
    public Subnet getSubnetById(String subnetId) {
        DescribeSubnetsRequest request = new DescribeSubnetsRequest().withSubnetIds(subnetId);
        DescribeSubnetsResult result = internalClient.describeSubnets(request);
        return result != null && result.getSubnets().size() > 0 ? new SubnetAWS(result.getSubnets().get(0)) : null;
    }
}

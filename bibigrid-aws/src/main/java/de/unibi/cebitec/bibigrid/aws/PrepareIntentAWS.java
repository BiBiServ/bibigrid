package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import de.unibi.cebitec.bibigrid.core.intents.PrepareIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class PrepareIntentAWS extends PrepareIntent {
    private static final Logger LOG = LoggerFactory.getLogger(PrepareIntentAWS.class);

    private final AmazonEC2 ec2;

    PrepareIntentAWS(ProviderModule providerModule, Client client, Configuration config) {
        super(providerModule, client, config);
        ec2 = ((ClientAWS) client).getInternal();
    }

    @Override
    protected boolean stopInstance(Instance instance) {
        String instanceId = ((InstanceAWS) instance).getInternal().getInstanceId();
        StopInstancesRequest request = new StopInstancesRequest().withInstanceIds(instanceId);
        ec2.stopInstances(request);
        return true;
    }

    @Override
    protected void waitForInstanceShutdown(Instance instance) {
        String instanceId = ((InstanceAWS) instance).getInternal().getInstanceId();
        do {
            DescribeInstancesRequest request = new DescribeInstancesRequest().withInstanceIds(instanceId);
            try {
                DescribeInstancesResult result = ec2.describeInstances(request);
                InstanceState state = result.getReservations().get(0).getInstances().get(0).getState();
                LOG.info(V, "Status of instance '{}': {}", instance.getName(), state.getName());
                if (state.getName().equals(InstanceStateName.Stopped.toString())) {
                    break;
                } else {
                    LOG.info(V, "...");
                    sleep(10);
                }
            } catch (AmazonServiceException e) {
                LOG.debug("{}", e);
                sleep(3);
            }
        } while (true);
    }

    @Override
    protected boolean createImageFromInstance(Instance instance, String imageName) {
        String instanceId = ((InstanceAWS) instance).getInternal().getInstanceId();
        CreateImageRequest request = new CreateImageRequest().withName(imageName).withInstanceId(instanceId);
        // TODO: tag IMAGE_SOURCE_LABEL
        CreateImageResult result = ec2.createImage(request);
        return result != null && result.getImageId() != null && result.getImageId().length() > 0;
    }
}

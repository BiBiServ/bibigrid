package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;

import java.util.*;
import java.util.stream.Collectors;

import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;

/**
 * Implementation of the general ListIntent interface for an AWS based cluster.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ListIntentAWS extends ListIntent {
    private final ConfigurationAWS config;

    ListIntentAWS(final ProviderModule providerModule, final ConfigurationAWS config) {
        super(providerModule, config);
        this.config = config;
    }

    @Override
    protected List<Instance> getInstances() {
        AmazonEC2 ec2 = IntentUtils.getClient(config);
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
        List<Reservation> reservations = describeInstancesResult.getReservations();
        List<Instance> instances = new ArrayList<>();
        for (Reservation reservation : reservations) {
            instances.addAll(reservation.getInstances().stream().map(i -> new InstanceAWS(null, i)).collect(Collectors.toList()));
        }
        return instances;
    }

    @Override
    protected void checkInstance(Instance instance) {
        InstanceAWS instanceAWS = (InstanceAWS) instance;
        if (instanceAWS.getState().equals("pending") || instanceAWS.getState().equals("running") ||
                instanceAWS.getState().equals("stopping") || instanceAWS.getState().equals("stopped")) {
            super.checkInstance(instance);
        }
    }

    @Override
    protected void loadInstanceConfiguration(Instance instance) {
        com.amazonaws.services.ec2.model.Instance internalInstance = ((InstanceAWS) instance).getInternal();
        Configuration.InstanceConfiguration instanceConfiguration = new Configuration.InstanceConfiguration();
        instanceConfiguration.setType(internalInstance.getInstanceType());
        try {
            instanceConfiguration.setProviderType(providerModule.getInstanceType(config, internalInstance.getInstanceType()));
        } catch (InstanceTypeNotFoundException ignored) {
        }
        instanceConfiguration.setImage(internalInstance.getImageId());
        instance.setConfiguration(instanceConfiguration);
    }
}

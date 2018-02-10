package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;

import java.util.*;
import java.util.stream.Collectors;

import de.unibi.cebitec.bibigrid.core.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the general ListIntent interface for an AWS based cluster.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ListIntentAWS extends ListIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ListIntentAWS.class);
    private final ConfigurationAWS config;

    ListIntentAWS(final ConfigurationAWS config) {
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
            instances.addAll(reservation.getInstances().stream().map(InstanceAWS::new).collect(Collectors.toList()));
        }
        return instances;
    }

    @Override
    protected void checkInstance(Instance instance) {
        InstanceAWS instanceAWS = (InstanceAWS) instance;
        if (!instanceAWS.getState().equals("pending") && !instanceAWS.getState().equals("running") &&
                !instanceAWS.getState().equals("stopping") && !instanceAWS.getState().equals("stopped")) {
            return;
        }
        // check if instance is a BiBiGrid instance and extract clusterId from it
        String clusterId = getClusterIdForInstance(instance);
        if (clusterId == null)
            return;
        Cluster cluster = getOrCreateCluster(clusterId);
        // Check whether master or slave instance
        String name = instance.getTag(Instance.TAG_NAME);
        if (name != null && name.startsWith(CreateCluster.MASTER_NAME_PREFIX)) {
            if (cluster.getMasterInstance() == null) {
                cluster.setMasterInstance(instance.getName());
                cluster.setStarted(instance.getCreationTimestamp().format(dateTimeFormatter));
            } else {
                LOG.error("Detected two master instances ({},{}) for cluster '{}'.", cluster.getMasterInstance(),
                        instance.getName(), clusterId);
            }
        } else {
            cluster.addSlaveInstance(instance.getName());
        }
        //keyname - should be always the same for all instances of one cluster
        if (cluster.getKeyName() != null) {
            if (!cluster.getKeyName().equals(instanceAWS.getKeyName())) {
                LOG.error("Detected two different keynames ({},{}) for cluster '{}'.",
                        cluster.getKeyName(), instanceAWS.getKeyName(), clusterId);
            }
        } else {
            cluster.setKeyName(instanceAWS.getKeyName());
        }
        // user - should be always the same for all instances of one cluster
        String user = instance.getTag(Instance.TAG_USER);
        if (user != null) {
            if (cluster.getUser() == null) {
                cluster.setUser(user);
            } else if (!cluster.getUser().equals(user)) {
                LOG.error("Detected two different users ({},{}) for cluster '{}'.", cluster.getUser(), user, clusterId);
            }
        }
    }
}

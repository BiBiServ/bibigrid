package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.LoadClusterConfigurationIntent;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Image;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Uses Openstack API to load assign values from cluster servers to internal config.
 * Largely adopted from formerly ListIntentOpenstack
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 * @author Tim Dilger - tdilger(at)techfak.uni-bielefeld.de
 */
public class LoadClusterConfigurationIntentOpenstack extends LoadClusterConfigurationIntent {
    private static final Logger LOG = LoggerFactory.getLogger(LoadClusterConfigurationIntentOpenstack.class);
    private final OSClient os;

    LoadClusterConfigurationIntentOpenstack(ProviderModule providerModule, Client client, Configuration config) {
        super(providerModule, client, config);
        os = ((ClientOpenstack) client).getInternal();
    }

    @Override
    public Map<String, List<Instance>> createInstanceMap() {
        Map<String, List<Instance>> instanceList = new HashMap<>();
        for (Server server : os.compute().servers().list()) {
            Map<String, String> metadata = server.getMetadata();
            String clusterId = metadata.get(Instance.TAG_BIBIGRID_ID);
            InstanceOpenstack instance = new InstanceOpenstack(null, server);
            if (!metadata.containsKey(Instance.TAG_BATCH)) {
                instance.setMaster(true);
            } else {
                instance.setMaster(false);
            }
            if (instanceList.containsKey(clusterId)) {
                instanceList.get(clusterId).add(instance);
            } else {
                List<Instance> clusterInstances = new ArrayList<>();
                clusterInstances.add(instance);
                instanceList.put(clusterId, clusterInstances);
            }
        }
        return instanceList;
    }

    @Override
    public List<Instance> getInstances() {
        List<? extends Server> serverList = os.compute().servers().list();
        List<Instance> list = serverList.stream().map(i -> new InstanceOpenstack(null, i)).collect(Collectors.toList());
        Collections.reverse(list);
        return list;
    }

    @Override
    public void loadInstanceConfiguration(Instance instance) {
        Configuration.InstanceConfiguration instanceConfiguration =
                instance.isMaster() ? new Configuration.InstanceConfiguration()
                        : new Configuration.WorkerInstanceConfiguration();
        Server server = os.compute().servers().get(instance.getId());
        Flavor flavor = server.getFlavor();
        if (flavor != null) {
            instanceConfiguration.setType(flavor.getName());
            try {
                instanceConfiguration.setProviderType(providerModule.getInstanceType(client, config, flavor.getName()));
            } catch (InstanceTypeNotFoundException ignored) {
            }
        }
        Map<String, String> metadata = server.getMetadata();
        String workerBatch = metadata.get(Instance.TAG_BATCH);
        if (workerBatch != null) {
            instance.setBatchIndex(Integer.parseInt(workerBatch));
        } else {
            if (instance.isMaster()) {
                config.setAvailabilityZone(server.getAvailabilityZone());
            } else {
                LOG.warn("Could not set worker batch for instance {}. Continuing ...", instance.getId());
            }
        }
        Image image = server.getImage();
        if (image != null) {
            instanceConfiguration.setImage(image.getName());
        }
        instance.setConfiguration(instanceConfiguration);
    }

}

package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;

import java.util.*;
import java.util.stream.Collectors;

import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements ListIntent for Openstack.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ListIntentOpenstack extends ListIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ListIntentOpenstack.class);
    private final OSClient os;

    ListIntentOpenstack(final ProviderModule providerModule, Client client, final Configuration config) {
        super(providerModule, client, config);
        os = ((ClientOpenstack) client).getInternal();
    }

    @Override
    protected void assignClusterConfigValues() {
        super.assignClusterConfigValues();
        searchSecurityGroups();
    }

    private void searchSecurityGroups() {
        for (SecGroupExtension sg : os.compute().securityGroups().list()) {
            String name = sg.getName();
            if (name != null && name.startsWith(CreateClusterEnvironment.SECURITY_GROUP_PREFIX)) {
                getOrCreateCluster(getClusterIdFromName(name)).setSecurityGroup(sg.getId());
            }
        }
    }

    @Override
    protected List<Instance> getInstances() {
        List<? extends Server> serverList = os.compute().servers().list();
        List<Instance> list = serverList.stream().map(i -> new InstanceOpenstack(null, i)).collect(Collectors.toList());
        Collections.reverse(list);
        return list;
    }

    /**
     * SETS instanceConfiguration by internal configuration.
     * @param instance given master / worker instance
     */
    @Override
    protected void loadInstanceConfiguration(Instance instance) {
        Server server = ((InstanceOpenstack) instance).getInternal();
        Configuration.InstanceConfiguration instanceConfiguration =
                instance.isMaster()
                        ? new Configuration.InstanceConfiguration()
                        : new Configuration.WorkerInstanceConfiguration();
        Flavor flavor = server.getFlavor();
        if (flavor != null) {
            instanceConfiguration.setType(flavor.getName());
            try {
                instanceConfiguration.setProviderType(providerModule.getInstanceType(client, config, flavor.getName()));
            } catch (InstanceTypeNotFoundException ignored) {
            }
        }
        server.getAvailabilityZone()
        Map<String, String> metadata = server.getMetadata();
        String wb = metadata.get(Instance.TAG_BATCH);
        int workerBatch;
        if (wb != null) {
            workerBatch = Integer.parseInt(server.getMetadata().get(Instance.TAG_BATCH));
            instance.setBatchIndex(workerBatch);
        } else {
            if (instance.isMaster()) {
                LOG.warn("{} - Could not set worker batch. Continuing ...", instance.getId());
            }
        }
        Image image = server.getImage();
        if (image != null) {
            instanceConfiguration.setImage(image.getName());
        }
        instance.setConfiguration(instanceConfiguration);
    }
}

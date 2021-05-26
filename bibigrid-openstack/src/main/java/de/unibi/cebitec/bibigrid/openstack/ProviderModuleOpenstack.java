package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.openstack.intents.ListIntentOpenstack;
import de.unibi.cebitec.bibigrid.openstack.intents.PrepareIntentOpenstack;
import de.unibi.cebitec.bibigrid.openstack.intents.TerminateIntentOpenstack;
import de.unibi.cebitec.bibigrid.openstack.intents.ValidateIntentOpenstack;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;

import java.util.Map;
import java.util.HashMap;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de, jkrueger(a)cebitec.uni-bielefeld.de
 */
@SuppressWarnings("unused")
public class ProviderModuleOpenstack extends ProviderModule {
    @Override
    public String getName() {
        return "openstack";
    }

    @Override
    public Class<? extends Configuration> getConfigurationClass() {
        return ConfigurationOpenstack.class;
    }

    @Override
    public Validator getValidator(Configuration config, ProviderModule module) throws ConfigurationException {
        return new ValidatorOpenstack(config, module);
    }

    @Override
    public void createClient(Configuration config) throws ClientConnectionFailedException {
        client = new ClientOpenstack((ConfigurationOpenstack) config);
    }

    @Override
    public ListIntent getListIntent(Map<String, Cluster> clusterMap) {
        return new ListIntentOpenstack(clusterMap);
    }

    @Override
    public TerminateIntent getTerminateIntent(Configuration config, Map<String, Cluster> clusterMap) {
        return new TerminateIntentOpenstack(this, client, config, clusterMap);
    }

    @Override
    public PrepareIntent getPrepareIntent(Configuration config) {
        return new PrepareIntentOpenstack(this, client, config);
    }

    @Override
    public CreateCluster getCreateIntent(Configuration config, String clusterId) {
        return new CreateClusterOpenstack(this, config, clusterId);
    }

    @Override
    public ScaleWorkerIntent getScaleWorkerIntent(Configuration config, String clusterId, int batchIndex, int count, String scaling) {
        return new ScaleWorkerOpenstack(this, config, clusterId, batchIndex, count, scaling);

    }

    @Override
    public LoadClusterConfigurationIntent getLoadClusterConfigurationIntent(Configuration config) {
        return new LoadClusterConfigurationIntentOpenstack(this, config);
    }

    @Override
    public CreateClusterEnvironment getClusterEnvironment(CreateCluster cluster) throws ConfigurationException {
        return new CreateClusterEnvironmentOpenstack(client, (CreateClusterOpenstack) cluster);
    }

    @Override
    public ValidateIntent getValidateIntent(Configuration config) {
        return new ValidateIntentOpenstack(this, client, config);
    }

    @Override
    public String getBlockDeviceBase() {
        return "/dev/vd";
    }

    @Override
    protected Map<String, InstanceType> getInstanceTypeMap(Configuration config) {
        OSClient os = ((ClientOpenstack) client).getInternal();
        Map<String, InstanceType> instanceTypes = new HashMap<>();
        for (Flavor f : os.compute().flavors().list()) {
            instanceTypes.put(f.getName(), new InstanceTypeOpenstack(f));
        }
        return instanceTypes;
    }
}

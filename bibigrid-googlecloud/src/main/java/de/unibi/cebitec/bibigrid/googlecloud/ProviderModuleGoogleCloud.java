package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.MachineType;
import com.google.api.services.compute.model.MachineTypesScopedList;
import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
@SuppressWarnings("unused")
public class ProviderModuleGoogleCloud extends ProviderModule {
    @Override
    public String getName() {
        return "googlecloud";
    }

    @Override
    public Class<? extends Configuration> getConfigurationClass() {
        return ConfigurationGoogleCloud.class;
    }

    @Override
    public Validator getValidator(Configuration config, ProviderModule module) throws ConfigurationException {
        return new ValidatorGoogleCloud(config, module);
    }


    @Override
    public void createClient(Configuration config) throws ClientConnectionFailedException {
        client = new ClientGoogleCloud((ConfigurationGoogleCloud) config);
    }

    @Override
    public ListIntent getListIntent(Map<String, Cluster> clusterMap) {
        return new ListIntentGoogleCloud(this, clusterMap);
    }

    @Override
    public TerminateIntent getTerminateIntent(Configuration config) {
        return new TerminateIntentGoogleCloud(this, client, (ConfigurationGoogleCloud) config);
    }

    @Override
    public PrepareIntent getPrepareIntent(Configuration config) {
        return new PrepareIntentGoogleCloud(this, client, (ConfigurationGoogleCloud) config);
    }

    @Override
    public CreateCluster getCreateIntent(Configuration config, String clusterId) {
        return new CreateClusterGoogleCloud(this, (ConfigurationGoogleCloud) config, clusterId);
    }

    @Override
    public ScaleWorkerIntent getScaleWorkerIntent(Configuration config, String clusterId, int batchIndex, int count, String scaling) {
        return null;
    }

    @Override
    public LoadClusterConfigurationIntent getLoadClusterConfigurationIntent(Configuration config) {
        return null;
    }

    @Override
    public CreateClusterEnvironment getClusterEnvironment(CreateCluster cluster) throws ConfigurationException {
        return new CreateClusterEnvironmentGoogleCloud(client, (CreateClusterGoogleCloud) cluster);
    }

    @Override
    public ValidateIntent getValidateIntent(Configuration config) {
        return null;
    }

    @Override
    public String getBlockDeviceBase() {
        return "/dev/sd";
    }

    @Override
    protected HashMap<String, InstanceType> getInstanceTypeMap(Configuration config) {
        Compute compute = ((ClientGoogleCloud) client).getInternal();
        String projectId = ((ConfigurationGoogleCloud) config).getGoogleProjectId();
        String zone = config.getAvailabilityZone();
        HashMap<String, InstanceType> instanceTypes = new HashMap<>();
        try {
            if (zone == null) {
                for (MachineTypesScopedList scopedList :
                        compute.machineTypes().aggregatedList(projectId).execute().getItems().values()) {
                    for (MachineType f : scopedList.getMachineTypes()) {
                        instanceTypes.put(f.getName(), new InstanceTypeGoogleCloud(f));
                    }
                }
            } else {
                for (MachineType f : compute.machineTypes().list(projectId, zone).execute().getItems()) {
                    instanceTypes.put(f.getName(), new InstanceTypeGoogleCloud(f));
                }
            }
        } catch (Exception ignored) {
        }
        return instanceTypes;
    }
}

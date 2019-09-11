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
    public Client getClient(Configuration config) throws ClientConnectionFailedException {
        return new ClientGoogleCloud((ConfigurationGoogleCloud) config);
    }

    @Override
    public ListIntent getListIntent(Client client, Configuration config) {
        return new ListIntentGoogleCloud(this, client, (ConfigurationGoogleCloud) config);
    }

    @Override
    public TerminateIntent getTerminateIntent(Client client, Configuration config) {
        return new TerminateIntentGoogleCloud(this, client, (ConfigurationGoogleCloud) config);
    }

    @Override
    public PrepareIntent getPrepareIntent(Client client, Configuration config) {
        return new PrepareIntentGoogleCloud(this, client, (ConfigurationGoogleCloud) config);
    }

    @Override
    public CreateCluster getCreateIntent(Client client, Configuration config) {
        return new CreateClusterGoogleCloud(this, client, (ConfigurationGoogleCloud) config);
    }

    @Override
    public CreateClusterEnvironment getClusterEnvironment(Client client, CreateCluster cluster) throws ConfigurationException {
        return new CreateClusterEnvironmentGoogleCloud(client, (CreateClusterGoogleCloud) cluster);
    }

    @Override
    public String getBlockDeviceBase() {
        return "/dev/sd";
    }

    @Override
    protected HashMap<String, InstanceType> getInstanceTypeMap(Client client, Configuration config) {
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

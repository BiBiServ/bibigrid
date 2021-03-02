package de.unibi.cebitec.bibigrid.aws;

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
public class ProviderModuleAWS extends ProviderModule {
    @Override
    public String getName() {
        return "aws";
    }

    @Override
    public Class<? extends Configuration> getConfigurationClass() {
        return ConfigurationAWS.class;
    }

    @Override
    public Validator getValidator(Configuration config, ProviderModule module) throws ConfigurationException {
        return new ValidatorAWS(config,module);
    }

    @Override
    public void createClient(Configuration config) throws ClientConnectionFailedException {
        client = new ClientAWS(config);
    }

    @Override
    public ListIntent getListIntent(Configuration config) {
        return new ListIntentAWS(this, client, config);
    }

    @Override
    public TerminateIntent getTerminateIntent(Configuration config) {
        return new TerminateIntentAWS(this, config);
    }

    @Override
    public PrepareIntent getPrepareIntent(Configuration config) {
        return new PrepareIntentAWS(this, client, config);
    }

    @Override
    public CreateCluster getCreateIntent(Configuration config) {
        return new CreateClusterAWS(this, (ConfigurationAWS) config, clusterId);
    }

    @Override
    public CreateClusterEnvironment getClusterEnvironment(CreateCluster cluster) throws ConfigurationException {
        return new CreateClusterEnvironmentAWS(client, (CreateClusterAWS) cluster);
    }

    @Override
    public ValidateIntent getValidateIntent(Configuration config) {
        return new ValidateIntentAWS(config);
    }

    @Override
    public String getBlockDeviceBase() {
        return "/dev/xvd";
    }

    @Override
    protected Map<String, InstanceType> getInstanceTypeMap(Client client, Configuration config) {
        Map<String, InstanceType> instanceTypes = new HashMap<>();
        for (InstanceType type : InstanceTypeAWS.getStaticInstanceTypeList()) {
            instanceTypes.put(type.getValue(), type);
        }
        return instanceTypes;
    }
}

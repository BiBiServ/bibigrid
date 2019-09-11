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
    public Client getClient(Configuration config) throws ClientConnectionFailedException {
        return new ClientAWS(config);
    }

    @Override
    public ListIntent getListIntent(Client client, Configuration config) {
        return new ListIntentAWS(this, client, config);
    }

    @Override
    public TerminateIntent getTerminateIntent(Client client, Configuration config) {
        return new TerminateIntentAWS(this, client, config);
    }

    @Override
    public PrepareIntent getPrepareIntent(Client client, Configuration config) {
        return new PrepareIntentAWS(this, client, config);
    }

    @Override
    public CreateCluster getCreateIntent(Client client, Configuration config) {
        return new CreateClusterAWS(this, client, (ConfigurationAWS) config);
    }

    @Override
    public CreateClusterEnvironment getClusterEnvironment(Client client, CreateCluster cluster) throws ConfigurationException {
        return new CreateClusterEnvironmentAWS(client, (CreateClusterAWS) cluster);
    }

    @Override
    public ValidateIntent getValidateIntent(Client client, Configuration config) {
        return new ValidateIntentAWS(client, config);
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

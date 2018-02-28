package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.intents.*;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.InstanceType;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import org.apache.commons.cli.CommandLine;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;

import java.util.Map;
import java.util.HashMap;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
@SuppressWarnings("unused")
public class ProviderModuleOpenstack extends ProviderModule {
    @Override
    public String getName() {
        return "openstack";
    }

    @Override
    public CommandLineValidator getCommandLineValidator(final CommandLine commandLine,
                                                        final DefaultPropertiesFile defaultPropertiesFile,
                                                        final IntentMode intentMode) {
        return new CommandLineValidatorOpenstack(commandLine, defaultPropertiesFile, intentMode, this);
    }

    @Override
    public ListIntent getListIntent(Configuration config) {
        return new ListIntentOpenstack(this, (ConfigurationOpenstack) config);
    }

    @Override
    public TerminateIntent getTerminateIntent(Configuration config) {
        return new TerminateIntentOpenstack(this, (ConfigurationOpenstack) config);
    }

    @Override
    public PrepareIntent getPrepareIntent(Configuration config) {
        return new PrepareIntentOpenstack(this, (ConfigurationOpenstack) config);
    }

    @Override
    public CreateCluster getCreateIntent(Configuration config) {
        return new CreateClusterOpenstack(this, (ConfigurationOpenstack) config);
    }

    @Override
    public CreateClusterEnvironment getClusterEnvironment(CreateCluster cluster) throws ConfigurationException {
        return new CreateClusterEnvironmentOpenstack((CreateClusterOpenstack) cluster);
    }

    @Override
    public ValidateIntent getValidateIntent(Configuration config) {
        return new ValidateIntentOpenstack((ConfigurationOpenstack) config);
    }

    @Override
    public String getBlockDeviceBase() {
        return "/dev/vd";
    }

    @Override
    protected Map<String, InstanceType> getInstanceTypeMap(Configuration config) {
        OSClient os = OpenStackUtils.buildOSClient((ConfigurationOpenstack) config);
        Map<String, InstanceType> instanceTypes = new HashMap<>();
        for (Flavor f : os.compute().flavors().list()) {
            instanceTypes.put(f.getName(), new InstanceTypeOpenstack(f));
        }
        return instanceTypes;
    }
}

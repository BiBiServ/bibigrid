package de.unibi.cebitec.bibigrid.aws;

import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.InstanceType;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import org.apache.commons.cli.CommandLine;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
@SuppressWarnings("unused")
public class ProviderModuleAWS implements ProviderModule {
    @Override
    public String getName() {
        return "aws";
    }

    @Override
    public CommandLineValidator getCommandLineValidator(final CommandLine commandLine,
                                                        final DefaultPropertiesFile defaultPropertiesFile,
                                                        final IntentMode intentMode) {
        return new CommandLineValidatorAWS(commandLine, defaultPropertiesFile, intentMode, this);
    }

    @Override
    public ListIntent getListIntent(Configuration config) {
        return new ListIntentAWS(((ConfigurationAWS) config));
    }

    @Override
    public TerminateIntent getTerminateIntent(Configuration config) {
        return new TerminateIntentAWS(((ConfigurationAWS) config));
    }

    @Override
    public CreateCluster getCreateIntent(Configuration config) {
        return new CreateClusterAWS(((ConfigurationAWS) config), this);
    }

    @Override
    public ValidateIntent getValidateIntent(Configuration config) {
        return new ValidateIntentAWS(((ConfigurationAWS) config));
    }

    @Override
    public InstanceType getInstanceType(Configuration config, String type) throws InstanceTypeNotFoundException {
        return InstanceTypeAWS.getByType(type);
    }

    @Override
    public String getBlockDeviceBase() {
        return "/dev/xvd";
    }
}

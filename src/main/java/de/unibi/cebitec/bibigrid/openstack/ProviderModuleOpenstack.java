package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.CommandLineValidator;
import de.unibi.cebitec.bibigrid.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.intents.ListIntent;
import de.unibi.cebitec.bibigrid.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.InstanceType;
import de.unibi.cebitec.bibigrid.model.IntentMode;
import de.unibi.cebitec.bibigrid.model.ProviderModule;
import de.unibi.cebitec.bibigrid.model.exceptions.InstanceTypeNotFoundException;
import de.unibi.cebitec.bibigrid.util.DefaultPropertiesFile;
import org.apache.commons.cli.CommandLine;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
@SuppressWarnings("unused")
public class ProviderModuleOpenstack implements ProviderModule {
    @Override
    public String getName() {
        return "openstack";
    }

    @Override
    public CommandLineValidator getCommandLineValidator(final CommandLine commandLine,
                                                        final DefaultPropertiesFile defaultPropertiesFile,
                                                        final IntentMode intentMode) {
        return new CommandLineValidatorOpenstack(commandLine, defaultPropertiesFile, intentMode);
    }

    @Override
    public ListIntent getListIntent(Configuration config) {
        return new ListIntentOpenstack(config);
    }

    @Override
    public TerminateIntent getTerminateIntent(Configuration config) {
        return new TerminateIntentOpenstack(config);
    }

    @Override
    public CreateCluster getCreateIntent(Configuration config) {
        return new CreateClusterOpenstack(config);
    }

    @Override
    public ValidateIntent getValidateIntent(Configuration config) {
        return null;
    }

    @Override
    public InstanceType getInstanceType(Configuration config, String type) throws InstanceTypeNotFoundException {
        return new InstanceTypeOpenstack(config, type);
    }

    @Override
    public String getBlockDeviceBase() {
        return "/dev/vd";
    }
}

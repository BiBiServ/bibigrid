package de.unibi.cebitec.bibigrid.azure;

import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.core.util.RuleBuilder;
import org.apache.commons.cli.CommandLine;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class CommandLineValidatorAzure extends CommandLineValidator {
    private final ConfigurationAzure azureConfig;

    CommandLineValidatorAzure(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                              final IntentMode intentMode, final ProviderModule providerModule) {
        super(cl, defaultPropertiesFile, intentMode, providerModule);
        azureConfig = (ConfigurationAzure) cfg;
    }

    @Override
    protected Configuration createProviderConfiguration() {
        return new ConfigurationAzure();
    }

    @Override
    protected List<String> getRequiredOptions() {
        switch (intentMode) {
            case LIST:
                return Arrays.asList(RuleBuilder.RuleNames.AZURE_CREDENTIALS_FILE_S.toString());
            case TERMINATE:
                return Arrays.asList(
                        "t",
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString(),
                        RuleBuilder.RuleNames.AZURE_CREDENTIALS_FILE_S.toString());
            case CREATE:
                return Arrays.asList(
                        RuleBuilder.RuleNames.SSH_USER_S.toString(),
                        RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.MASTER_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT_S.toString(),
                        RuleBuilder.RuleNames.KEYPAIR_S.toString(),
                        RuleBuilder.RuleNames.IDENTITY_FILE_S.toString(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString(),
                        RuleBuilder.RuleNames.NFS_SHARES_S.toString(),
                        RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString(),
                        RuleBuilder.RuleNames.AZURE_CREDENTIALS_FILE_S.toString());
            case VALIDATE:
                return Arrays.asList(
                        RuleBuilder.RuleNames.MASTER_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.AZURE_CREDENTIALS_FILE_S.toString());
        }
        return null;
    }

    @Override
    protected boolean validateProviderParameters(List<String> req, Properties defaults) {
        final String shortParamCredentials = RuleBuilder.RuleNames.AZURE_CREDENTIALS_FILE_S.toString();
        final String longParamCredentials = RuleBuilder.RuleNames.AZURE_CREDENTIALS_FILE_L.toString();
        if (cl.hasOption(shortParamCredentials)) {
            azureConfig.setAzureCredentialsFile(cl.getOptionValue(shortParamCredentials));
        } else if (defaults.containsKey(longParamCredentials)) {
            azureConfig.setAzureCredentialsFile(defaults.getProperty(longParamCredentials));
        } else {
            LOG.error("No suitable entry for Azure-Credentials-File (" + shortParamCredentials + ") found! Exit");
            return false;
        }
        return true;
    }
}

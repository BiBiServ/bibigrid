package de.unibi.cebitec.bibigrid.azure;

import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.core.util.RuleBuilder;
import org.apache.commons.cli.CommandLine;

import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class CommandLineValidatorAzure extends CommandLineValidator {
    private final ConfigurationAzure azureConfig;

    CommandLineValidatorAzure(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                              final IntentMode intentMode, final ProviderModule providerModule) {
        super(cl, defaultPropertiesFile, intentMode, providerModule);
        azureConfig = (ConfigurationAzure) config;
    }

    @Override
    protected Class<ConfigurationAzure> getProviderConfigurationClass() {
        return ConfigurationAzure.class;
    }

    @Override
    protected List<String> getRequiredOptions() {
        List<String> options = new ArrayList<>();
        switch (intentMode) {
            default:
                return null;
            case LIST:
                options.add(RuleBuilder.RuleNames.AZURE_CREDENTIALS_FILE.getShortParam());
                break;
            case TERMINATE:
                options.add(IntentMode.TERMINATE.getShortParam());
                options.add(RuleBuilder.RuleNames.REGION.getShortParam());
                options.add(RuleBuilder.RuleNames.AVAILABILITY_ZONE.getShortParam());
                options.add(RuleBuilder.RuleNames.AZURE_CREDENTIALS_FILE.getShortParam());
                break;
            case PREPARE:
            case CREATE:
                options.add(RuleBuilder.RuleNames.SSH_USER.getShortParam());
                options.add(RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE.getShortParam());
                options.add(RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT.getShortParam());
            case VALIDATE:
                options.add(RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE.getShortParam());
                options.add(RuleBuilder.RuleNames.MASTER_IMAGE.getShortParam());
                options.add(RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE.getShortParam());
                options.add(RuleBuilder.RuleNames.SLAVE_IMAGE.getShortParam());
                options.add(RuleBuilder.RuleNames.KEYPAIR.getShortParam());
                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE.getShortParam());
                options.add(RuleBuilder.RuleNames.REGION.getShortParam());
                options.add(RuleBuilder.RuleNames.AVAILABILITY_ZONE.getShortParam());
                options.add(RuleBuilder.RuleNames.AZURE_CREDENTIALS_FILE.getShortParam());
                break;
            case CLOUD9:
                options.add(IntentMode.CLOUD9.getShortParam());
                options.add(RuleBuilder.RuleNames.AZURE_CREDENTIALS_FILE.getShortParam());
                options.add(RuleBuilder.RuleNames.SSH_USER.getShortParam());
                options.add(RuleBuilder.RuleNames.KEYPAIR.getShortParam());
                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE.getShortParam());
                break;
        }
        return options;
    }

    @Override
    protected boolean validateProviderParameters() {
        final String shortParam = RuleBuilder.RuleNames.AZURE_CREDENTIALS_FILE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                azureConfig.setAzureCredentialsFile(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(azureConfig.getAzureCredentialsFile())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            } else if (!FileSystems.getDefault().getPath(azureConfig.getAzureCredentialsFile()).toFile().exists()) {
                LOG.error("Azure credentials file '{}' does not exist!", azureConfig.getAzureCredentialsFile());
                return false;
            }
        }
        return true;
    }
}

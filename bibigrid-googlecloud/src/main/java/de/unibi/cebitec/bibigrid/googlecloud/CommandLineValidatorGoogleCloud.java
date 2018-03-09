package de.unibi.cebitec.bibigrid.googlecloud;

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
public final class CommandLineValidatorGoogleCloud extends CommandLineValidator {
    private final ConfigurationGoogleCloud googleCloudConfig;

    CommandLineValidatorGoogleCloud(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                                    final IntentMode intentMode, final ProviderModule providerModule) {
        super(cl, defaultPropertiesFile, intentMode, providerModule);
        googleCloudConfig = (ConfigurationGoogleCloud) config;
    }

    @Override
    protected Class<ConfigurationGoogleCloud> getProviderConfigurationClass() {
        return ConfigurationGoogleCloud.class;
    }

    @Override
    protected List<String> getRequiredOptions() {
        List<String> options = new ArrayList<>();
        switch (intentMode) {
            default:
                return null;
            case LIST:
                options.add(RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString());
                options.add(RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString());
                break;
            case TERMINATE:
                options.add(IntentMode.TERMINATE.getShortParam());
                options.add(RuleBuilder.RuleNames.REGION_S.toString());
                options.add(RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString());
                options.add(RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString());
                options.add(RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString());
                break;
            case PREPARE:
            case CREATE:
                options.add(RuleBuilder.RuleNames.SSH_USER_S.toString());
                options.add(RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString());
                options.add(RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT_S.toString());
            case VALIDATE:
                options.add(RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE_S.toString());
                options.add(RuleBuilder.RuleNames.MASTER_IMAGE_S.toString());
                options.add(RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE_S.toString());
                options.add(RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString());
                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE_S.toString());
                options.add(RuleBuilder.RuleNames.REGION_S.toString());
                options.add(RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString());
                options.add(RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString());
                options.add(RuleBuilder.RuleNames.GOOGLE_IMAGE_PROJECT_ID_S.toString());
                options.add(RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString());
                break;
        }
        return options;
    }

    @Override
    protected boolean validateProviderParameters() {
        return parseGoogleProjectIdParameter() &&
                parseGoogleImageProjectIdParameter() &&
                parseGoogleCredentialsFileParameter();
    }

    private boolean parseGoogleProjectIdParameter() {
        final String shortParam = RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                googleCloudConfig.setGoogleProjectId(value);
            }
        }
        return checkRequiredParameter(shortParam, googleCloudConfig.getGoogleProjectId());
    }

    private boolean parseGoogleImageProjectIdParameter() {
        final String shortParam = RuleBuilder.RuleNames.GOOGLE_IMAGE_PROJECT_ID_S.toString();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                googleCloudConfig.setGoogleImageProjectId(value);
            }
        }
        return checkRequiredParameter(shortParam, googleCloudConfig.getGoogleImageProjectId());
    }

    private boolean parseGoogleCredentialsFileParameter() {
        final String shortParam = RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                googleCloudConfig.setGoogleCredentialsFile(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(googleCloudConfig.getGoogleCredentialsFile())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            } else if (!FileSystems.getDefault().getPath(googleCloudConfig.getGoogleCredentialsFile()).toFile().exists()) {
                LOG.error("Google credentials file '{}' does not exist!", googleCloudConfig.getGoogleCredentialsFile());
                return false;
            }
        }
        return true;
    }
}

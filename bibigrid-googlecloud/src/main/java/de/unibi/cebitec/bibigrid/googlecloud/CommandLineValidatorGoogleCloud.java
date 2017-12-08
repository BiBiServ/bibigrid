package de.unibi.cebitec.bibigrid.googlecloud;

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
public final class CommandLineValidatorGoogleCloud extends CommandLineValidator {
    private final ConfigurationGoogleCloud googleCloudConfig;

    CommandLineValidatorGoogleCloud(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                                    final IntentMode intentMode, final ProviderModule providerModule) {
        super(cl, defaultPropertiesFile, intentMode, providerModule);
        googleCloudConfig = (ConfigurationGoogleCloud) cfg;
    }

    @Override
    protected Configuration createProviderConfiguration() {
        return new ConfigurationGoogleCloud();
    }

    @Override
    protected List<String> getRequiredOptions() {
        switch (intentMode) {
            case LIST:
                return Arrays.asList(
                        RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString(),
                        RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString());
            case TERMINATE:
                return Arrays.asList(
                        "t",
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString(),
                        RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString(),
                        RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString());
            case CREATE:
                return Arrays.asList(
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
                        RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString(),
                        RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString());
            case VALIDATE:
                return Arrays.asList(
                        RuleBuilder.RuleNames.MASTER_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString(),
                        RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString());
        }
        return null;
    }

    @Override
    protected boolean validateProviderParameters(List<String> req, Properties defaults) {
        final String shortParamProject = RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_S.toString();
        final String longParamProject = RuleBuilder.RuleNames.GOOGLE_PROJECT_ID_L.toString();
        if (cl.hasOption(shortParamProject)) {  // Google Cloud - required
            googleCloudConfig.setGoogleProjectId(cl.getOptionValue(shortParamProject).trim());
        } else if (defaults.containsKey(longParamProject)) {
            googleCloudConfig.setGoogleProjectId(defaults.getProperty(longParamProject));
        } else {
            LOG.error("No suitable entry for Google-ProjectId (" + shortParamProject + ") found! Exit");
            return false;
        }

        final String shortParamCredentials = RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString();
        final String longParamCredentials = RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_L.toString();
        if (cl.hasOption(shortParamCredentials)) {  // Google Cloud - required
            googleCloudConfig.setGoogleCredentialsFile(cl.getOptionValue(shortParamCredentials));
        } else if (defaults.containsKey(longParamCredentials)) {
            googleCloudConfig.setGoogleCredentialsFile(defaults.getProperty(longParamCredentials));
        } else {
            LOG.error("No suitable entry for Google-Credentials-File (" + shortParamCredentials + ") found! Exit");
            return false;
        }
        return true;
    }
}

package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.CommandLineValidator;
import de.unibi.cebitec.bibigrid.model.IntentMode;
import de.unibi.cebitec.bibigrid.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.util.RuleBuilder;
import org.apache.commons.cli.CommandLine;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class CommandLineValidatorGoogleCloud extends CommandLineValidator {
    CommandLineValidatorGoogleCloud(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                                    final IntentMode intentMode) {
        super(cl, defaultPropertiesFile, intentMode);
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
            cfg.setGoogleProjectId(cl.getOptionValue(shortParamProject).trim());
        } else if (defaults.containsKey(longParamProject)) {
            cfg.setGoogleProjectId(defaults.getProperty(longParamProject));
        } else {
            LOG.error("No suitable entry for Google-ProjectId (" + shortParamProject + ") found! Exit");
            return false;
        }

        final String shortParamCredentials = RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_S.toString();
        final String longParamCredentials = RuleBuilder.RuleNames.GOOGLE_CREDENTIALS_FILE_L.toString();
        if (cl.hasOption(shortParamCredentials)) {  // Google Cloud - required
            cfg.setGoogleCredentialsFile(cl.getOptionValue(shortParamCredentials));
        } else if (defaults.containsKey(longParamCredentials)) {
            cfg.setGoogleCredentialsFile(defaults.getProperty(longParamCredentials));
        } else {
            LOG.error("No suitable entry for Google-Credentials-File (" + shortParamCredentials + ") found! Exit");
            return false;
        }
        return true;
    }
}

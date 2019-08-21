package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.ConfigurationFile;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de, jkrueger(at)cebitec.uni-bielefeld.de
 *
 * @ToDo: add missing checks for
 */
public final class ValidatorGoogleCloud extends Validator {
    private final ConfigurationGoogleCloud googleCloudConfig;

    ValidatorGoogleCloud(final CommandLine cl, final ConfigurationFile configurationFile,
                         final IntentMode intentMode, final ProviderModule providerModule)
            throws ConfigurationException {
        super(cl, configurationFile, intentMode, providerModule);
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
                // @ToDo: Check for config properties CREDENTIALS_FILE and GOOGLE_PROJECT_ID
                break;
            case TERMINATE:
                options.add(IntentMode.TERMINATE.getShortParam());
                // @ToDo: Check for config properties REGION, AVAILABILITY_ZONE, CREDENTIALS_FILE and GOOGLE_PROJECT_ID
                break;
            case PREPARE:
            case CREATE:
                // @ToDo: Check for config properties USE_MASTER_AS_COMPUTE and WORKER_INSTANCE_COUNT;
            case VALIDATE:
                //@ToDo: Check for the following config properties:
                // MASTER_INSTANCE_TYPE, MASTER_IMAGE
                // WORKER_INSTANCE_TYPE, WORKER_IMAGE
                // SSH_USER, SSH_PRIVATE_KEY_FILE, REGION, AVAILABILITY_ZONE
                // CREDENTIALS_FILE
                // GOOGLE_IMAGE_PROJECT_ID, GOOGLE_PROJECT_ID
                break;
            case CLOUD9:
                options.add(IntentMode.CLOUD9.getShortParam());
                //@ToDo: Check for CREDENTIALS_FILE, GOOGLE_PROJECT_ID, SSH_USER and SSH_PRIVATE_KEY_FILE
                break;
        }
        return options;
    }

    @Override
    protected boolean validateProviderParameters() {
        return parseGoogleProjectIdParameter() && parseGoogleImageProjectIdParameter();
    }

    private boolean parseGoogleProjectIdParameter() {
        return !isStringNullOrEmpty(googleCloudConfig.getGoogleProjectId());
    }

    private boolean parseGoogleImageProjectIdParameter() {
        return !isStringNullOrEmpty(googleCloudConfig.getGoogleImageProjectId());
    }
}

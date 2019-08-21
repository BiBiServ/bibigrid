package de.unibi.cebitec.bibigrid.aws;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.ConfigurationFile;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.List;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * AWS specific implementation for the {@link Validator}.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de, jkrueger(at)cebitec.uni-bielefeld.de
 */
public final class ValidatorAWS extends Validator {
    private final ConfigurationAWS awsConfig;

    ValidatorAWS(final CommandLine cl, final ConfigurationFile configurationFile,
                 final IntentMode intentMode, final ProviderModule providerModule)
            throws ConfigurationException {
        super(cl, configurationFile, intentMode, providerModule);
        awsConfig = (ConfigurationAWS) config;
    }

    @Override
    protected Class<ConfigurationAWS> getProviderConfigurationClass() {
        return ConfigurationAWS.class;
    }

    @Override
    protected List<String> getRequiredOptions() {
        List<String> options = new ArrayList<>();
        switch (intentMode) {
            default:
                return null;
            case LIST:
//                options.add(RuleBuilder.RuleNames.KEYPAIR.getShortParam());
//                options.add(RuleBuilder.RuleNames.REGION.getShortParam());
//                options.add(RuleBuilder.RuleNames.CREDENTIALS_FILE.getShortParam());
                break;
            case TERMINATE:
                options.add(IntentMode.TERMINATE.getShortParam());
//                options.add(RuleBuilder.RuleNames.REGION.getShortParam());
//                options.add(RuleBuilder.RuleNames.CREDENTIALS_FILE.getShortParam());
                break;
            case PREPARE:
            case CREATE:
//                options.add(RuleBuilder.RuleNames.SSH_USER.getShortParam());
//                options.add(RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE.getShortParam());
//                options.add(RuleBuilder.RuleNames.WORKER_INSTANCE_COUNT.getShortParam());
            case VALIDATE:
//                options.add(RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE.getShortParam());
//                options.add(RuleBuilder.RuleNames.MASTER_IMAGE.getShortParam());
//                options.add(RuleBuilder.RuleNames.WORKER_INSTANCE_TYPE.getShortParam());
//                options.add(RuleBuilder.RuleNames.WORKER_IMAGE.getShortParam());
//                options.add(RuleBuilder.RuleNames.KEYPAIR.getShortParam());
//                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE.getShortParam());
//                options.add(RuleBuilder.RuleNames.REGION.getShortParam());
//                options.add(RuleBuilder.RuleNames.AVAILABILITY_ZONE.getShortParam());
//                options.add(RuleBuilder.RuleNames.CREDENTIALS_FILE.getShortParam());
                break;
            case CLOUD9:
                options.add(IntentMode.CLOUD9.getShortParam());
//                options.add(RuleBuilder.RuleNames.CREDENTIALS_FILE.getShortParam());
//                options.add(RuleBuilder.RuleNames.SSH_USER.getShortParam());
//                options.add(RuleBuilder.RuleNames.KEYPAIR.getShortParam());
//                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE.getShortParam());
                break;
        }
        return options;
    }

    @Override
    protected boolean validateProviderParameters() {
        return true;
    }

}

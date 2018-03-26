package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.core.util.RuleBuilder;
import org.apache.commons.cli.CommandLine;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * AWS specific implementation for the {@link CommandLineValidator}.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class CommandLineValidatorAWS extends CommandLineValidator {
    private final ConfigurationAWS awsConfig;

    CommandLineValidatorAWS(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                            final IntentMode intentMode, final ProviderModule providerModule) {
        super(cl, defaultPropertiesFile, intentMode, providerModule);
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
                options.add(RuleBuilder.RuleNames.KEYPAIR.getShortParam());
                options.add(RuleBuilder.RuleNames.REGION.getShortParam());
                options.add(RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE.getShortParam());
                break;
            case TERMINATE:
                options.add(IntentMode.TERMINATE.getShortParam());
                options.add(RuleBuilder.RuleNames.REGION.getShortParam());
                options.add(RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE.getShortParam());
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
                options.add(RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE.getShortParam());
                break;
            case CLOUD9:
                options.add(IntentMode.CLOUD9.getShortParam());
                options.add(RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE.getShortParam());
                options.add(RuleBuilder.RuleNames.SSH_USER.getShortParam());
                options.add(RuleBuilder.RuleNames.KEYPAIR.getShortParam());
                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE.getShortParam());
                break;
        }
        return options;
    }

    @Override
    protected boolean validateProviderParameters() {
        return parseAwsCredentialsFileParameter() &&
                parsePublicSlaveIpParameter() &&
                parseSpotInstanceParameters();
    }

    private boolean parseAwsCredentialsFileParameter() {
        final String shortParam = RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                awsConfig.setAwsCredentialsFile(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(awsConfig.getAwsCredentialsFile())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            } else if (!FileSystems.getDefault().getPath(awsConfig.getAwsCredentialsFile()).toFile().exists()) {
                LOG.error("AWS credentials file '{}' does not exist!", awsConfig.getAwsCredentialsFile());
                return false;
            }
        }
        try {
            AWSCredentials keys = new PropertiesCredentials(
                    FileSystems.getDefault().getPath(awsConfig.getAwsCredentialsFile()).toFile());
            awsConfig.setCredentials(keys);
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("AWS credentials file could not be loaded: {}", e.getMessage());
            return false;
        }
        return true;
    }

    private boolean parsePublicSlaveIpParameter() {
        Boolean parseResult = parseBooleanParameter(RuleBuilder.RuleNames.PUBLIC_SLAVE_IP);
        if (parseResult != null) {
            awsConfig.setPublicSlaveIps(parseResult);
        }
        return true;
    }

    private boolean parseSpotInstanceParameters() {
        final String spotShortParam = RuleBuilder.RuleNames.USE_SPOT_INSTANCE_REQUEST.getShortParam();
        if (cl.hasOption(spotShortParam)) {
            final String value = cl.getOptionValue(spotShortParam);
            if (value.equalsIgnoreCase("yes")) { // TODO: keyword
                config.setUseSpotInstances(true);
                String bidPriceShortParam = RuleBuilder.RuleNames.BID_PRICE.getShortParam();
                if (cl.hasOption(bidPriceShortParam)) {
                    try {
                        awsConfig.setBidPrice(Double.parseDouble(cl.getOptionValue(bidPriceShortParam)));
                        if (awsConfig.getBidPrice() <= 0.0) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        LOG.error("Argument bp/bidprice is not a valid double value  and must be > 0.0 !");
                        return false;
                    }
                } else {
                    LOG.error("If use-spot-instance-request is set, a bidprice must defined!");
                    return false;
                }
                String bidPriceMasterShortParam = RuleBuilder.RuleNames.BID_PRICE_MASTER.getShortParam();
                if (cl.hasOption(bidPriceMasterShortParam)) {
                    try {
                        awsConfig.setBidPriceMaster(Double.parseDouble(cl.getOptionValue(bidPriceMasterShortParam)));
                        if (awsConfig.getBidPriceMaster() <= 0.0) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        LOG.error("Argument bpm/bidprice-master is not a valid double value and must be > 0.0 !");
                        return false;
                    }
                } else {
                    LOG.info(V, "Bidprice master is not set, use general bidprice instead!");
                }
                LOG.info(V, "Use spot request for all");
            } else if (value.equalsIgnoreCase("no")) { // TODO: keyword
                LOG.info(V, "SpotInstance usage disabled.");
                this.config.setMesos(false);
            } else {
                LOG.error("SpotInstanceRequest value not recognized. Please use yes/no.");
                return false;
            }
        }
        return true;
    }
}

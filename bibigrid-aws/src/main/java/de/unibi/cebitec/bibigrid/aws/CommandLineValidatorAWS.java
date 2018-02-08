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
import java.util.Arrays;
import java.util.List;

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
        switch (intentMode) {
            case LIST:
                return Arrays.asList(
                        RuleBuilder.RuleNames.KEYPAIR_S.toString(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_S.toString());
            case TERMINATE:
                return Arrays.asList(
                        "t",
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_S.toString());
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
                        RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString(),
                        RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_S.toString());
            case VALIDATE:
                return Arrays.asList(
                        RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.MASTER_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString(),
                        RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT_S.toString(),
                        RuleBuilder.RuleNames.USER_S.toString(),
                        RuleBuilder.RuleNames.KEYPAIR_S.toString(),
                        RuleBuilder.RuleNames.IDENTITY_FILE_S.toString(),
                        RuleBuilder.RuleNames.REGION_S.toString(),
                        RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString(),
                        RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString(),
                        RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_S.toString());
        }
        return null;
    }

    @Override
    protected boolean validateProviderParameters() {
        return parseAwsCredentialsFileParameter() &&
                parsePublicSlaveIpParameter() &&
                parseSpotInstanceParameters();
    }

    private boolean parseAwsCredentialsFileParameter() {
        final String shortParam = RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_S.toString();
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
        if (cl.hasOption(RuleBuilder.RuleNames.PUBLIC_SLAVE_IP_S.toString())) {
            awsConfig.setPublicSlaveIps(true);
        }
        return true;
    }

    private boolean parseSpotInstanceParameters() {
        /* TODO
        final String spotShortParam = RuleBuilder.RuleNames.USE_SPOT_INSTANCE_REQUEST_S.toString();
        if (cl.hasOption(spotShortParam) || defaults.containsKey(spotLongParam)) {
            String value = parseParameterOrDefault(defaults, spotShortParam, spotLongParam);
            if (value.equalsIgnoreCase(KEYWORD_YES)) {
                config.setUseSpotInstances(true);
                String bidPriceShortParam = RuleBuilder.RuleNames.BID_PRICE_S.toString();
                if (cl.hasOption(bidPriceShortParam) || defaults.containsKey(bidPriceLongParam)) {
                    try {
                        awsConfig.setBidPrice(Double.parseDouble(
                                parseParameterOrDefault(defaults, bidPriceShortParam, bidPriceLongParam)));
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
                String bidPriceMasterShortParam = RuleBuilder.RuleNames.BID_PRICE_MASTER_S.toString();
                if (cl.hasOption(bidPriceMasterShortParam) || defaults.containsKey(bidPriceMasterLongParam)) {
                    try {
                        awsConfig.setBidPriceMaster(Double.parseDouble(
                                parseParameterOrDefault(defaults, bidPriceMasterShortParam, bidPriceMasterLongParam)));
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
            } else if (value.equalsIgnoreCase(KEYWORD_NO)) {
                LOG.info(V, "SpotInstance usage disabled.");
                this.config.setMesos(false);
            } else {
                LOG.error("SpotInstanceRequest value not recognized. Please use yes/no.");
                return false;
            }
        }
        */
        return true;
    }
}

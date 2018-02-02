package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.core.util.RuleBuilder;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
        awsConfig = (ConfigurationAWS) cfg;
    }

    @Override
    protected Configuration createProviderConfiguration() {
        return new ConfigurationAWS();
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
                        RuleBuilder.RuleNames.NFS_SHARES_S.toString(),
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
                        RuleBuilder.RuleNames.NFS_SHARES_S.toString(),
                        RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString(),
                        RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_S.toString());
        }
        return null;
    }

    @Override
    protected boolean validateProviderParameters(List<String> req, Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_S.toString();
        final String longParam = RuleBuilder.RuleNames.AWS_CREDENTIALS_FILE_L.toString();
        if (req.contains(shortParam)) {
            String awsCredentialsFilePath = null;
            if (defaults.containsKey(longParam)) {
                awsCredentialsFilePath = defaults.getProperty(longParam);
            }
            if (cl.hasOption(shortParam)) {
                awsCredentialsFilePath = cl.getOptionValue(shortParam);
            }
            if (awsCredentialsFilePath == null) {
                if (Files.exists(defaultPropertiesFile.getPropertiesFilePath())) {
                    awsCredentialsFilePath = defaultPropertiesFile.getPropertiesFilePath().toString();
                } else {
                    LOG.error("Default credentials file not found! ({})", defaultPropertiesFile.getPropertiesFilePath());
                    LOG.error("-" + shortParam + " option is required! Please specify the properties file " +
                            "containing the aws credentials.");
                    return false;
                }
            }
            File credentialsFile = new File(awsCredentialsFilePath.trim());
            try {
                AWSCredentials keys = new PropertiesCredentials(credentialsFile);
                ((ConfigurationAWS) cfg).setCredentials(keys);
                LOG.info(V, "AWS-Credentials successfully loaded! ({})", awsCredentialsFilePath.trim());
            } catch (IOException | IllegalArgumentException e) {
                LOG.error("AWS-Credentials from properties: {}", e.getMessage());
                return false;
            }
        }
        if (!parsePublicSlaveIpParameter(defaults)) return false;
        if (!parseSpotInstanceParameters(defaults)) return false;
        return true;
    }

    private boolean parsePublicSlaveIpParameter(Properties defaults) {
        final String shortParam = RuleBuilder.RuleNames.PUBLIC_SLAVE_IP_S.toString();
        final String longParam = RuleBuilder.RuleNames.PUBLIC_SLAVE_IP_L.toString();
        // public ip address for all slaves
        if (cl.hasOption(shortParam) || defaults.containsKey(longParam)) {
            awsConfig.setPublicSlaveIps(
                    parseParameterOrDefault(defaults, shortParam, longParam).equalsIgnoreCase(KEYWORD_YES));
        }
        return true;
    }

    private boolean parseSpotInstanceParameters(Properties defaults) {
        final String spotShortParam = RuleBuilder.RuleNames.USE_SPOT_INSTANCE_REQUEST_S.toString();
        final String spotLongParam = RuleBuilder.RuleNames.USE_SPOT_INSTANCE_REQUEST_L.toString();
        if (cl.hasOption(spotShortParam) || defaults.containsKey(spotLongParam)) {
            String value = parseParameterOrDefault(defaults, spotShortParam, spotLongParam);
            if (value.equalsIgnoreCase(KEYWORD_YES)) {
                cfg.setUseSpotInstances(true);
                String bidPriceShortParam = RuleBuilder.RuleNames.BID_PRICE_S.toString();
                String bidPriceLongParam = RuleBuilder.RuleNames.BID_PRICE_L.toString();
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
                String bidPriceMasterLongParam = RuleBuilder.RuleNames.BID_PRICE_MASTER_L.toString();
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
                this.cfg.setMesos(false);
            } else {
                LOG.error("SpotInstanceRequest value not recognized. Please use yes/no.");
                return false;
            }
        }
        return true;
    }
}

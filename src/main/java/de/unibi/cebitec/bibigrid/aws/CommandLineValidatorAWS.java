package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import de.unibi.cebitec.bibigrid.CommandLineValidator;
import de.unibi.cebitec.bibigrid.model.IntentMode;
import de.unibi.cebitec.bibigrid.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.util.RuleBuilder;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;

/**
 * AWS specific implementation for the {@link CommandLineValidator}.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class CommandLineValidatorAWS extends CommandLineValidator {
    CommandLineValidatorAWS(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                            final IntentMode intentMode) {
        super(cl, defaultPropertiesFile, intentMode);
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
            File credentialsFile = new File(awsCredentialsFilePath);
            try {
                AWSCredentials keys = new PropertiesCredentials(credentialsFile);
                cfg.setCredentials(keys);
                LOG.info(V, "AWS-Credentials successfully loaded! ({})", awsCredentialsFilePath);
            } catch (IOException | IllegalArgumentException e) {
                LOG.error("AWS-Credentials from properties: {}", e.getMessage());
                return false;
            }
        }
        return true;
    }
}

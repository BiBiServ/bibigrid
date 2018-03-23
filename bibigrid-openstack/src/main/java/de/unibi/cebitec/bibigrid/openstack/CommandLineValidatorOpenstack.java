package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.core.util.RuleBuilder;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class CommandLineValidatorOpenstack extends CommandLineValidator {
    private final ConfigurationOpenstack openstackConfig;

    CommandLineValidatorOpenstack(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                                  final IntentMode intentMode, final ProviderModule providerModule) {
        super(cl, defaultPropertiesFile, intentMode, providerModule);
        openstackConfig = (ConfigurationOpenstack) config;
    }

    @Override
    protected Class<ConfigurationOpenstack> getProviderConfigurationClass() {
        return ConfigurationOpenstack.class;
    }

    @Override
    protected List<String> getRequiredOptions() {
        List<String> options = new ArrayList<>();
        switch (intentMode) {
            default:
                return null;
            case LIST:
                options.add(RuleBuilder.RuleNames.REGION_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
                break;
            case TERMINATE:
                options.add(IntentMode.TERMINATE.getShortParam());
                options.add(RuleBuilder.RuleNames.REGION_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
                break;
            case PREPARE:
            case CREATE:
                options.add(RuleBuilder.RuleNames.SSH_USER_S.toString());
                options.add(RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE_S.toString());
                options.add(RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT_S.toString());
                options.add(RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE_S.toString());
                options.add(RuleBuilder.RuleNames.MASTER_IMAGE_S.toString());
                options.add(RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE_S.toString());
                options.add(RuleBuilder.RuleNames.SLAVE_IMAGE_S.toString());
                options.add(RuleBuilder.RuleNames.KEYPAIR_S.toString());
                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE_S.toString());
                options.add(RuleBuilder.RuleNames.REGION_S.toString());
                options.add(RuleBuilder.RuleNames.AVAILABILITY_ZONE_S.toString());
            case VALIDATE:
                options.add(RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
                break;
            case CLOUD9:
                options.add(IntentMode.CLOUD9.getShortParam());
                options.add(RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString());
                options.add(RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString());
                options.add(RuleBuilder.RuleNames.SSH_USER_S.toString());
                options.add(RuleBuilder.RuleNames.KEYPAIR_S.toString());
                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE_S.toString());
                break;
        }
        return options;
    }

    @Override
    protected boolean validateProviderParameters() {
        return parseCredentialParameters() &&
                parseRouterParameter() &&
                parseSecurityGroupParameter();
    }

    private boolean parseCredentialParameters() {
        if (openstackConfig.getOpenstackCredentials() == null) {
            openstackConfig.setOpenstackCredentials(new OpenStackCredentials());
        }
        OpenStackCredentials credentials = openstackConfig.getOpenstackCredentials();
        return parseCredentialsUsernameParameter(credentials) &&
                parseCredentialsDomainParameter(credentials) &&
                parseCredentialsTenantNameParameter(credentials) &&
                parseCredentialsTenantDomainParameter(credentials) &&
                parseCredentialsPasswordParameter(credentials) &&
                parseCredentialsEndpointParameter(credentials);
    }

    private boolean parseCredentialsUsernameParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_USERNAME_S.toString();
        String envParam = RuleBuilder.RuleNames.OPENSTACK_USERNAME_ENV.toString();
        // Parse environment variable if not loaded from config file
        if (isStringNullOrEmpty(credentials.getUsername()) && !isStringNullOrEmpty(System.getenv(envParam))) {
            credentials.setUsername(System.getenv(envParam));
        }
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setUsername(value);
            }
        }
        return checkRequiredParameter(shortParam, credentials.getUsername());
    }

    private boolean parseCredentialsDomainParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_DOMAIN_S.toString();
        String envParam = RuleBuilder.RuleNames.OPENSTACK_DOMAIN_ENV.toString();
        // Parse environment variable if not loaded from config file
        if (isStringNullOrEmpty(credentials.getDomain()) && !isStringNullOrEmpty(System.getenv(envParam))) {
            credentials.setDomain(System.getenv(envParam));
        }
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setDomain(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(credentials.getDomain())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        if (isStringNullOrEmpty(credentials.getDomain())) {
            LOG.info("Keystone V2 API.");
        }
        return true;
    }

    private boolean parseCredentialsTenantNameParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_S.toString();
        String envParam = RuleBuilder.RuleNames.OPENSTACK_TENANT_NAME_ENV.toString();
        // Parse environment variable if not loaded from config file
        if (isStringNullOrEmpty(credentials.getTenantName()) && !isStringNullOrEmpty(System.getenv(envParam))) {
            credentials.setTenantName(System.getenv(envParam));
        }
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setTenantName(value);
            }
        }
        return checkRequiredParameter(shortParam, credentials.getTenantName());
    }

    private boolean parseCredentialsTenantDomainParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_TENANT_DOMAIN_S.toString();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setTenantDomain(value);
            }
        }
        // Validate parameter if required
        if (req.contains(shortParam)) {
            if (isStringNullOrEmpty(credentials.getTenantDomain())) {
                LOG.error("-" + shortParam + " option is required!");
                return false;
            }
        }
        if (isStringNullOrEmpty(credentials.getTenantDomain())) {
            LOG.info("OpenStack TenantDomain is empty! Use OpenStack Domain instead!");
            credentials.setTenantDomain(credentials.getDomain());
        }
        return true;
    }

    private boolean parseCredentialsPasswordParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_PASSWORD_S.toString();
        String envParam = RuleBuilder.RuleNames.OPENSTACK_PASSWORD_ENV.toString();
        // Parse environment variable if not loaded from config file
        if (isStringNullOrEmpty(credentials.getPassword()) && !isStringNullOrEmpty(System.getenv(envParam))) {
            credentials.setPassword(System.getenv(envParam));
        }
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setPassword(value);
            }
        }
        return checkRequiredParameter(shortParam, credentials.getPassword());
    }

    private boolean parseCredentialsEndpointParameter(OpenStackCredentials credentials) {
        String shortParam = RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_S.toString();
        String envParam = RuleBuilder.RuleNames.OPENSTACK_ENDPOINT_ENV.toString();
        // Parse environment variable if not loaded from config file
        if (isStringNullOrEmpty(credentials.getEndpoint()) && !isStringNullOrEmpty(System.getenv(envParam))) {
            credentials.setEndpoint(System.getenv(envParam));
        }
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                credentials.setEndpoint(value);
            }
        }
        return checkRequiredParameter(shortParam, credentials.getEndpoint());
    }

    private boolean parseRouterParameter() {
        final String shortParam = RuleBuilder.RuleNames.ROUTER_S.toString();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                openstackConfig.setRouter(value);
            }
        }
        return checkRequiredParameter(shortParam, openstackConfig.getRouter());
    }

    private boolean parseSecurityGroupParameter() {
        final String shortParam = RuleBuilder.RuleNames.SECURITY_GROUP_S.toString();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                openstackConfig.setSecurityGroup(value);
            }
        }
        return checkRequiredParameter(shortParam, openstackConfig.getSecurityGroup());
    }
}

package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Openstack specific implementation for a validator
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de, t.dilger(at)uni-bielefeld.de, jkrueger(at)cebitec.uni-bielefeld.de
 */
public final class ValidatorOpenstack extends Validator {
    private final ConfigurationOpenstack openstackConfig;

    private enum EnvCredentials {
        OS_PROJECT_NAME,
        OS_USER_DOMAIN_NAME,
        OS_USER_DOMAIN_ID,
        OS_PROJECT_DOMAIN_NAME,
        OS_PROJECT_DOMAIN_ID,
        OS_AUTH_URL,
        OS_PASSWORD,
        OS_USERNAME
    }

    ValidatorOpenstack(final Configuration config, final ProviderModule providerModule)
            throws ConfigurationException {
        super( config, providerModule);
        openstackConfig = (ConfigurationOpenstack) config;
    }

    @Override
    protected Class<ConfigurationOpenstack> getProviderConfigurationClass() {
        return ConfigurationOpenstack.class;
    }

    @Override
    protected List<String> getRequiredOptions() {
        return null;
    }

    /**
     * Loads credentials.yml or environment variables set via sourced RC file.
     * @return true, if credentials loaded successfully
     */
    @Override
    public boolean validateProviderParameters() {
        OpenStackCredentials openStackCredentials;
        String path = config.getCredentialsFile();
        if (path != null) {
            openStackCredentials = loadCredentialsFile(path);
        } else {
            LOG.info("No credentials file provided. Checking environment variables ...");
            openStackCredentials = loadEnvCredentials();
        }
        if (openStackCredentials == null) {
            LOG.error("No or incomplete credentials provided. Please use a credentials file or source the OpenStack RC file.");
            return false;
        }
        LOG.info("Set OpenStack Credentials ...");
        openstackConfig.setOpenstackCredentials(openStackCredentials);
        return true;
    }

    /**
     * Loads credentials file and checks, if every parameter has been set.
     * @return openStackCredentials if parameters given, otherwise null
     */
    private OpenStackCredentials loadCredentialsFile(String path) {
        try {
            File credentialsFile = Paths.get(path).toFile();
            OpenStackCredentials openStackCredentials =  new Yaml().loadAs(new FileInputStream(credentialsFile), OpenStackCredentials.class);
            LOG.info("Found valid credentials file.");
            if (openStackCredentials.getProjectName() == null) {
                LOG.error("Openstack credentials : Missing 'projectName' parameter!");
                return null;
            }
            if (openStackCredentials.getDomain() == null) {
                LOG.error("Openstack credentials : Missing 'domain' parameter!");
                return null;
            }
            if (openStackCredentials.getProjectDomain() == null) {
                LOG.error("Openstack credentials : Missing 'projectDomain' parameter!");
                return null;
            }
            if (openStackCredentials.getEndpoint() == null) {
                LOG.error("Openstack credentials : Missing 'endpoint' parameter!");
                return null;
            }
            if (openStackCredentials.getUsername() == null) {
                LOG.error("Openstack credentials : Missing 'username' parameter!");
                return null;
            }
            if (openStackCredentials.getPassword() == null) {
                LOG.error("Openstack credentials : Missing 'password' parameter!");
                return null;
            }
            return openStackCredentials;
        } catch (FileNotFoundException e) {
            LOG.error("Failed to locate openstack credentials file.", e);
            return null;
        } catch (YAMLException e) {
            LOG.error("Failed to parse openstack credentials file. {}",
                    e.getCause() != null ? e.getCause().getMessage() : e);
            return null;
        }
    }

    /**
     * Loads environment variables.
     * @return openStackCredentials if parameters given, otherwise null
     */
    private OpenStackCredentials loadEnvCredentials() {
        Map env =  System.getenv();
        OpenStackCredentials openStackCredentials = new OpenStackCredentials();

        String project_name = EnvCredentials.OS_PROJECT_NAME.name();
        if (!env.containsKey(project_name)) {
            LOG.error("Missing Project name in Environment credentials.");
            return null;
        }
        openStackCredentials.setProjectName((String)env.get(project_name));
        String user_domain;
        if (env.containsKey(EnvCredentials.OS_USER_DOMAIN_NAME.name())) {
            user_domain = (String)env.get(EnvCredentials.OS_USER_DOMAIN_NAME.name());
        } else if (env.containsKey(EnvCredentials.OS_USER_DOMAIN_ID.name())) {
            user_domain = (String)env.get(EnvCredentials.OS_USER_DOMAIN_ID.name());
        } else {
            LOG.error("Missing User Domain in Environment credentials.");
            return null;
        }

        String project_domain;
        if (env.containsKey(EnvCredentials.OS_PROJECT_DOMAIN_NAME.name())) {
            project_domain = (String)env.get(EnvCredentials.OS_PROJECT_DOMAIN_NAME.name());
        } else if (env.containsKey(EnvCredentials.OS_PROJECT_DOMAIN_ID.name())) {
           project_domain = (String)env.get(EnvCredentials.OS_PROJECT_DOMAIN_ID.name());
        } else {
            LOG.info("Project Domain name not set. Use User Domain instead ...");
            project_domain = user_domain;
        }
        openStackCredentials.setDomain(user_domain);
        openStackCredentials.setProjectDomain(project_domain);

        if (!(env.containsKey(EnvCredentials.OS_AUTH_URL.name())
                && env.containsKey(EnvCredentials.OS_PASSWORD.name())
                && env.containsKey(EnvCredentials.OS_USERNAME.name()))) {
            LOG.error("Missing Environment credentials.");
            return null;
        }
        openStackCredentials.setEndpoint((String)env.get(EnvCredentials.OS_AUTH_URL.name()));
        openStackCredentials.setPassword((String)env.get(EnvCredentials.OS_PASSWORD.name()));
        openStackCredentials.setUsername((String)env.get(EnvCredentials.OS_USERNAME.name()));
        return openStackCredentials;
    }

}

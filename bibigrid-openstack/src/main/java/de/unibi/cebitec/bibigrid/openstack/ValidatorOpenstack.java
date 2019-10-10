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

    private static class EnvCredentials {
        private static String PROJECT_NAME = "OS_PROJECT_NAME";
        private static String USER_DOMAIN_NAME = "OS_USER_DOMAIN_NAME";
        private static String PROJECT_DOMAIN_ID = "OS_PROJECT_DOMAIN_ID";
        private static String AUTH_URL = "OS_AUTH_URL";
        private static String PASSWORD = "OS_PASSWORD";
        private static String USERNAME = "OS_USERNAME";
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

    @Override
    protected boolean validateProviderParameters() {
        return loadAndparseCredentialParameters();
    }

    /**
     * Loads credentials.yml or environment variables set via sourced RC file.
     * @return true, if credentials loaded successfully
     */
    private boolean loadAndparseCredentialParameters() {
        OpenStackCredentials openStackCredentials = null;

        if (config.getCredentialsFile() != null) {
            try {
                File credentialsFile = Paths.get(config.getCredentialsFile()).toFile();
                openStackCredentials =  new Yaml().loadAs(new FileInputStream(credentialsFile), OpenStackCredentials.class);

            } catch (FileNotFoundException e) {
                LOG.error("Failed to locate openstack credentials file.", e);
            } catch (YAMLException e) {
                LOG.error("Failed to parse openstack credentials file. {}",
                        e.getCause() != null ? e.getCause().getMessage() : e);
            }
        } else {
            LOG.info("No credentials file provided. Checking environment variables ...");
            Map env =  System.getenv();
            openStackCredentials = new OpenStackCredentials();
            if (env.containsKey(EnvCredentials.PROJECT_NAME)) {
                openStackCredentials.setProjectName((String)env.get(EnvCredentials.PROJECT_NAME));
            }
            if (env.containsKey(EnvCredentials.USER_DOMAIN_NAME)) {
                openStackCredentials.setDomain((String)env.get(EnvCredentials.USER_DOMAIN_NAME));
            }
            if (env.containsKey(EnvCredentials.PROJECT_DOMAIN_ID)) {
                openStackCredentials.setProjectDomain((String)env.get(EnvCredentials.PROJECT_DOMAIN_ID));
            }
            if (env.containsKey(EnvCredentials.AUTH_URL)) {
                openStackCredentials.setEndpoint((String)env.get(EnvCredentials.AUTH_URL));
            }
            if (env.containsKey(EnvCredentials.PASSWORD)) {
                openStackCredentials.setPassword((String)env.get(EnvCredentials.PASSWORD));
            }
            if (env.containsKey(EnvCredentials.USERNAME)) {
                openStackCredentials.setUsername((String)env.get(EnvCredentials.USERNAME));
            }
        }

        if (openStackCredentials == null) {
            LOG.error("Openstack credentials missing (file or environment).");
            return false;
        }

        if (openStackCredentials.getDomain() == null) {
            LOG.error("Openstack credentials : Missing 'domain' parameter!");
            return false;
        }
        if (openStackCredentials.getProjectDomain() == null) {
            LOG.error("Openstack credentials : Missing 'projectDomain' parameter!");
            return false;
        }
        if (openStackCredentials.getProjectName() == null) {
            LOG.error("Openstack credentials : Missing 'projectName' parameter!");
            return false;
        }
        if (openStackCredentials.getEndpoint() == null) {
            LOG.error("Openstack credentials : Missing 'endpoint' parameter!");
            return false;
        }
        if (openStackCredentials.getUsername() == null) {
            LOG.error("Openstack credentials : Missing 'username' parameter!");
            return false;
        }
        if (openStackCredentials.getPassword() == null) {
            LOG.error("Openstack credentials : Missing 'password' parameter!");
            return false;
        }

        openstackConfig.setOpenstackCredentials(openStackCredentials);

        return true;

    }

}

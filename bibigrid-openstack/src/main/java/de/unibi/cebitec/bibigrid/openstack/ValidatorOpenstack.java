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

    private boolean loadAndparseCredentialParameters() {

        OpenStackCredentials openStackCredentials = null;

        // if credentials is given, read it ...
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
            // ... otherwise try to extract necessary informations from environment
            openStackCredentials = new OpenStackCredentials();

            Map env =  System.getenv();

            if (env.containsKey("OS_PROJECT_NAME")){
                openStackCredentials.setProjectName((String)env.get("OS_PROJECT_NAME"));
            }
            if (env.containsKey("OS_USER_DOMAIN_NAME")){
                openStackCredentials.setDomain((String)env.get("OS_USER_DOMAIN_NAME"));
            }
            if (env.containsKey("OS_PROJECT_DOMAIN_NAME")){
                openStackCredentials.setProjectDomain((String)env.get("OS_PROJECT_DOMAIN_NAME"));
            }

            if (env.containsKey("OS_AUTH_URL")){
                openStackCredentials.setEndpoint((String)env.get("OS_AUTH_URL"));
            }
            if (env.containsKey("OS_PASSWORD")){
                openStackCredentials.setPassword((String)env.get("OS_PASSWORD"));
            }
            if (env.containsKey("OS_USERNAME")){
                openStackCredentials.setUsername((String)env.get("OS_USERNAME"));
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

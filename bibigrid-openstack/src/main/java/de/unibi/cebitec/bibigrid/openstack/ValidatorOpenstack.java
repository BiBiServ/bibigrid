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

/**
 * Openstack specfic implementation for a CommandValidatorOpenstack
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
        if (config.getCredentialsFile() != null) {
            try {
                File credentialsFile = Paths.get(config.getCredentialsFile()).toFile();
                openstackConfig.setOpenstackCredentials(
                        new Yaml().loadAs(new FileInputStream(credentialsFile), OpenStackCredentials.class));
                OpenStackCredentials openStackCredentials = openstackConfig.getOpenstackCredentials();
                if (openStackCredentials.getDomain() == null) {
                    LOG.error("Credentials file: Missing 'domain' parameter!");
                    return false;
                }
                if (openStackCredentials.getTenantDomain() == null) {
                    LOG.error("Credentials file: Missing 'tenantDomain' parameter!");
                    return false;
                }
                if (openStackCredentials.getTenantName() == null) {
                    LOG.error("Credentials file: Missing 'tenantName' parameter!");
                    return false;
                }
                if (openStackCredentials.getEndpoint() == null) {
                    LOG.error("Credentials file: Missing 'endpoint' parameter!");
                    return false;
                }
                if (openStackCredentials.getUsername() == null) {
                    LOG.error("Credentials file: Missing 'username' parameter!");
                    return false;
                }
                if (openStackCredentials.getPassword() == null) {
                    LOG.error("Credentials file: Missing 'password' parameter!");
                    return false;
                }
                return true;


            } catch (FileNotFoundException e) {
                LOG.error("Failed to locate openstack credentials file.", e);
            } catch (YAMLException e) {
                LOG.error("Failed to parse openstack credentials file. {}",
                        e.getCause() != null ? e.getCause().getMessage() : e);
            }
        } else {
            LOG.error("Option 'credentialsFile' not set!");
        }
        return false;
    }

}

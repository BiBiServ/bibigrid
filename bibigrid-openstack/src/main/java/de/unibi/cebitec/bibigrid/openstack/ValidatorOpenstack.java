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
 * Openstack specific implementation for a validator.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de,
 * t.dilger(at)uni-bielefeld.de,
 * jkrueger(at)cebitec.uni-bielefeld.de
 */
public final class ValidatorOpenstack extends Validator {
    private final ConfigurationOpenstack openstackConfig;

    public enum EnvCredentials {
        OS_PROJECT_NAME,
        OS_PROJECT_ID,
        OS_USER_DOMAIN_NAME,
        OS_USER_DOMAIN_ID,
        OS_PROJECT_DOMAIN_NAME,
        OS_PROJECT_DOMAIN_ID,
        OS_AUTH_URL,
        OS_PASSWORD,
        OS_USERNAME,
        OS_REGION_NAME
    }

    public ValidatorOpenstack(final Configuration config, final ProviderModule providerModule)
            throws ConfigurationException {
        super(config, providerModule);
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
            LOG.info("Found valid credentials file ({}).", credentialsFile.getAbsolutePath());
            return validateCredentials(openStackCredentials) ? openStackCredentials : null;
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
    public OpenStackCredentials loadEnvCredentials() {
        Map env =  System.getenv();
        OpenStackCredentials openStackCredentials = new OpenStackCredentials();

        // Project[name]
        if (env.containsKey(EnvCredentials.OS_PROJECT_NAME.name())) {
            openStackCredentials.setProject((String)env.get(EnvCredentials.OS_PROJECT_NAME.name()));
        }
        // Project Id
        if (env.containsKey(EnvCredentials.OS_PROJECT_ID.name())) {
            openStackCredentials.setProjectId((String)env.get(EnvCredentials.OS_PROJECT_ID.name()));
        }

        // User Domain
        if (env.containsKey(EnvCredentials.OS_USER_DOMAIN_NAME.name())) {
            openStackCredentials.setUserDomain((String)env.get(EnvCredentials.OS_USER_DOMAIN_NAME.name()));
        }

        // User Domain Id
        if (env.containsKey(EnvCredentials.OS_USER_DOMAIN_ID.name())) {
            openStackCredentials.setUserDomainId((String)env.get(EnvCredentials.OS_USER_DOMAIN_ID.name()));
        }

        // Project Domain
        if (env.containsKey(EnvCredentials.OS_PROJECT_DOMAIN_NAME.name())) {
            openStackCredentials.setProjectDomain((String)env.get(EnvCredentials.OS_PROJECT_DOMAIN_NAME.name()));
        }

        // Project Domain Id
        if (env.containsKey(EnvCredentials.OS_PROJECT_DOMAIN_ID.name())) {
            openStackCredentials.setProjectDomainId((String)env.get(EnvCredentials.OS_PROJECT_DOMAIN_ID.name()));
        }

        // Region
        if (env.containsKey(EnvCredentials.OS_REGION_NAME.name())) {
            openStackCredentials.setRegion((String)env.get(EnvCredentials.OS_REGION_NAME.name()));
        }

        openStackCredentials.setEndpoint((String)env.get(EnvCredentials.OS_AUTH_URL.name()));
        openStackCredentials.setPassword((String)env.get(EnvCredentials.OS_PASSWORD.name()));
        openStackCredentials.setUsername((String)env.get(EnvCredentials.OS_USERNAME.name()));
        return validateCredentials(openStackCredentials) ? openStackCredentials : null;
    }

    private boolean validateCredentials(OpenStackCredentials openStackCredentials){
        if (openStackCredentials.getProject() == null && openStackCredentials.getProjectId() == null) {
            LOG.error("Openstack credentials : Missing 'project' or 'projectId' parameter!");
            return false;
        }
        if (openStackCredentials.getUserDomain() == null && openStackCredentials.getUserDomainId() == null) {
            LOG.error("Openstack credentials : Missing 'userDomain' or 'userDomainId' parameter!");
            return false;
        }
        if (openStackCredentials.getProjectDomain() == null && openStackCredentials.getProjectDomainId() == null) {
            LOG.error("Openstack credentials : Missing 'projectDomain' or 'projectDomainId' parameter!");
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
        if (openStackCredentials.getRegion() == null) {
            LOG.error("Openstack credentials: Missing 'region' parameter");
        }
        return true;

    }

}

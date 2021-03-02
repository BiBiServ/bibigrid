package de.unibi.cebitec.bibigrid.core;

import de.unibi.cebitec.bibigrid.core.model.*;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.Constant.ABORT_WITH_NOTHING_STARTED;

/**
 * Checks, if commandline and configuration input is set correctly.
 */
public abstract class Validator {
    protected static final Logger LOG = LoggerFactory.getLogger(Validator.class);

    protected final List<String> req;
    private final ProviderModule providerModule;

    protected Configuration config;
    private Configuration.WorkerInstanceConfiguration commandLineWorkerInstance;

    public Validator(final Configuration config, final ProviderModule providerModule)
            throws ConfigurationException {
        this.config = config;
        this.providerModule = providerModule;
        req = getRequiredOptions();
    }

    /**
     * Create the {@link Configuration} model instance. Must be overridden by provider
     * implementations for specific configuration fields.
     */
    protected abstract Class<? extends Configuration> getProviderConfigurationClass();

    /**
     * Checks, whether public keys files are readable
     * @return true, if file is valid
     */
    private boolean validateSSHKeyFiles() {
        List<String> keyFiles = new ArrayList<>(config.getSshPublicKeyFiles());
        if (config.getSshPublicKeyFile() != null) {
            keyFiles.add(config.getSshPublicKeyFile());
        }

        for(String keyFile : keyFiles) {
            Path publicKeyFile = Paths.get(keyFile);
            if (!Files.exists(publicKeyFile)) {
                LOG.error("SshPublicKeyFile {} does not exists.", publicKeyFile.toString());
                return false;
            }
            if (!Files.isReadable(publicKeyFile)) {
                LOG.error("SshPublicKeyFile {} is not readable.", publicKeyFile.toString());
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if ansible (galaxy) configuration is valid.
     * @return true, if file(s) found, galaxy, git or url defined and hosts given
     */
    private boolean validateAnsibleRequirements() {
        final String MISSING_HOST = "hosts parameter not set.";
        final String HOST_DEFINE = "hosts parameter has to be defined as either 'master', 'worker', 'workers' or 'all'.";
        // Check Ansible roles
        for (Configuration.AnsibleRoles role : config.getAnsibleRoles()) {
            if (role.getFile() == null) {
                LOG.error("Ansible: file parameter not set.");
                return false;
            }
            Path path = Paths.get(role.getFile());
            if (!Files.isReadable(path)) {
                LOG.error("Ansible: File '{}' does not exist.", path);
                return false;
            } else if (!role.getFile().contains(".tgz") && !role.getFile().contains(".tar.gz")){
                LOG.error("Ansible: File '{}' has to be a '.tgz' or '.tar.gz'.", path);
                return false;
            }
            if (role.getVarsFile() != null) {
                Path varFilePath = Paths.get(role.getVarsFile());
                if (!Files.isReadable(varFilePath)){
                    LOG.error("Ansible: vars file '{}' does not exist.", varFilePath);
                    return false;
                }
            }
            if (role.getHosts() == null) {
                LOG.error("Ansible: " + MISSING_HOST);
                return false;
            } else if (!role.getHosts().equals("master") &&
                    !role.getHosts().equals("worker") &&
                    !role.getHosts().equals("workers") &&
                    !role.getHosts().equals("all")) {
                LOG.error("Ansible: " + HOST_DEFINE);
                return false;
            }
        }
        // Check Ansible Galaxy roles
        for (Configuration.AnsibleGalaxyRoles role : config.getAnsibleGalaxyRoles()) {
            if (role.getGalaxy() == null && role.getGit() == null && role.getUrl() == null) {
                LOG.error("Ansible Galaxy: At least one of 'galaxy', 'git' or 'url' has to be specified.");
                return false;
            }
            if (role.getVarsFile() != null) {
                Path varFilePath = Paths.get(role.getVarsFile());
                if (!Files.isReadable(varFilePath)){
                    LOG.error("Ansible Galaxy: vars file {} does not exist.", varFilePath);
                    return false;
                }
            }
            if (role.getHosts() == null) {
                LOG.error("Ansible Galaxy: " + MISSING_HOST);
                return false;
            } else if (!role.getHosts().equals("master") &&
                    !role.getHosts().equals("worker") &&
                    !role.getHosts().equals("workers") &&
                    !role.getHosts().equals("all")) {
                LOG.error("Ansible Galaxy: " + HOST_DEFINE);
                return false;
            }
            if (role.getUrl() != null) {
                if (!isValidURL(role.getUrl())) {
                    LOG.error("Ansible Galaxy: url parameter contains no valid URL.");
                    return false;
                }
            }
            if (role.getGalaxy() != null) {
                String[] galaxyName = role.getGalaxy().split("[.]");
                if (galaxyName.length != 2) {
                    LOG.error("Ansible Galaxy: galaxy parameter has author.rolename to be defined.");
                    return false;
                }

                if (!isValidAnsibleGalaxyRole(galaxyName[0], galaxyName[1])) {
                    LOG.error("Ansible Galaxy: Not a valid galaxy role: {}.", role.getGalaxy());
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Validates CIDR by using regex pattern.
     * @param serviceCIDR config parameter to
     * @return true, if serviceCIDR provided in config matches pattern, false, if it does not
     */
    private boolean validateServiceCIDR(String serviceCIDR){
        String pattern = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/\\d{1,2}";
        if (!serviceCIDR.matches(pattern)) {
            LOG.error("Value '{}' of option serviceCIDR does not match pattern '{}'.", serviceCIDR, pattern);
            return false;
        }
        return true;
    }

    /**
     * Determine validity of URL.
     * @param url url / galaxy url
     * @return true, if url is valid
     */
    private boolean isValidURL(String url) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.connect();
            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Validates galaxy role by checking API values.
     * @param author galaxy author
     * @param role name of galaxy role
     * @return true, if galaxy role is valid
     */
    private boolean isValidAnsibleGalaxyRole(String author, String role) {
        try {
            String galaxyURL = "https://galaxy.ansible.com/api/v1/roles/?format=json&search=" + role;
            URL u = new URL(galaxyURL);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            InputStream in = new BufferedInputStream(conn.getInputStream());
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(new InputStreamReader(in));

            if (jsonObject.containsKey("count")) {
                int count = Integer.parseInt(String.valueOf(jsonObject.get("count")));
                if (count == 0) {
                    return false;
                } else if (count == 1) {
                    // role name is valid, but probably the author is false
                    JSONArray results = (JSONArray) jsonObject.get("results");
                    if (!results.toJSONString().contains("\"name\":\"" + author + "\"")) {
                        LOG.error("Ansible Galaxy: Author '{}' invalid for specified role.", author);
                        return false;
                    }
                    if (!results.toJSONString().contains("\"name\":\"" + role + "\"")) {
                        LOG.error("Ansible Galaxy: Name of role '{}' is invalid.", role);
                        return false;
                    }
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Checks requirements.
     * @return true, if requirements fulfilled
     */
    public boolean validateConfiguration() {
        boolean validProviderTypes = validateProviderTypes();
        boolean validSSHKeyFiles = validateSSHKeyFiles();
        boolean validAnsibleRequirements = true;
        boolean validServiceCIDR = true;
        if (config.hasCustomAnsibleRoles() || config.hasCustomAnsibleGalaxyRoles()) {
            LOG.info("Checking Ansible configuration ...");
            validAnsibleRequirements = validateAnsibleRequirements();
        }
        return validProviderTypes &&
                validSSHKeyFiles &&
                validAnsibleRequirements;
    }

    protected abstract List<String> getRequiredOptions();

    /**
     * Check provider specific configuration credentials.
     * @return true, if validation successful, otherwise false
     */
    public abstract boolean validateProviderParameters();

    /**
     * Checks master instanceType.
     * @return true, if validation successful, otherwise false
     */
    public boolean validateProviderTypes() {
        try {
            InstanceType masterType = providerModule.getInstanceType(config, config.getMasterInstance().getType());
            config.getMasterInstance().setProviderType(masterType);
        } catch (InstanceTypeNotFoundException e) {
            LOG.error("Invalid master instance type specified!", e);
            return false;
        }
        try {
            for (Configuration.InstanceConfiguration instanceConfiguration : config.getWorkerInstances()) {
                InstanceType workerType = providerModule.getInstanceType(config, instanceConfiguration.getType());
                instanceConfiguration.setProviderType(workerType);
            }
        } catch (InstanceTypeNotFoundException e) {
            LOG.error("Invalid worker instance type specified!", e);
            return false;
        }
        return true;
    }
    /**
     * Validate native instance types and configuration file when used.
     * Step is deferred AFTER client connection is established
     * @return true, if configuration file is invalid
     */
    public boolean configurationInvalid() {
        try {
            if (!validateConfiguration()) {
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return true;
            }
        } catch (Exception e){
            LOG.error(e.getMessage());
            if (Configuration.DEBUG) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }


    protected static boolean isStringNullOrEmpty(final String s) {
        return s == null || s.trim().isEmpty();
    }

    private int checkStringAsInt(String s, int max) throws Exception {
        int v = Integer.parseInt(s);
        if (v < 0 || v > max) {
            throw new Exception();
        }
        return v;
    }

}

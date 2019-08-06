package de.unibi.cebitec.bibigrid.core;

import de.unibi.cebitec.bibigrid.core.model.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import de.unibi.cebitec.bibigrid.core.util.ConfigurationFile;
import org.apache.commons.cli.CommandLine;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks, if commandline and configuration input is set correctly.
 */
public abstract class Validator {
    protected static final Logger LOG = LoggerFactory.getLogger(Validator.class);
    protected final CommandLine cl;
    protected final List<String> req;
    protected final ConfigurationFile configurationFile;
    protected final IntentMode intentMode;
    private final ProviderModule providerModule;
    protected final Configuration config;
    private Configuration.SlaveInstanceConfiguration commandLineSlaveInstance;

    public Validator(final CommandLine cl, final ConfigurationFile configurationFile,
                     final IntentMode intentMode, final ProviderModule providerModule)
            throws ConfigurationException {
        this.cl = cl;
        this.configurationFile = configurationFile;
        this.intentMode = intentMode;
        this.providerModule = providerModule;
        config = configurationFile.loadConfiguration(getProviderConfigurationClass());
        if (config != null && configurationFile.isAlternativeFilepath()) {
            config.setAlternativeConfigPath(configurationFile.getPropertiesFilePath().toString());
        }
        req = getRequiredOptions();
    }

    /**
     * Create the {@link Configuration} model instance. Must be overridden by provider
     * implementations for specific configuration fields.
     */
    protected abstract Class<? extends Configuration> getProviderConfigurationClass();

    protected final boolean checkRequiredParameter(String shortParam, String value) {
        if (req.contains(shortParam) && isStringNullOrEmpty(value)) {
            LOG.error("-" + shortParam + " option is required!");
            return false;
        }
        return true;
    }

    /**
     * Validates correct terminate (cluster-id) cmdline input.
     * @return true, if terminate parameter set correctly
     */
    private boolean parseTerminateParameter() {
        if (req.contains(IntentMode.TERMINATE.getShortParam())) {
            config.setClusterIds(cl.getOptionValue(IntentMode.TERMINATE.getShortParam()).trim());
        }
        return true;
    }

    /**
     * Validates correct cloud9 / ide (cluster-id) cmdline input.
     * @return true, if ide parameter set correctly
     */
    private boolean parseIdeParameter() {
        if (req.contains(IntentMode.CLOUD9.getShortParam())) {
            config.setClusterIds(cl.getOptionValue(IntentMode.CLOUD9.getShortParam()).trim());

        }
        if (req.contains(IntentMode.IDE.getShortParam())) {
            config.setClusterIds(cl.getOptionValue(IntentMode.IDE.getShortParam()).trim());

        }
        return true;
    }

    /**
     * Checks if ansible (galaxy) configuration is valid.
     * @return true, if file(s) found, galaxy, git or url defined and hosts given
     */
    private boolean validateAnsibleRequirements() {
        if (config.hasCustomAnsibleRoles() || config.hasCustomAnsibleGalaxyRoles()) {
            LOG.info("Checking Ansible Configuration...");
        }
        // Check Ansible roles
        for (Configuration.AnsibleRoles role : config.getAnsibleRoles()) {
            if (role.getFile() == null) {
                LOG.error("Ansible: file parameter not set.");
                return false;
            }
            Path path = Paths.get(role.getFile());
            if (!Files.isReadable(path)) {
                LOG.error("Ansible: File {} does not exist.", path);
                return false;
            } else if (!role.getFile().contains(".tgz") && !role.getFile().contains(".tar.gz")){
                LOG.error("Ansible: File {} has to be a '.tgz' or '.tar.gz'.", path);
                return false;
            }
            if (role.getVarsFile() != null) {
                Path varFilePath = Paths.get(role.getVarsFile());
                if (!Files.isReadable(varFilePath)){
                    LOG.error("Ansible: varsFile {} does not exist.", varFilePath);
                    return false;
                }
            }
            if (role.getHosts() == null) {
                LOG.error("Ansible: hosts parameter not set.");
                return false;
            } else if (!role.getHosts().equals("master") &&
                    !role.getHosts().equals("slave") &&
                    !role.getHosts().equals("all")) {
                LOG.error("Ansible: hosts parameter has to be defined either as 'master', 'slave' or 'all'.");
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
                    LOG.error("Ansible Galaxy: varsFile {} does not exist.", varFilePath);
                    return false;
                }
            }
            if (role.getHosts() == null) {
                LOG.error("Ansible Galaxy: hosts parameter not set.");
                return false;
            } else if (!role.getHosts().equals("master") &&
                    !role.getHosts().equals("slave") &&
                    !role.getHosts().equals("all")) {
                LOG.error("Ansible Galaxy: hosts parameter has to be defined either as 'master', 'slave' or 'all'.");
                return false;
            }
            if (role.getUrl() != null) {
                if (!isValidURL(role.getUrl())) {
                    LOG.error("Ansible Galaxy: url parameter contains no valid url.");
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
                    LOG.error("Ansible Galaxy: Not a valid galaxy role {}.", role.getGalaxy());
                    return false;
                }
            }
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
                        LOG.error("Ansible Galaxy: Author {} not correct for specified role.", author);
                        return false;
                    }
                    if (!results.toJSONString().contains("\"name\":\"" + role + "\"")) {
                        LOG.error("Ansible Galaxy: Name of role {} is not correct.", role);
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
     * Checks, if providerModule exists and requirements fulfilled.
     * @param mode ProviderMode
     * @return true, if requirements fulfilled
     */
    public boolean validate(String mode) {
        config.setMode(mode);
        if (providerModule == null) {
            LOG.error("No provider module for mode '" + mode + "' found. ");
            return false;
        }
        if (req == null) {
            LOG.info("No requirements defined ...");
            return true;
        }
        return parseTerminateParameter() && parseIdeParameter() && validateAnsibleRequirements() && validateProviderParameters();
    }

    protected abstract List<String> getRequiredOptions();

    protected abstract boolean validateProviderParameters();

    public boolean validateProviderTypes(Client client) {
        try {
            InstanceType masterType = providerModule.getInstanceType(client, config, config.getMasterInstance().getType());
            config.getMasterInstance().setProviderType(masterType);
        } catch (InstanceTypeNotFoundException e) {
            LOG.error("Invalid master instance type specified!", e);
            return false;
        }
        try {
            for (Configuration.InstanceConfiguration instanceConfiguration : config.getSlaveInstances()) {
                InstanceType slaveType = providerModule.getInstanceType(client, config, instanceConfiguration.getType());
                instanceConfiguration.setProviderType(slaveType);
            }
        } catch (InstanceTypeNotFoundException e) {
            LOG.error("Invalid slave instance type specified!", e);
            return false;
        }
        return true;
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

    public Configuration getConfig() {
        return config;
    }
}

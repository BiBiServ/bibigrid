package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import de.unibi.cebitec.bibigrid.Provider;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import de.unibi.cebitec.bibigrid.openstack.OpenStackCredentials;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ServiceProviderConnector {

    private ConfigurationOpenstack config;
    private ProviderModule module;
    private Client client;
    private String error = "";


    public void setError(String error) { this.error = error; }

    public String getError() { return error; }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setConfig(ConfigurationOpenstack config) {
        this.config = config;
    }

    public ConfigurationOpenstack getConfig() {
        return config;
    }

    public ProviderModule getModule() {
        return module;
    }

    public void setModule(ProviderModule module) {
        this.module = module;
    }

    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";


    public boolean connectToServiceProvider(HttpServerExchange exchange) {

        Map bodyMap = (Map) exchange.getAttachment(BodyHandler.REQUEST_BODY);
        module = null;
        String providerMode = bodyMap.get("mode").toString();

        if (providerMode.equals("openstack")) {
            String[] availableProviderModes = Provider.getInstance().getProviderNames();
            if (availableProviderModes.length == 1) {
                LOG.info("Use {} provider.", availableProviderModes[0]);
                module = Provider.getInstance().getProviderModule(availableProviderModes[0]);
            } else {
                LOG.info("Use {} provider.", providerMode);
                module = Provider.getInstance().getProviderModule(providerMode);
            }
            if (module == null) {
                error = ABORT_WITH_NOTHING_STARTED;
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return false;
            }

            ObjectMapper mapper = new ObjectMapper();
            try {
                String requestBody = mapper.writeValueAsString(bodyMap);

                // TODO figure out whether parsing can fail
                config = new Yaml().loadAs(requestBody, ConfigurationOpenstack.class);

                Map env = System.getenv();
                OpenStackCredentials openStackCredentials = new OpenStackCredentials();

                ArrayList<String> env_vars = new ArrayList<String>( Arrays.asList(  "OS_PROJECT_NAME",
                                                                                    "OS_USER_DOMAIN_NAME",
                                                                                    "OS_PROJECT_DOMAIN_NAME",
                                                                                    "OS_AUTH_URL",
                                                                                    "OS_PASSWORD",
                                                                                    "OS_USERNAME"));
                boolean env_valid = true;
                for(String var : env_vars){
                    if (env.containsKey(var)) {
                        openStackCredentials.setProjectName((String) env.get(var));
                    } else {
                        env_valid = false;
                    }
                }

                if(!env_valid) {
                    error = "There were some errors in your environment variables. Did you source the Openstack RC File?";
                    LOG.info(this.getError());
                    return false;
                }

                config.setOpenstackCredentials(openStackCredentials);

                try {
                    client = module.getClient(config);
                    return true;
                } catch (ClientConnectionFailedException e) {
                    error = e.getMessage();
                    LOG.error(error);
                    LOG.error(ABORT_WITH_NOTHING_STARTED);
                    return false;
                }

            } catch (JsonProcessingException j) {
                error = j.getMessage();
                LOG.error(error);
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return false;
            }
        } else {
            error = "Currently only Openstack as provider is supported";
            return false;
        }
    }
}

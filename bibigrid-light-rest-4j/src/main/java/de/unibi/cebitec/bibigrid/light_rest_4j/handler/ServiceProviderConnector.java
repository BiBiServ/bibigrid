package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import de.unibi.cebitec.bibigrid.Provider;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import de.unibi.cebitec.bibigrid.openstack.ValidatorOpenstack;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;


public class ServiceProviderConnector {

    // Attributes
    private ConfigurationOpenstack config;
    private ProviderModule module;

    /*
    A string is used for custom error messages and to share them with controllers when connection to service provider
    fails.
     */
    private String error = "";

    // Getter and setter
    public void setError(String error) { this.error = error; }

    public String getError() { return error; }

    public void setConfig(ConfigurationOpenstack config) { this.config = config; }

    public ConfigurationOpenstack getConfig() { return config; }

    public ProviderModule getModule() { return module; }

    public void setModule(ProviderModule module) { this.module = module; }

    // Logger
    private static final Logger LOG = LoggerFactory.getLogger(ServiceProviderConnector.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";

    /**
     * Copy of initProviderModule method in StartUp.java
     * Initializes ProviderModule.
     * @param providerMode provider mode
     * @return module, if initialization finished properly, otherwise null
     */
    private ProviderModule initProviderModule(String providerMode) {
        ProviderModule module;
        String [] availableProviderModes = Provider.getInstance().getProviderNames();
        if (availableProviderModes.length == 1) {
            LOG.info("Use {} provider.",availableProviderModes[0]);
            module = Provider.getInstance().getProviderModule(availableProviderModes[0]);
        } else {
            LOG.info("Use {} provider.",providerMode);
            module = Provider.getInstance().getProviderModule(providerMode);
        }
        if (module == null) {
            LOG.error(ABORT_WITH_NOTHING_STARTED);
        }
        return module;
    }

    /**
     * Establish a connection to cloud computing service provider based on a HTTP request and environment vars.
     * This method is based on the main() method of StartUp.java where a connection is established based on CMD and
     * environment vars.
     * @param exchange Received HTTP request
     * @return  <code>true</code> if connection could be established.
     *          <code>false</code> otherwise.
     */
    public boolean connectToServiceProvider(HttpServerExchange exchange) {

        // Process request body
        Map bodyMap = (Map) exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String providerMode = bodyMap.get("mode").toString();

        // The only supported provider mode of api is openstack
        if (providerMode.equals("openstack")) {
            module = initProviderModule(providerMode);
            ObjectMapper mapper = new ObjectMapper();
            try {
                String requestBody = mapper.writeValueAsString(bodyMap);
                // TODO figure out whether parsing can fail

                // get openstack specific specific configuration
                config = new Yaml().loadAs(requestBody, ConfigurationOpenstack.class);

                // set openstack credentials
                ValidatorOpenstack validatorOpenstack = new ValidatorOpenstack(config,module);
                config.setOpenstackCredentials(validatorOpenstack.loadEnvCredentials());

                // Create client
                try {
                    module.createClient(config);
                    return true;
                } catch (ClientConnectionFailedException e) {
                    error = e.getMessage();
                    LOG.error(error);
                    LOG.error(ABORT_WITH_NOTHING_STARTED);
                    return false;
                }

            } catch (JsonProcessingException | ConfigurationException j) {
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

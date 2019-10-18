package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import de.unibi.cebitec.bibigrid.Provider;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public class ServiceProviderConnector {

    private ConfigurationOpenstack config;
    private ProviderModule module;
    private Client client;


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

        String[] availableProviderModes = Provider.getInstance().getProviderNames();
        if (availableProviderModes.length == 1) {
            LOG.info("Use {} provider.", availableProviderModes[0]);
            module = Provider.getInstance().getProviderModule(availableProviderModes[0]);
        } else {
            LOG.info("Use {} provider.", providerMode);
            module = Provider.getInstance().getProviderModule(providerMode);
        }
        if (module == null) {
            LOG.error(ABORT_WITH_NOTHING_STARTED);
            return false;
        }

         ObjectMapper mapper = new ObjectMapper();
            try {
                String requestBody = mapper.writeValueAsString(bodyMap);

                // TODO evaluate error handling possibilities
                config = new Yaml().loadAs(requestBody, ConfigurationOpenstack.class);

                try {
                    client = module.getClient(config);
                    return true;
                } catch (ClientConnectionFailedException e) {
                    LOG.error(e.getMessage());
                    LOG.error(ABORT_WITH_NOTHING_STARTED);
                }

            } catch(JsonProcessingException j){
                LOG.error(j.getMessage());
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return false;
            }
        return false;
    }
}

package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import io.undertow.util.HttpString;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unibi.cebitec.bibigrid.Provider;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;

/**
 * Controller for handling validation requests.
 */
public class BibigridValidatePostHandler implements LightHttpHandler{

    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";

    private void validateConfig(ConfigurationOpenstack config, HttpServerExchange exchange){
        try {
            ProviderModule module = null;
            String providerMode = config.getMode();

            String []  availableProviderModes = Provider.getInstance().getProviderNames();
            if (availableProviderModes.length == 1) {
                LOG.info("Use {} provider.",availableProviderModes[0]);
                module = Provider.getInstance().getProviderModule(availableProviderModes[0]);
            } else {
                LOG.info("Use {} provider.",providerMode);
                module = Provider.getInstance().getProviderModule(providerMode);
            }
            if (module == null) {
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return;
            }
            Client client;
            try {
                client = module.getClient(config);
            } catch (ClientConnectionFailedException e) {
                LOG.error(e.getMessage());
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return;
            }
            Validator validator =  module.getValidator(config,module);
            if (!validator.validateProviderTypes(client)) {
                LOG.error(ABORT_WITH_NOTHING_STARTED);
            }
            try {
                ValidateIntent intent  = module.getValidateIntent(client, config);
                if (intent.validate()) {
                    LOG.info(I, "You can now start your cluster.");
                    exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                    exchange.setStatusCode(200);
                    exchange.getResponseSender().send("{\"is_valid\":\"true\",\"info\":\"You can now start your cluster.\"}");
                } else {
                    exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                    exchange.setStatusCode(200);
                    exchange.getResponseSender().send("{\"is_valid\":\"false\",\"info\":\""+intent.getValidateResponse()+"\"}");
                }
            } catch (Exception e) {
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                exchange.setStatusCode(200);
                exchange.getResponseSender().send("{\"is_valid\":\"false\",\"info\":\"Invalid instance type\"}");
            }
        } catch(Exception e) {
            LOG.error(e.getMessage());
        }
        exchange.endExchange();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map bodyMap = (Map)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String requestBody = mapper.writeValueAsString(bodyMap);
        ConfigurationOpenstack config = new Yaml().loadAs(requestBody,ConfigurationOpenstack.class);

        validateConfig(config, exchange);

        exchange.endExchange();
    }
}

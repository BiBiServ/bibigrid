package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import io.undertow.util.HttpString;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;

/**
 * Controller for handling validation requests.
 */
public class BibigridValidatePostHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";
    private final ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();


    @Override
    public void handleRequest(HttpServerExchange exchange) {
        JSONObject response = new JSONObject();
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");

        if (serviceProviderConnector.connectToServiceProvider(exchange)) {

            ProviderModule module = serviceProviderConnector.getModule();
            Client client = module.getClient();
            ConfigurationOpenstack config = serviceProviderConnector.getConfig();


            try {
                Validator validator = module.getValidator(config, module);
                if (!validator.validateProviderTypes()) {
                    LOG.error(ABORT_WITH_NOTHING_STARTED);
                    exchange.setStatusCode(400);
                    response.put("is_valid", false);
                    response.put("info", "Invalid instance type");

                    exchange.getResponseSender().send(response.toJSONString());
                    // Maybe TODO determine whether master of worker instance was invalid
                }
                ValidateIntent intent = module.getValidateIntent(config);
                if (intent.validate()) {
                    LOG.info(I, "You can now start your cluster.");
                    exchange.setStatusCode(200);
                    response.put("is_valid", true);
                    response.put("info", "You can now start your cluster.");
                    exchange.getResponseSender().send(response.toJSONString());
                } else {
                    exchange.setStatusCode(400);
                    response.put("is_valid", false);
                    response.put("info", intent.getValidateResponse());
                    exchange.getResponseSender().send(response.toJSONString());
                }
            } catch (ConfigurationException c) {
                exchange.setStatusCode(400);
                response.put("is_valid", false);
                response.put("info", c.getMessage());
                exchange.getResponseSender().send(response.toJSONString());
            }
        } else {
            exchange.setStatusCode(400);
            response.put("message", serviceProviderConnector.getError());

            exchange.getResponseSender().send(response.toJSONString());
        }
        exchange.endExchange();
    }
}

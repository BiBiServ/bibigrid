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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;

/**
 * Controller for handling validation requests.
 */
public class BibigridValidatePostHandler implements LightHttpHandler{

    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";
    private ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();


    @Override
    public void handleRequest(HttpServerExchange exchange) {

        if(serviceProviderConnector.connectToServiceProvider(exchange)){

            ProviderModule module = serviceProviderConnector.getModule();
            Client client = serviceProviderConnector.getClient();
            ConfigurationOpenstack config = serviceProviderConnector.getConfig();

            try {
                Validator validator = module.getValidator(config, module);
                if (!validator.validateProviderTypes(client)) {
                    LOG.error(ABORT_WITH_NOTHING_STARTED);
                    exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                    exchange.setStatusCode(400);
                    exchange.getResponseSender().send("{\"is_valid\":\"false\",\"info\":\"Invalid instance type\"}");
                    // Maybe TODO determine whether master of worker instance was invalid
                }
                ValidateIntent intent  = module.getValidateIntent(client, config);
                if (intent.validate()) {
                    LOG.info(I, "You can now start your cluster.");
                    exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                    exchange.setStatusCode(200);
                    exchange.getResponseSender().send("{\"is_valid\":\"true\",\"info\":\"You can now start your cluster.\"}");
                } else {
                    exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                    exchange.setStatusCode(400);
                    exchange.getResponseSender().send("{\"is_valid\":\"false\",\"info\":\""+intent.getValidateResponse()+"\"}");
                }
            } catch(ConfigurationException c){
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                exchange.setStatusCode(400);
                exchange.getResponseSender().send("{\"is_valid\":\"false\",\"info\":\""+c.getMessage()+"\"}");
            }
        } else {
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"message\":\""+serviceProviderConnector.getError()+"\"}");
        }
        exchange.endExchange();
    }
}

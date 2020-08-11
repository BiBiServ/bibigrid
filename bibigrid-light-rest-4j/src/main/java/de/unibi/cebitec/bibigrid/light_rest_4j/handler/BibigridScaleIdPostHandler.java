package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import static de.unibi.cebitec.bibigrid.core.model.IntentMode.*;

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
 * Controller for handling scaling requests.
 */
public class BibigridScaleHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BibigridScaleHandler.class);
    private ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();


    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if (serviceProviderConnector.connectToServiceProvider(exchange)) {
            ProviderModule module = serviceProviderConnector.getModule();
            Client client = serviceProviderConnector.getClient();
            ConfigurationOpenstack config = serviceProviderConnector.getConfig();

            clusterId = exchange.getQueryParameters().get("id").getFirst();
            scale_request = exchange.getQueryParameters().get("scaling").getFirst();
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            try {
                workerBatch = Integer.parseInt(exchange.getQueryParameters().get("batch").getFirst());
                count = Integer.parseInt(exchange.getQueryParameters().get("count").getFirst());
            } catch (NumberFormatException nf) {
                exchange.setStatusCode(400);
                exchange.getResponseSender().send("{\"message\":\"Malformed request\"}");
            }

            switch (scale_request) {
                case SCALE_DOWN:
                    module.getTerminateIntent(client, config)
                            .terminateInstances(clusterId, workerBatch, count);
                    exchange.getResponseSender().send("{\"info\":\"" + module.getTerminateIntent(client, config).getTerminateResponse() + "\"}");
            }
            exchange.endExchange();

        }


    }
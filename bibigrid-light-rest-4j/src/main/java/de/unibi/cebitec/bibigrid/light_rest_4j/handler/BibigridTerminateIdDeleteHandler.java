package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BibigridTerminateIdDeleteHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BibigridTerminateIdDeleteHandler.class);
    private ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();
    private String clusterId;

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        long startTime = System.nanoTime();
        if(serviceProviderConnector.connectToServiceProvider(exchange)){
            ProviderModule module = serviceProviderConnector.getModule();
            Client client = serviceProviderConnector.getClient();
            ConfigurationOpenstack config = serviceProviderConnector.getConfig();

            // set id for termination
            clusterId = exchange.getQueryParameters().get("id").getFirst();
            TerminateIntent terminateIntent = module.getTerminateIntent(client, config);

            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            if(terminateIntent.terminate(clusterId)){
                try {
                    exchange.setStatusCode(200);
                    Map<String, Object> params = new HashMap<>();
                    params.put("info",terminateIntent.getTerminateResponse());
                    String payload = new ObjectMapper().writeValueAsString(params);
                    exchange.getResponseSender().send(payload);
                } catch (JsonProcessingException j){
                    LOG.error(j.getMessage());
                    exchange.setStatusCode(500);
                    exchange.getResponseSender().send("{\"message\":\""+j.getMessage()+"\"}");
                }
            } else {
                exchange.setStatusCode(400);
                exchange.getResponseSender().send("{\"message\":\""+terminateIntent.getTerminateResponse()+"\"}");
            }
        } else {
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"message\":\""+serviceProviderConnector.getError()+"\"}");
        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/1000000; // needed time for handling the request in ms
        LOG.info("TERMINATE /bibigrid/id/"+clusterId+" "+ exchange.getStatusCode()+ " " + duration+"ms\n");
        exchange.endExchange();
    }

}

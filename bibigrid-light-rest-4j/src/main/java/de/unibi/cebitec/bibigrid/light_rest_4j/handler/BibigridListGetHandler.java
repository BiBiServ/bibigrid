package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BibigridListGetHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";

    @Override
    public void handleRequest(HttpServerExchange exchange){

        ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();
        if(serviceProviderConnector.connectToServiceProvider(exchange)){

            ListIntent listIntent =  serviceProviderConnector.getModule().getListIntent(serviceProviderConnector.getClient(), serviceProviderConnector.getConfig());
            String  [] ids =  serviceProviderConnector.getConfig().getClusterIds();

            if (ids != null && ids.length > 0) {
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                exchange.setStatusCode(200);
                Map<String, Object> params = new HashMap<>();
                try {
                    String payload = new ObjectMapper().writeValueAsString(params);
                    params.put("info",listIntent.toDetailString(ids[0]));
                    exchange.getResponseSender().send(payload);
                } catch(JsonProcessingException e){
                    LOG.error(e.getMessage());
                    exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                    exchange.setStatusCode(400);
                    exchange.getResponseSender().send("{\"{\"error\":\""+e.getMessage()+"\",\"}");
                }

            } else {
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                exchange.setStatusCode(200);
                exchange.getResponseSender().send(listIntent.toJsonString());
            }
        }
        else{
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"{\"error\":\"Connection to service provider could not be established\",\"}");
        }
        exchange.endExchange();
    }

}

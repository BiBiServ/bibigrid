package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class BibigridListGetHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);
    private ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();

    @Override
    public void handleRequest(HttpServerExchange exchange){
        long startTime = System.nanoTime();
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        if(serviceProviderConnector.connectToServiceProvider(exchange)){
            ListIntent listIntent = serviceProviderConnector.getModule().getListIntent(serviceProviderConnector.getClient(), serviceProviderConnector.getConfig());
            exchange.setStatusCode(200);
            exchange.getResponseSender().send(listIntent.toJsonString());
        } else {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\""+serviceProviderConnector.getError()+"\"}");
        }
        exchange.endExchange();
        long endTime = System.nanoTime();
        long duration = (endTime - startTime)/1000000; // needed time for handling the request in ms
        LOG.info("GET /bibigrid/list "+ exchange.getStatusCode()+ " " + duration+"ms\n");

    }

}

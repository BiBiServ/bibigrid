package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.Provider;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

public class BibigridTerminateIdDeleteHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) {

        ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();
        if(serviceProviderConnector.connectToServiceProvider(exchange)){

            ProviderModule module = serviceProviderConnector.getModule();
            Client client = serviceProviderConnector.getClient();
            ConfigurationOpenstack config = serviceProviderConnector.getConfig();
            String clusterId = exchange.getQueryParameters().get("id").getFirst();
            config.setClusterIds(clusterId);

            TerminateIntent terminateIntent = module.getTerminateIntent(client, config);
            terminateIntent.terminate();

            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(200);
            Map<String, Object> params = new HashMap<>();
            params.put("info",terminateIntent.getTerminateResponse());
            try {
                String payload = new ObjectMapper().writeValueAsString(params);
                exchange.getResponseSender().send(payload);
            } catch (JsonProcessingException j){
                LOG.error(j.getMessage());
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                exchange.setStatusCode(400);
                exchange.getResponseSender().send("{\"{\"error\":\""+j.getMessage()+"\",\"}");
            }
        } else{
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"{\"error\":\"Connection to service provider could not be established\",\"}");
            // TODO better message for request
        }
        exchange.endExchange();
    }

}

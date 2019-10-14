package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.Provider;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
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

public class BibigridListGetHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";

    @Override
    public void handleRequest(HttpServerExchange exchange){

        OpenstackConnector openstackConnector = new OpenstackConnector();
        if(openstackConnector.connectToServiceProvider(exchange)){

            ListIntent listIntent =  openstackConnector.getModule().getListIntent(openstackConnector.getClient(), openstackConnector.getConfig());
            String  [] ids =  openstackConnector.getConfig().getClusterIds();

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
    }

}

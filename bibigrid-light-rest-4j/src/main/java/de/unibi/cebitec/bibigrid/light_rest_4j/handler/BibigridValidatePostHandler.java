package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ConcreteConfiguration;
import io.undertow.server.HttpServerExchange;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import java.util.*;

/**
 * Controller for handling validation requests.
 */
public class BibigridValidatePostHandler implements LightHttpHandler{


    public Configuration c = new ConcreteConfiguration();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map s = (Map)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String j = mapper.writeValueAsString(s);
        JSONObject json = (JSONObject)new JSONParser().parse(j);
        byte[] decodedBytes = Base64.getDecoder().decode( (String) json.get("credentialsFile"));
        String decodedString = new String(decodedBytes);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            c = objectMapper.readValue(j, Configuration.class);         }
        catch(Exception e){
            System.out.println(e);
        }

        System.out.println(c.getMode());
        System.out.println(c.getSshPublicKeyFile());


        exchange.endExchange();
    }
}

package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class BibigridValidatePostHandler implements LightHttpHandler {
    
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {


        ObjectMapper mapper = new ObjectMapper();
        Map s = (Map)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String j = mapper.writeValueAsString(s);
        JSONObject json = (JSONObject)new JSONParser().parse(j);
        byte[] decodedBytes = Base64.getDecoder().decode( (String) json.get("credentialsFile"));
        String decodedString = new String(decodedBytes);

        System.out.println(decodedString);


        exchange.endExchange();
    }
}

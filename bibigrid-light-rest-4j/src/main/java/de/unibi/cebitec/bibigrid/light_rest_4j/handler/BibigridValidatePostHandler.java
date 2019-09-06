package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.unibi.cebitec.bibigrid.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Port;
import de.unibi.cebitec.bibigrid.core.model.Tomate;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import java.io.DataInput;
import java.util.*;

public class BibigridValidatePostHandler implements LightHttpHandler{


    public Configuration c = new Tomate();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map s = (Map)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String j = mapper.writeValueAsString(s);
        JSONObject json = (JSONObject)new JSONParser().parse(j);
        byte[] decodedBytes = Base64.getDecoder().decode( (String) json.get("credentialsFile"));
        String decodedString = new String(decodedBytes);
        System.out.println(decodedString);

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

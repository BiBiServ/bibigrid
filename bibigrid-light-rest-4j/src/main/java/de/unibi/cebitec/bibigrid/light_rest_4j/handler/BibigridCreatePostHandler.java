package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.light_rest_4j.model.Access;
import de.unibi.cebitec.bibigrid.light_rest_4j.model.ClusterConfiguration;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import java.util.HashMap;
import java.util.Map;

public class BibigridCreatePostHandler implements LightHttpHandler {
    
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        Map<String, Object> body = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);

        Access file = Config.getInstance().getMapper().convertValue(body.get("access"), Access.class);
        ClusterConfiguration cfg =  Config.getInstance().getMapper().convertValue(body.get("clusterConfiguration"), ClusterConfiguration.class);

        System.out.println(file);
        System.out.println(cfg);



        exchange.endExchange();
    }
}

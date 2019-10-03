package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

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
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map bodyMap = (Map)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String requestBody = mapper.writeValueAsString(bodyMap);
        ConfigurationOpenstack config = new Yaml().loadAs(requestBody,ConfigurationOpenstack.class);

        try {
            ProviderModule module = null;
            String providerMode = config.getMode();

            String []  availableProviderModes = Provider.getInstance().getProviderNames();
            if (availableProviderModes.length == 1) {
                LOG.info("Use {} provider.",availableProviderModes[0]);
                module = Provider.getInstance().getProviderModule(availableProviderModes[0]);
            } else {
                LOG.info("Use {} provider.",providerMode);
                module = Provider.getInstance().getProviderModule(providerMode);
            }
            if (module == null) {
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return;
            }

            Client client;
            try {
                client = module.getClient(config);
            } catch (ClientConnectionFailedException e) {
                LOG.error(e.getMessage());
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return;
            }

            ListIntent listIntent = module.getListIntent(client, config);
            String  [] ids =  config.getClusterIds();

            if (ids != null && ids.length > 0) {
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                exchange.setStatusCode(200);
                Map<String, Object> params = new HashMap<>();
                String payload = new ObjectMapper().writeValueAsString(params);
                params.put("info",listIntent.toDetailString(ids[0]));
                exchange.getResponseSender().send(payload);
            } else {
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                exchange.setStatusCode(200);
                Map<String, Object> params = new HashMap<>();
                params.put("info",listIntent.toString());
                String payload = new ObjectMapper().writeValueAsString(params);
                exchange.getResponseSender().send(payload);
            }
        }
        catch(Exception e){
            LOG.error(e.getMessage());
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"Internal server error\"}");
        }
        exchange.endExchange();
    }

}

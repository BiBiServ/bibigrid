package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.body.BodyHandler;
import de.unibi.cebitec.bibigrid.core.intents.ScaleWorkerIntent;
import io.undertow.util.HttpString;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;


/**
 * Controller for handling scaling requests.
 */
public class BibigridScaleIdPostHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BibigridScaleIdPostHandler.class);
    private final ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();
    private int workerBatch;
    private int count;


    @Override
    public void handleRequest(HttpServerExchange exchange) {

        JSONObject response = new JSONObject();
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");

        /**
         * TODO -----------------------------------
         * If the whole request body is passed the call serviceProviderConnector.connectToServiceProvider(exchange) fails
         */
        Map bodyMap = (Map) exchange.getAttachment(BodyHandler.REQUEST_BODY);
        exchange.putAttachment(BodyHandler.REQUEST_BODY, Collections.singletonMap("mode", bodyMap.get("mode")));

        if (serviceProviderConnector.connectToServiceProvider(exchange)) {

            ProviderModule module = serviceProviderConnector.getModule();
            Client client = module.getClient();
            ConfigurationOpenstack config = serviceProviderConnector.getConfig();

            String clusterId = exchange.getQueryParameters().get("id").getFirst();
            String scaling = bodyMap.get("scaling").toString();
            try {
                workerBatch = Integer.parseInt(bodyMap.get("batch").toString());
                count = Integer.parseInt(bodyMap.get("count").toString());
            } catch (NumberFormatException nf) {
                exchange.setStatusCode(400);
                response.put("message", "Malformed request");
                exchange.getResponseSender().send(response.toJSONString());
            }
            ScaleWorkerIntent scaleWorkerIntent = module.getScaleWorkerIntent(config, clusterId, workerBatch, count, scaling);
            Thread t = new Thread(scaleWorkerIntent);
            t.start();
            exchange.setStatusCode(200);
            response.put("info", "Scaling: " + scaling + " started!");

            exchange.getResponseSender().send(response.toJSONString());


        } else {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"message\":\"" + serviceProviderConnector.getError() + ". This is most likely" +
                    " caused by not sourcing the CloudComputingOpenRC.sh file." + "\"}");
        }
        exchange.endExchange();


    }
}
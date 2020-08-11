package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import static de.unibi.cebitec.bibigrid.core.model.IntentMode.*;

import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
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


/**
 * Controller for handling scaling requests.
 */
public class BibigridScaleIdPostHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BibigridScaleIdPostHandler.class);
    private ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();
    private int workerBatch;
    private int count;


    @Override
    public void handleRequest(HttpServerExchange exchange) {
        JSONObject response = new JSONObject();
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        if (serviceProviderConnector.connectToServiceProvider(exchange)) {

            ProviderModule module = serviceProviderConnector.getModule();
            Client client = serviceProviderConnector.getClient();
            ConfigurationOpenstack config = serviceProviderConnector.getConfig();

            String clusterId = exchange.getQueryParameters().get("id").getFirst();
            String scaling = exchange.getQueryParameters().get("scaling").getFirst();
            try {
                workerBatch = Integer.parseInt(exchange.getQueryParameters().get("batch").getFirst());
                count = Integer.parseInt(exchange.getQueryParameters().get("count").getFirst());
            } catch (NumberFormatException nf) {
                exchange.setStatusCode(400);
                response.put("message", "Malformed request");
                exchange.getResponseSender().send(response.toJSONString());
            }
            ScaleWorkerIntent scaleWorkerIntent = module.getScaleWorkerIntent(client, config, clusterId, workerBatch, count, scaling);
            Thread t = new Thread(scaleWorkerIntent);
            t.start();
            exchange.setStatusCode(200);
            response.put("info", "Scaling started!");

            exchange.getResponseSender().send(response.toJSONString());


        } else {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"message\":\"" + serviceProviderConnector.getError() + ". This is most likely" +
                    " caused by not sourcing the CloudComputingOpenRC.sh file." + "\"}");
        }
        exchange.endExchange();


    }
}
package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.intents.LoadClusterConfigurationIntent;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BibigridTerminateIdDeleteHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BibigridTerminateIdDeleteHandler.class);
    private final ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();
    private String clusterId;

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        long startTime = System.nanoTime();
        JSONObject response = new JSONObject();
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        if (serviceProviderConnector.connectToServiceProvider(exchange)) {
            ProviderModule module = serviceProviderConnector.getModule();
            Client client = module.getClient();
            ConfigurationOpenstack config = serviceProviderConnector.getConfig();

            // set id for termination
            clusterId = exchange.getQueryParameters().get("id").getFirst();

            // Load cluster config from cloud provider -> clusterMap for specific intentModes
            LoadClusterConfigurationIntent loadIntent = module.getLoadClusterConfigurationIntent(config);
            loadIntent.loadClusterConfiguration(clusterId);
            // Load specific cluster to put into clusterMap
            Cluster cluster = loadIntent.getCluster(clusterId);
            Map<String, Cluster> clusterMap = new HashMap<>();
            clusterMap.put(clusterId, cluster);
            TerminateIntent terminateIntent = module.getTerminateIntent(config, clusterMap);

            if (terminateIntent.terminate(clusterId)) {
                try {
                    exchange.setStatusCode(200);
                    Map<String, Object> params = new HashMap<>();
                    params.put("info", terminateIntent.getTerminateResponse());
                    String payload = new ObjectMapper().writeValueAsString(params);
                    exchange.getResponseSender().send(payload);
                } catch (JsonProcessingException j) {
                    LOG.error(j.getMessage());
                    exchange.setStatusCode(500);
                    response.put("message", j.getMessage());
                    exchange.getResponseSender().send(response.toJSONString());
                }
            } else {
                exchange.setStatusCode(400);
                response.put("message", terminateIntent.getTerminateResponse());

                exchange.getResponseSender().send(response.toJSONString());
            }
        } else {
            exchange.setStatusCode(400);
            response.put("message", serviceProviderConnector.getError());

            exchange.getResponseSender().send(response.toJSONString());
        }
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000; // needed time for handling the request in ms
        LOG.info("TERMINATE /bibigrid/id/" + clusterId + " " + exchange.getStatusCode() + " " + duration + "ms\n");
        exchange.endExchange();
    }

}

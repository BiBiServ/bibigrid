package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.intents.LoadClusterConfigurationIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class BibigridListGetHandler implements LightHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BibigridListGetHandler.class);
    private final ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();

    @Override
    public void handleRequest(HttpServerExchange exchange){
        long startTime = System.nanoTime();
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        if (serviceProviderConnector.connectToServiceProvider(exchange)) {
            ProviderModule module = serviceProviderConnector.getModule();
            ConfigurationOpenstack config = serviceProviderConnector.getConfig();

            LoadClusterConfigurationIntent loadIntent = module.getLoadClusterConfigurationIntent(config);
            loadIntent.loadClusterConfiguration(null);
            Map<String, Cluster> clusterMap = loadIntent.getClusterMap();
            ListIntent listIntent = serviceProviderConnector.getModule().getListIntent(clusterMap);
            exchange.setStatusCode(200);
            exchange.getResponseSender().send(listIntent.toJsonString());
        } else {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"message\":\"" + serviceProviderConnector.getError() + ". This is most likely" +
                    " caused by not sourcing the CloudComputingOpenRC.sh file." + "\"}");
        }
        exchange.endExchange();
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000; // needed time for handling the request in ms
        LOG.info("GET /bibigrid/list " + exchange.getStatusCode() + " " + duration + "ms\n");

    }

}

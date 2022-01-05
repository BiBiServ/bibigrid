package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.intents.CreateIntent;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public class BibigridCreatePostHandler implements LightHttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";
    private static final String ABORT_WITH_INSTANCES_RUNNING = "Aborting operation. Instances already running. " +
            "Attempting to shut them down but in case of an error they might remain running. Please verify " +
            "afterwards.";
    private static final String KEEP = "Keeping the partly configured cluster for debug purposes. Please remember to shut it down afterwards.";
    // Establish connection to service provider
    private final ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();

    /*
    Attribute that is used to send the cluster_id back to the user
     */
    private String cluster_id;

    /**
     * This method is almost identical to the runCreateIntent method in Sta     rtUp.java, but the access to the method in
     * StartUp.java is private and does not give this controller the cluster_id of the to-be-started cluster, hence
     * a second implementation was made.
     * <p>
     * TODO -----------------------------------
     * Must be implemented asynchronously to return cluster id when creating in CreateCluster
     */
    private boolean runCreateIntent(ProviderModule module, Configuration config, Client client,
                                    CreateCluster cluster) {
        try {
            // configure environment
            cluster.createClusterEnvironment()
                    .createNetwork()
                    .createSubnet()
                    .createSecurityGroup()
                    .createKeyPair()
                    .createPlacementGroup();
            // configure cluster
            boolean success = cluster.configureClusterInstances() && cluster.launchClusterInstances();
            if (!success) {
                /*  In DEBUG mode keep partial configured cluster running, otherwise clean it up */
                if (Configuration.DEBUG) {
                    LOG.error(BibigridCreatePostHandler.KEEP);
                } else {
                    LOG.error(BibigridCreatePostHandler.ABORT_WITH_INSTANCES_RUNNING);
                    Map<String, Cluster> clusterMap = new HashMap<>();
                    clusterMap.put(cluster.getCluster().getClusterId(), cluster.getCluster());
                    TerminateIntent cleanupIntent = module.getTerminateIntent(config, clusterMap);

                    cleanupIntent.terminate(cluster_id);
                }
                return false;
            }
        } catch (ConfigurationException ex) {
            // print stacktrace only in verbose mode, otherwise just the message is fine
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                LOG.error("Failed to create cluster. {} {}", ex.getMessage(), ex);
            } else {
                LOG.error("Failed to create cluster. {}", ex.getMessage());
            }
            return false;
        }
        cluster_id = cluster.getCluster().getClusterId();
        return true;
    }


    @Override
    public void handleRequest(HttpServerExchange exchange) {
        JSONObject response = new JSONObject();
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        if (serviceProviderConnector.connectToServiceProvider(exchange)) {
            ProviderModule module = serviceProviderConnector.getModule();
            Client client = module.getClient();
            ConfigurationOpenstack config = serviceProviderConnector.getConfig();


            try {
                Validator validator = module.getValidator(config, module);


                if (!validator.validateProviderTypes()) {
                    LOG.error(ABORT_WITH_NOTHING_STARTED);
                }
                ValidateIntent intent = module.getValidateIntent(config);
                if (intent.validate()) {
                    CreateIntent create = new CreateIntent(module, config);

                    // run createIntent as background process ..
                    Thread t = new Thread(create);
                    t.start();
                    // ... and return immediately
                    exchange.setStatusCode(200);
                    response.put("id", create.getClusterId());
                    exchange.getResponseSender().send(response.toJSONString());

                } else {
                    exchange.setStatusCode(400);
                    response.put("is_valid", intent.getValidateResponse());

                    exchange.getResponseSender().send(response.toJSONString());
                }
            } catch (ConfigurationException c) {
                exchange.setStatusCode(400);
                response.put("is_valid", false);
                response.put("info", c.getMessage());

                exchange.getResponseSender().send(response.toJSONString());
            }
        } else {
            exchange.setStatusCode(400);
            response.put("message", serviceProviderConnector.getError());
            exchange.getResponseSender().send(response.toJSONString());
        }
        exchange.endExchange();
    }

}

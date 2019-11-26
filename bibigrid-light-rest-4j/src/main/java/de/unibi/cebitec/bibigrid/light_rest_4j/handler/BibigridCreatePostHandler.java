package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BibigridCreatePostHandler implements LightHttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";
    private static final String ABORT_WITH_INSTANCES_RUNNING = "Aborting operation. Instances already running. " +
            "Attempting to shut them down but in case of an error they might remain running. Please verify " +
            "afterwards.";
    private static final String KEEP = "Keeping the partly configured cluster for debug purposes. Please remember to shut it down afterwards.";
    // Establish connection to service provider
    private ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();

    /*
    Attribute that is used to send the cluster_id back to the user
     */
    private String cluster_id;

    /**
     * This method is almost identical to the runCreateIntent method in StartUp.java, but the access to the method in
     * StartUp.java is private and does not give this controller the cluster_id of the to-be-started cluster, hence
     * a second implementation was made.
     */
    private boolean runCreateIntent(ProviderModule module, Configuration config, Client client,
                                    CreateCluster cluster, boolean prepare) {
        try {
            // configure environment
            cluster .createClusterEnvironment()
                    .createNetwork()
                    .createSubnet()
                    .createSecurityGroup()
                    .createKeyPair()
                    .createPlacementGroup();
            // configure cluster
            boolean success =  cluster
                    .configureClusterMasterInstance()
                    .configureClusterWorkerInstance()
                    .launchClusterInstances(prepare);
            if (!success) {
                /*  In DEBUG mode keep partial configured cluster running, otherwise clean it up */
                if (Configuration.DEBUG) {
                    LOG.error(BibigridCreatePostHandler.KEEP);
                } else {
                    LOG.error(BibigridCreatePostHandler.ABORT_WITH_INSTANCES_RUNNING);

                    TerminateIntent cleanupIntent = module.getTerminateIntent(client, config);

                    cleanupIntent.terminate();
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
        cluster_id = cluster.getClusterId();
        return true;
    }


    @Override
    public void handleRequest(HttpServerExchange exchange){
        if(serviceProviderConnector.connectToServiceProvider(exchange)){
            ProviderModule module = serviceProviderConnector.getModule();
            Client client = serviceProviderConnector.getClient();
            ConfigurationOpenstack config = serviceProviderConnector.getConfig();

            try {
                Validator validator = module.getValidator(config, module);
                if (!validator.validateProviderTypes(client)) {
                    LOG.error(ABORT_WITH_NOTHING_STARTED);
                }
                ValidateIntent intent  = module.getValidateIntent(client, config);
                if (intent.validate()) {
                    if(runCreateIntent(module, config, client, module.getCreateIntent(client, config), false)){
                        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                        exchange.setStatusCode(200);
                        exchange.getResponseSender().send("{\"id\":\""+cluster_id+"\"}");
                    }
                    else{
                        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                        exchange.setStatusCode(400);
                        exchange.getResponseSender().send("{\"is_valid\":\"false\",\"info\":\""+intent.getValidateResponse()+"\"}");
                    }
                } else {
                    exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                    exchange.setStatusCode(400);
                    exchange.getResponseSender().send("{\"is_valid\":\"false\",\"info\":\""+intent.getValidateResponse()+"\"}");
                }
            } catch(ConfigurationException c) {
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                exchange.setStatusCode(400);
                exchange.getResponseSender().send("{\"is_valid\":\"false\",\"info\":\""+c.getMessage()+"\"}");
            }
        } else {
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\""+serviceProviderConnector.getError()+"\"}");
        }
        exchange.endExchange();
    }

}

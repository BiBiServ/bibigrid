package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.Provider;
import de.unibi.cebitec.bibigrid.StartUp;
import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;

public class BibigridCreatePostHandler implements LightHttpHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";
    private static final String ABORT_WITH_INSTANCES_RUNNING = "Aborting operation. Instances already running. " +
            "Attempting to shut them down but in case of an error they might remain running. Please verify " +
            "afterwards.";
    private static final String KEEP = "Keeping the partly configured cluster for debug purposes. Please remember to shut it down afterwards.";


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
        return true;
    }


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
            Validator validator =  module.getValidator(config,module);
            if (!validator.validateProviderTypes(client)) {
                LOG.error(ABORT_WITH_NOTHING_STARTED);
            }

            try {
                ValidateIntent intent  = module.getValidateIntent(client, config);
                if (intent.validate()) {
                    if(runCreateIntent(module, config, client, module.getCreateIntent(client, config), false)){
                        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                        exchange.setStatusCode(200);
                        exchange.getResponseSender().send("{\"id\":\"TODO\"}");
                    }
                    else{
                        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                        exchange.setStatusCode(200);
                        exchange.getResponseSender().send("{\"code\":\"TODO\",\"message\":\"TODO!\"}");
                    }
                } else {
                    exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                    exchange.setStatusCode(200);
                    exchange.getResponseSender().send("{\"is_valid\":\"false\",\"info\":\""+intent.getValidateResponse()+"\"}");
                }
            }
            catch (Exception e){
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                exchange.setStatusCode(200);
                exchange.getResponseSender().send("{\"is_valid\":\"false\",\"info\":\"Invalid instance type\"}");
            }
        }
        catch(Exception e){
            LOG.error(e.getMessage());
        }
        exchange.endExchange();
    }

}

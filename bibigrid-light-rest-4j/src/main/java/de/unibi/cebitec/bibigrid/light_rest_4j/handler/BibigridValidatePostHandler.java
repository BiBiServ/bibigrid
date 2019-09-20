package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import de.unibi.cebitec.bibigrid.openstack.OpenStackCredentials;
import org.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unibi.cebitec.bibigrid.Provider;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.openstack.ConfigurationOpenstack;
import io.undertow.server.HttpServerExchange;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;

/**
 * Controller for handling validation requests.
 */
public class BibigridValidatePostHandler implements LightHttpHandler{

    private static final Logger LOG = LoggerFactory.getLogger(BibigridValidatePostHandler.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";
    private static final String ABORT_WITH_INSTANCES_RUNNING = "Aborting operation. Instances already running. " +
            "Attempting to shut them down but in case of an error they might remain running. Please verify " +
            "afterwards.";
    private static final String KEEP = "Keeping the partly configured cluster for debug purposes. Please remember to shut it down afterwards.";




    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map s = (Map)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String j = mapper.writeValueAsString(s);

        ConfigurationOpenstack test = new Yaml().loadAs(j,ConfigurationOpenstack.class);

        try {
            ProviderModule module;
            module = Provider.getInstance().getProviderModule(test.getMode());

            Client client;
            try {
                LOG.error("1 ist null");
                client = module.getClient(test);
                LOG.error("2 ist null");
            } catch (ClientConnectionFailedException e) {
                LOG.error(e.getMessage());
                LOG.error(ABORT_WITH_NOTHING_STARTED);
                return;
            }

//            if(client == null){
//                LOG.error("Client ist null");
//            }
            LOG.error(client.getClass().toString());

            if (module.getValidateIntent(client, test).validate()) {
                LOG.info(I, "You can now start your cluster.");
            } else {
                LOG.error("There were one or more errors. Please adjust your configuration.");
            }


        }
        catch(Exception e){
            LOG.error(e.getLocalizedMessage());
        }





        exchange.endExchange();
    }
}

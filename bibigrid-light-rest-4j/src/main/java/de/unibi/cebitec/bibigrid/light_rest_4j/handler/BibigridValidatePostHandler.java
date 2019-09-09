package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unibi.cebitec.bibigrid.Provider;
import com.networknt.body.BodyHandler;
import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.StartUp;
import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ConcreteConfiguration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
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




    public Configuration c = new ConcreteConfiguration();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map s = (Map)exchange.getAttachment(BodyHandler.REQUEST_BODY);
        String j = mapper.writeValueAsString(s);
        JSONObject json = (JSONObject)new JSONParser().parse(j);
        byte[] decodedBytes = Base64.getDecoder().decode( (String) json.get("credentialsFile"));
        String decodedString = new String(decodedBytes);

        try {
            // create Configuration object
            ObjectMapper objectMapper = new ObjectMapper();
            c = objectMapper.readValue(j, Configuration.class);

            // create Provider Module
            ProviderModule module;
            module = Provider.getInstance().getProviderModule(c.getMode());
            System.out.println(module);


            Validator validator;
//            try {
//
//                // use id instead of commandline object
//                //validator = module.getCommandLineValidator(commandLine, c, module);
//            } catch (ConfigurationException e) {
//                LOG.error(e.getMessage());
//                LOG.error(ABORT_WITH_NOTHING_STARTED);
//                return;
//            }
//            if (validator.validate("openstack")) {
//                Client client;
//                try {
//                    client = module.getClient(validator.getConfig());
//                } catch (ClientConnectionFailedException e) {
//                    LOG.error(e.getMessage());
//                    LOG.error(ABORT_WITH_NOTHING_STARTED);
//                    return;
//                }
//                // In order to validate the native instance types, we need a client. So this step is deferred after
//                // client connection is established.
//                if (!validator.validateProviderTypes(client)) {
//                    LOG.error(ABORT_WITH_NOTHING_STARTED);
//                }
//            }
        }
        catch(Exception e){
            System.out.println(e);
        }





        exchange.endExchange();
    }
}

package de.unibi.cebitec.bibigrid.light_rest_4j.handler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.body.BodyHandler;
import com.networknt.config.Config;


import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.Provider;
import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.light_rest_4j.model.Access;
import de.unibi.cebitec.bibigrid.light_rest_4j.model.ClusterConfiguration;
import io.undertow.server.HttpServerExchange;

import java.io.InputStream;
import java.util.Map;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;

public class BibigridValidatePostHandler implements LightHttpHandler{

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {


        Map<String, Object> body = (Map<String, Object>)exchange.getAttachment(BodyHandler.REQUEST_BODY);

        Access file = Config.getInstance().getMapper().convertValue(body.get("access"), Access.class);
        ClusterConfiguration cfg =  Config.getInstance().getMapper().convertValue(body.get("clusterConfiguration"), ClusterConfiguration.class);

        System.out.println(file);
        System.out.println(cfg);


        ProviderModule module;
        String providerMode = cfg.getMode();
        module = Provider.getInstance().getProviderModule(providerMode);

        Validator validator;
//        try {
//            validator = module.getCommandLineValidator(commandLine, configurationFile, intentMode);
//        } catch (ConfigurationException e) {
//            LOG.error(e.getMessage());
//            LOG.error(ABORT_WITH_NOTHING_STARTED);
//            return;
//        }
//
//        if (validator.validate(providerMode)) {
//            Client client;
//            try {
//                client = module.getClient(validator.getConfig());
//
//                if (module.getValidateIntent(client, true)) {
//                    System.out.println("You can now start your cluster.");
//                } else {
//                    System.out.println("There were one or more errors. Please adjust your configuration.");
//                }
//
//
//            } catch (ClientConnectionFailedException e) {
//                LOG.error(e.getMessage());
//                LOG.error(ABORT_WITH_NOTHING_STARTED);
//                return;
//            }
//        }















        exchange.endExchange();
    }

}

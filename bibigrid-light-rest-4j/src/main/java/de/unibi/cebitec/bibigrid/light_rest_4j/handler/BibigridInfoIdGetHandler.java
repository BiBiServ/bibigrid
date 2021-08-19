package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.DataBase;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.util.Status;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class BibigridInfoIdGetHandler implements LightHttpHandler {

    private final ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
        JSONObject response = new JSONObject();
        if (serviceProviderConnector.connectToServiceProvider(exchange)) {
            String clusterId = exchange.getQueryParameters().get("id").getFirst();
            Status status = DataBase.getDataBase().status.get(clusterId);
            if (status == null) {
                status = new Status(Status.CODE.Error, "Unknown cluster id " + clusterId + "!");
            }
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(200);
            response.put("info", status.code.toString());
            response.put("msg", status.msg);

            try {

                Path path = Paths.get(Configuration.LOG_DIR + "/cluster_" + clusterId + ".log");
                List<String> log = Files.readAllLines(path);
                response.put("log", log);

                exchange.getResponseSender().send(response.toJSONString());

            } catch (IOException e) {
                exchange.getResponseSender().send(response.toJSONString());
            }


        } else {
            response.put("message", serviceProviderConnector.getError());

            exchange.setStatusCode(400);
            exchange.getResponseSender().send(response.toJSONString());
        }
        exchange.endExchange();
    }
}

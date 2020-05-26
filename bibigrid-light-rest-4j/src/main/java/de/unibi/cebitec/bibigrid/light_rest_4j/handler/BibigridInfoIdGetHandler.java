package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.DataBase;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.util.Status;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.slf4j.MDC;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


public class BibigridInfoIdGetHandler implements LightHttpHandler {

    private ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if (serviceProviderConnector.connectToServiceProvider(exchange)) {
            String clusterId = exchange.getQueryParameters().get("id").getFirst();
            Status status = DataBase.getDataBase().status.get(clusterId);
            if (status == null) {
                status = new Status(Status.CODE.Error, "Unknown cluster id +'" + clusterId + "'!");
            }
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(200);


            try {

                Path path = Paths.get(Configuration.LOG_DIR+ "/cluster_" + clusterId + ".log");
                List<String> log = Files.readAllLines(path);
                exchange.getResponseSender().send("{\"info\":\"" + status.code + "\",\"log\":\"" + log + "\"}");

            } catch (IOException e) {
                exchange.getResponseSender().send("{\"info\":\"" + status.code + "\",\"log\":\"" + null + "\"}");
            }


        } else {
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"message\":\"" + serviceProviderConnector.getError() + "\"}");
        }
        exchange.endExchange();
    }
}

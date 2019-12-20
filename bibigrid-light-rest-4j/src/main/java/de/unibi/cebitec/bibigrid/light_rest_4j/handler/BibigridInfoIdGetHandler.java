package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.handler.LightHttpHandler;
import de.unibi.cebitec.bibigrid.core.DataBase;
import de.unibi.cebitec.bibigrid.core.util.Status;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;


public class BibigridInfoIdGetHandler implements LightHttpHandler {

    private ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();

    @Override
    public void handleRequest(HttpServerExchange exchange){
        if(serviceProviderConnector.connectToServiceProvider(exchange)){
            String clusterId = exchange.getQueryParameters().get("id").getFirst();
            Status status = DataBase.getDataBase().status.get(clusterId);
            if (status == null) {
                status = new Status(Status.CODE.Error, "Unknown cluster id +'" + clusterId + "'!");
            }
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(200);
            exchange.getResponseSender().send("{\"info\":\"" + status.code + "\",\"log\":\"" + status.msg + "\"}");
        } else {
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"message\":\""+serviceProviderConnector.getError()+"\"}");
        }
        exchange.endExchange();
    }
}

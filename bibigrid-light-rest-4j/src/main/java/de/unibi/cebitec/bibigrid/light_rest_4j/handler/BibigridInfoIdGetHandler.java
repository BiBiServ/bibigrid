package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.handler.LightHttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;


public class BibigridInfoIdGetHandler implements LightHttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange){
        ServiceProviderConnector serviceProviderConnector = new ServiceProviderConnector();
        if(serviceProviderConnector.connectToServiceProvider(exchange)){
            String clusterId = exchange.getQueryParameters().get("id").getFirst();
            try{
                FileInputStream fstream = new FileInputStream("../../bibigrid-main/"+clusterId+".log");
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                String strLine;
                String lastLine = "";
                while ((strLine = br.readLine()) != null)
                {
                    lastLine = strLine;
                }
                fstream.close();
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                exchange.setStatusCode(200);
                exchange.getResponseSender().send("{\"info\":\"Starting up\",\"log\":\""+lastLine+"\"}");

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
                exchange.setStatusCode(200);
                exchange.getResponseSender().send("{\"error\":\"Unable to find log file.\"}");
            }

        } else {
            exchange.getResponseHeaders().add(new HttpString("Content-Type"), "application/json");
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\""+serviceProviderConnector.getError()+"\"}");
        }
        exchange.endExchange();
    }
}

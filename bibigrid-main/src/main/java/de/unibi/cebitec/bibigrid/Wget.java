package de.unibi.cebitec.bibigrid;



import java.io.*;
import java.net.*;

/**
 * Simple implementation for a wget in Java implementation for proxy settings test.
 *
 */
public class Wget {

    public static void main(String [] args) throws IOException {
        System.out.println("https.proxyHost    : " + System.getProperty("https.proxyHost"));
        System.out.println("https.proxyPort    : " + System.getProperty("https.proxyPort"));
        System.out.println("http.proxyHost     : " + System.getProperty("http.proxyHost"));
        System.out.println("http.proxyPort     : " + System.getProperty("http.proxyPort"));
        System.out.println("socksProxyHost     : " + System.getProperty("socksProxyHost"));
        System.out.println("socksProxyPort     : " + System.getProperty("socksProxyPort"));
        System.out.println("http.nonProxyHosts : " +  System.getProperty("http.nonProxyHosts"));
        System.out.println("Trying to access   : "+args[0]);

        URL url = new URL(args[0]);
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));

        String line = null;
        while ((line = br.readLine())!= null) {
            System.out.println(line);
        }
        br.close();
    }
}

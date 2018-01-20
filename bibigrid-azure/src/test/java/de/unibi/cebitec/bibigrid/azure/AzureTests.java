package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineImage;
import com.microsoft.azure.management.compute.VirtualMachineOffer;
import com.microsoft.azure.management.compute.VirtualMachinePublisher;
import com.microsoft.azure.management.compute.VirtualMachineSku;
import com.microsoft.azure.management.network.Network;
import org.junit.Test;

import java.util.List;

public class AzureTests {
    @Test
    public void connect() {
        ConfigurationAzure config = new ConfigurationAzure();
        config.setAzureCredentialsFile("E:\\Azure_Auth.properties");
        Azure compute = AzureUtils.getComputeService(config);
        /*
        VirtualMachineImage image = compute.virtualMachineImages().getImage("westeurope", "canonical", "UbuntuServer", "16.04-LTS", "latest");
        System.out.println(image.id());
        System.out.println(image.sku());
        System.out.println(image.version());
        System.out.println(image.publisherName());
        System.out.println(image.offer());
        */
        Network vpc = compute.networks().list().stream().filter(x -> x.name().equalsIgnoreCase("default")).findFirst().orElse(null);
        System.out.println(vpc.id());
        System.out.println(vpc.name());
        System.out.println(vpc.type());
        System.out.println(vpc.region());
        System.out.println(vpc.regionName());
        List<String> l = vpc.addressSpaces();
        System.out.println("addressSpaces:");
        for (String s : l) {
            System.out.println("\t" + s);
        }
        l = vpc.dnsServerIPs();
        System.out.println("dnsServerIPs:");
        for (String s : l) {
            System.out.println("\t" + s);
        }
        System.out.println("subnets:");
        for (String s : vpc.subnets().keySet()) {
            System.out.println("\t" + s);
            System.out.println("\t\t" + vpc.subnets().get(s).addressPrefix());
        }
    }
}

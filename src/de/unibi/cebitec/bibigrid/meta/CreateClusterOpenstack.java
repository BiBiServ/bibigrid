///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package de.unibi.cebitec.bibigrid.meta;
//
//import com.google.common.collect.ImmutableSet;
//import com.google.inject.Module;
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.Set;
//import org.jclouds.ContextBuilder;
//import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
//import org.jclouds.openstack.neutron.v2.NeutronApi;
//import org.jclouds.openstack.neutron.v2.domain.Router;
//import org.jclouds.openstack.neutron.v2.domain.Subnet;
//import org.jclouds.openstack.neutron.v2.extensions.RouterApi;
//import org.jclouds.openstack.neutron.v2.features.NetworkApi;
//import org.jclouds.openstack.neutron.v2.features.SubnetApi;
//import org.jclouds.openstack.nova.v2_0.NovaApi;
//import org.jclouds.openstack.nova.v2_0.domain.Network;
//import org.jclouds.openstack.nova.v2_0.domain.Server;
//import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
//import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
//import org.jclouds.openstack.nova.v2_0.features.ServerApi;
//import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
//
///**
// *
// * @author jsteiner
// */
//public class CreateClusterOpenstack implements CreateCluster<CreateClusterOpenstack> {
//
//    public CreateClusterOpenstack() {
//
//    }
//
//    Iterable<Module> modules;
//    NovaApi novaApi;
//    NeutronApi neutronApi;
//    Set<String> regions;
//
////    public void createKeyPair(String name, String path) throws IOException {
////        KeyPairApi keypairApi = this.novaApi.getKeyPairExtensionForZone(this.defaultZone).get();
////        BufferedReader br = null;
////        try {
////            br = new BufferedReader(new FileReader(path));
////            StringBuilder sb = new StringBuilder();
////            String line;
////            while ((line = br.readLine()) != null) {
////                sb.append(line);
////            }
////            line = sb.toString();
////            keypairApi.createWithPublicKey(name, line);
////        } catch (IOException e) {
////            System.out.println("ERROR::Given file path is not valid.");
////        } finally {
////            br.close();
////        }
////    }
//
////    public String createNetwork(String name) {
////        NetworkApi networkApi = neutronApi.getNetworkApiForZone(defaultZone);
////        CreateNetworkOptions createNetworkOptions = CreateNetworkOptions.builder().name(name).build();
////        Network network = networkApi.create(createNetworkOptions);
////        return network.getId();
////    }
////
////    public String createSubnet(String network, String cidr) {
////        SubnetApi subnetApi = neutronApi.getSubnetApiForZone(defaultZone);
////        Subnet subnet = subnetApi.create(network, 4, cidr);
////        return subnet.getId();
////    }
////
////    public String createRouter(String name) {
////        RouterApi routerApi = neutronApi.getRouterExtensionForZone(defaultZone).get();
////        CreateRouterOptions options = CreateRouterOptions.builder().name(name).adminStateUp(true).build();
////        Router router = routerApi.create(options);
////        return router.getId();
////    }
////
////    public String launch_instance(String name, String image, String flavor, String keypair, String network, Iterable<String> secGroup, String userData) {
////        ServerApi serverApi = this.novaApi.getServerApiForZone(defaultZone);
////        CreateServerOptions options = CreateServerOptions.Builder.keyPairName(keypair).networks(network).securityGroupNames(secGroup).userData(userData.getBytes());
////        ServerCreated ser = serverApi.create(name, "imageID", "flavourID", options);
////        return ser.getId();
////    }
//
//    @Override
//    public CreateClusterOpenstack createClusterEnvironment() {
//        modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());
//
//        novaApi = ContextBuilder.newBuilder("openstack-nova")
//                .endpoint("http://ec2-52-19-118-234.eu-west-1.compute.amazonaws.com:5000/v2.0/")
//                .credentials("demo:demo", "nomoresecrets")
//                .modules(modules)
//                .buildApi(NovaApi.class);
//
//        regions = novaApi.getConfiguredRegions();
//        return this;
//    }
//
//    @Override
//    public CreateClusterOpenstack configureClusterMasterInstance() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public CreateClusterOpenstack configureClusterSlaveInstance() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public boolean launchClusterInstances() {
//        for (String region : regions) {
//            ServerApi serverApi = novaApi.getServerApi(region);
//
//            System.out.println("Servers in: " + region);
//
//            for (Server server : serverApi.listInDetail().concat()) {
//                System.out.println(" " + server);
//            }
//        }
//        return true;
//    }
//
//}

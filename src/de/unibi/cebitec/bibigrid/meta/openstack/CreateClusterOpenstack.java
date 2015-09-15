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
//public class CreateClusterOpenstack {
//
//    public CreateClusterOpenstack() {
//
//        Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());
//        
//        String provider = "openstack-nova";
//        String identity = "demo:demo";
//        String credential = "devstack";
//        
//        NovaApi novaApi = ContextBuilder.newBuilder(provider)
//                .endpoint("http://xxx.xxx.xxx.xxx:5000/v2.0/")
//                .credentials(identity, credential)
//                .modules(modules)
//                .buildApi(NovaApi.class);
//        
//        Set<String> regions = novaApi.getConfiguredRegions();
//        
//        
//    }
//
//
//}

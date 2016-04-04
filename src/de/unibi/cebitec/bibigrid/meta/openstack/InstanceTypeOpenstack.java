/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.InstanceType;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import java.util.ArrayList;
import java.util.List;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.sshj.config.SshjSshClientModule;

/**
 * Provides a list of all supported flavors supported by configured openstack installation/region.
 * 
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class InstanceTypeOpenstack extends InstanceType {

    private final String provider = "openstack-nova";

    public InstanceTypeOpenstack(Configuration conf, String type) throws Exception {
        for (Flavor f : listFlavors(conf)) {
            if (f.getName().equalsIgnoreCase(type)) {
                value = f.getName();
                spec = new InstanceInformation.InstanceSpecification(f.getVcpus(), (f.getEphemeral().get() == 0) ? 0 : 1, false, false, false);
                return;
            }
        }
        throw new Exception("Invalid instance type");
    }

    private List<Flavor> listFlavors(Configuration conf) {

        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule());

        NovaApi novaClient = ContextBuilder.newBuilder(provider)
                .endpoint(conf.getOpenstackCredentials().getEndpoint())
                .credentials(conf.getOpenstackCredentials().getTenantName() + ":" + conf.getOpenstackCredentials().getUsername(), conf.getOpenstackCredentials().getPassword())
                .modules(modules)
                .buildApi(NovaApi.class);

        List<Flavor> ret = new ArrayList<>();
        FlavorApi f = novaClient.getFlavorApi(conf.getRegion()); // hardcoded
        for (Flavor r : f.listInDetail().concat()) {
            ret.add(r);
        }
        return ret;
    }
}

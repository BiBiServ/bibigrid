/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.InstanceType;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import java.util.ArrayList;
import java.util.List;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;

/**
 * Provides a list of all supported flavors supported by configured openstack installation/region.
 * 
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class InstanceTypeOpenstack extends InstanceType {

    private OSClient os;

    public InstanceTypeOpenstack(Configuration conf, String type) throws Exception {
        os = OpenStackIntent.buildOSClient(conf);
        for (Flavor f : listFlavors()) {
            if (f.getName().equalsIgnoreCase(type)) {
                value = f.getName();
                spec = new InstanceInformation.InstanceSpecification(f.getVcpus(), (f.getEphemeral() == 0) ? 0 : 1, false, false, false);
                return;
            }
        }
        throw new Exception("Invalid instance type");
    }

    private List<Flavor> listFlavors() {
        List<Flavor> ret = new ArrayList<>();
                
        for (Flavor f : os.compute().flavors().list()) {
            ret.add(f);
            
        }
   
        return ret;
    }
}

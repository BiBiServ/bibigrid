/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.model;

import de.unibi.cebitec.bibigrid.util.InstanceInformation;

/**
 *
 * @author jsteiner
 */
public abstract class InstanceType {
   
    protected String value;
    protected InstanceInformation.InstanceSpecification spec;
    
//    public InstanceType fromValue(String type);

    public String getValue() {
        return value;
    }
    
    public InstanceInformation.InstanceSpecification getSpec() {
        return spec;
    }
    
}

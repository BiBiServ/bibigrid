package de.unibi.cebitec.bibigrid.model;

import de.unibi.cebitec.bibigrid.util.InstanceInformation;

/**
 * @author jsteiner
 */
public abstract class InstanceType {
    protected String value;
    protected InstanceInformation.InstanceSpecification spec;

    public String getValue() {
        return value;
    }

    public InstanceInformation.InstanceSpecification getSpec() {
        return spec;
    }

    @Override
    public String toString() {
        return getValue();
    }
}

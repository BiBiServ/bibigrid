package de.unibi.cebitec.bibigrid.model;

/**
 * @author jsteiner
 */
public abstract class InstanceType {
    protected String value;
    protected InstanceSpecification spec;

    public String getValue() {
        return value;
    }

    public InstanceSpecification getSpec() {
        return spec;
    }

    @Override
    public String toString() {
        return getValue();
    }
}

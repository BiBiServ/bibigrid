package de.unibi.cebitec.bibigrid.model;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
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

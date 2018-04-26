package de.unibi.cebitec.bibigrid.core.model.exceptions;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class InstanceTypeNotFoundException extends Exception {
    public InstanceTypeNotFoundException(){
        super();
    }

    public InstanceTypeNotFoundException(String m){
        super(m);
    }

    public InstanceTypeNotFoundException(String m, Throwable t){
        super(m,t);
    }
}

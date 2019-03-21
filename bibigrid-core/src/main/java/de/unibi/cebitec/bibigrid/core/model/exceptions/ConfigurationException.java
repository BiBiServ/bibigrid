package de.unibi.cebitec.bibigrid.core.model.exceptions;

/**
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ConfigurationException extends Exception {
    public ConfigurationException(Exception e){
        super();
    }
    
    public ConfigurationException(String m){
        super(m);
    }
    
    public ConfigurationException(String m, Throwable t){
        super(m,t);
    }
}

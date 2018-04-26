package de.unibi.cebitec.bibigrid.core.model.exceptions;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ClientConnectionFailedException extends Exception {
    public ClientConnectionFailedException() {
        super();
    }

    public ClientConnectionFailedException(String m) {
        super(m);
    }

    public ClientConnectionFailedException(String m, Throwable t) {
        super(m, t);
    }
}

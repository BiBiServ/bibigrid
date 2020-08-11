package de.unibi.cebitec.bibigrid.core.util;


/**
 * Represents a cluster status (for createIntent)
 */
public class Status {

    public enum CODE {
        Creating, Configuring, Preparing, Running, Error, Scale_Up, Scale_Down
    }

    public CODE code;
    public String msg;

    public Status(CODE code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Status(CODE code) {
        this.code = code;
    }
}

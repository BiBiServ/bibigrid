package de.unibi.cebitec.bibigrid.model.exceptions;

public class IntentNotConfiguredException extends Exception {
    public IntentNotConfiguredException() {
        super("The intent has not been configured yet! Configuration variable not set.");
    }
}
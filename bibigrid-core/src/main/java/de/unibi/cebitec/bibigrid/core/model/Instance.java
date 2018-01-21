package de.unibi.cebitec.bibigrid.core.model;

/**
 * Class representing information about a single cloud instance.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public abstract class Instance {
    public abstract String getPublicIp();
    public abstract String getPrivateIp();
    public abstract String getHostname();
}

package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * Intent for starting and tunneling the cloud9 installation on a cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class Cloud9Intent extends Intent {
    private final Configuration config;

    public Cloud9Intent(Configuration config) {
        this.config = config;
    }

    public void start() {
        // TODO
    }
}

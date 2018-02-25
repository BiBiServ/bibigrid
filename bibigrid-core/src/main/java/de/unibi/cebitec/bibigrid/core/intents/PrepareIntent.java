package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public abstract class PrepareIntent implements Intent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntent.class);
    private final Configuration config;
    private final ProviderModule providerModule;

    protected PrepareIntent(ProviderModule providerModule, Configuration config) {
        this.config = config;
        this.providerModule = providerModule;
    }

    /**
     * Prepare cluster images for the cluster with id in configuration.
     *
     * @return Return true in case of success, false otherwise
     */
    public boolean prepare() {
        boolean success = false;
        return success;
    }
}

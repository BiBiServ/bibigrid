package de.unibi.cebitec.bibigrid.core.intents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider module base class for accessing the implementation details for a specific cloud provider.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public abstract class Intent {
    private static final Logger LOG = LoggerFactory.getLogger(Intent.class);

    protected void sleep(int seconds) {
        sleep(seconds, true);
    }

    protected void sleep(int seconds, boolean throwException) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ie) {
            if (throwException) {
                LOG.error("Thread.sleep interrupted!");
                ie.printStackTrace();
            }
        }
    }
}

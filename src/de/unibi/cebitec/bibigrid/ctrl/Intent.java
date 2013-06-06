package de.unibi.cebitec.bibigrid.ctrl;

import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.model.Configuration;
import java.util.List;

public abstract class Intent {

    private Configuration configuration = null;

    public Intent() {
    }

    public abstract String getCmdLineOption();

    public abstract List<String> getRequiredOptions();

    public abstract boolean execute() throws IntentNotConfiguredException;

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
package de.unibi.cebitec.bibigrid.core.model;

import java.time.ZonedDateTime;

/**
 * Class representing information about a single cloud instance.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public abstract class Instance {
    public static final String TAG_NAME = "name";
    public static final String TAG_USER = "user";
    public static final String TAG_BIBIGRID_ID = "bibigrid-id";

    private boolean isMaster = false;

    private Configuration.InstanceConfiguration configuration;

    protected Instance(Configuration.InstanceConfiguration configuration) {
        this.configuration = configuration;
    }

    public final Configuration.InstanceConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration.InstanceConfiguration configuration) {
        this.configuration = configuration;
    }

    public abstract String getPublicIp();

    public abstract String getPrivateIp();

    public abstract String getHostname();

    public abstract String getId();

    public abstract String getName();

    public abstract String getTag(String key);

    public abstract ZonedDateTime getCreationTimestamp();

    public abstract String getKeyName();

    public boolean isMaster() {
        return isMaster;
    }

    public void setMaster(boolean master) {
        isMaster = master;
    }
}

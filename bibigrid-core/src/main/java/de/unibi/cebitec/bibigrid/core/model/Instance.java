package de.unibi.cebitec.bibigrid.core.model;

import java.time.ZonedDateTime;

/**
 * Class representing information about a single cloud instance. *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public abstract class Instance implements Comparable<Instance> {
    public static final String TAG_NAME = "name";
    public static final String TAG_USER = "user";
    public static final String TAG_BIBIGRID_ID = "bibigrid-id";
    public static final String TAG_BATCH = "worker-batch";

    private boolean isMaster = false;
    private int batchIndex;

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

    public int getBatchIndex() {
        return batchIndex;
    }

    public void setBatchIndex(int batchIndex) {
        this.batchIndex = batchIndex;
    }

    /**
     * Sorts instances by name.
     * @param instance instance to compare with
     * @return a negative integer, zero, or a positive integer as the name of the instance
     *          is less than, equal to, or greater than the name of the specified instance
     */
    public int compareTo(Instance instance) {
        return getName().compareTo(instance.getName());
    }
}

package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Class representing information about a single cloud instance. *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public abstract class Instance implements Comparable<Instance> {
    public static final String TAG_NAME = "name";
    public static final String TAG_USER = "user";
    public static final String TAG_BIBIGRID_ID = "bibigrid-id";
    public static final String TAG_BATCH = "worker-batch";
    public static final String TAG_INDEX = "worker-index";

    private boolean isMaster = false;
    private String clusterid = "UNSET";
    private int batchIndex = -1;
    private int index = -1;


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
        if (batchIndex == -1) {
            extractInfofromName();
        }
        return batchIndex;
    }

    public void setBatchIndex(int batchIndex) {
        this.batchIndex = batchIndex;
    }

    public String getClusterID() {
        if (clusterid == null || clusterid.equalsIgnoreCase("unset")) {
            extractInfofromName();
        }
        return clusterid;
    }

    public int getIndex(){
        if (index == -1) {
            extractInfofromName();
        }
        return index;
    }

    /**
     * Sorts instances by clusterid, batchindex and instance number
     * @param instance instance to compare with
     * @return a negative integer, zero, or a positive integer as the name of the instance
     *          is less than, equal to, or greater than the name of the specified instance
     */
    public int compareTo(Instance instance) {
        int cmp_id = clusterid.compareTo(instance.getClusterID());
        if (cmp_id == 0) {
            int cmp_batch = batchIndex - instance.getBatchIndex();
            if (cmp_batch == 0) {
                return (index - instance.getIndex());
            } else {
                return cmp_batch;
            }
        }
        return cmp_id;
    }

    @Override
    public String toString(){
        return getName();
    }

    /**
     * Helper function that parses an instance name to extract clusterID, batchIndex and index from it.
     */
    private void extractInfofromName(){
        String name = getName();
        if (name != null) {
            // name is something like bibigrid-master-7h880rbcqwqatpb or bibigrid-worker-1-1-7h880rbcqwqatpb
            String [] parts = name.split(CreateCluster.SEPARATOR);
            if (parts.length == 3) { // master
                clusterid = parts[parts.length-1];
            } else {
                clusterid = parts[parts.length-1];
                try {
                    index = Integer.parseInt(parts[parts.length - 2]);
                } catch (NumberFormatException e) {
                    // should never happen
                    e.printStackTrace();
                }
                try {
                    batchIndex = Integer.parseInt(parts[parts.length - 3]);
                } catch (NumberFormatException e) {
                    // should never happen
                    e.printStackTrace();
                }
            }
        }
    }
}

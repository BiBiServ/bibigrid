package de.unibi.cebitec.bibigrid.core;

public class Constant {
    public static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";
    public static final String ABORT_WITH_INSTANCES_RUNNING = "Aborting operation. Instances already running. " +
            "Attempting to shut them down but in case of an error they might remain running. Please verify " +
            "afterwards.";
    public static final String KEEP = "Keeping the partly configured cluster for debug purposes. Please remember to shut it down afterwards.";

}

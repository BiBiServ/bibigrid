package de.unibi.cebitec.bibigrid.model;

import com.amazonaws.util.StringUtils;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentClusters {

    public static final Logger log = LoggerFactory.getLogger(CurrentClusters.class);
    private static Preferences p = Preferences.userNodeForPackage(CurrentClusters.class);
    public static final String DELIMITER = "|";
    public static final String DELIMITER_REGEX = "\\|";

    public static void addCluster(String masterReservationId, String autoScalingGroupName, String clusterId, int slaveInstanceCount, boolean useGluster, String glusterReservationID) {
        String data = StringUtils.join(DELIMITER, masterReservationId, autoScalingGroupName, String.valueOf(slaveInstanceCount), String.valueOf(Calendar.getInstance().getTimeInMillis()), String.valueOf(useGluster), glusterReservationID);
        p.put(clusterId, data);
    }

    public static void removeCluster(String clusterId) {
        p.remove(clusterId);
    }

    private CurrentClusters() {
    }

    public static String getMasterReservationId(String clusterId) {
        return getDataArray(clusterId)[0];
    }
    public static String getGlusterReservationId(String clusterId) {
        return getDataArray(clusterId)[5];
    }

    public static String getAutoScalingGroupName(String clusterId) {
        return getDataArray(clusterId)[1];
    }
    
    public static String getUseGluster(String clusterId){
        return getDataArray(clusterId)[4];
    }

    private static String[] getDataArray(String clusterId) {
        String[] data = p.get(clusterId, "-|-|-|-|-|-|-").split(DELIMITER_REGEX);
        return data;
    }
    
    
    public static boolean exists(String clusterId) {
        return p.get(clusterId, null) != null;
    }

    public static String printClusterList() {
        StringBuilder display = new StringBuilder();
        display.append("\n");
        display.append("cluster-id").append("\t").append("launch date").append("\t\t").append("max # of slaves/EHs").append("\n");
        display.append("----------").append("\t").append("--------------").append("\t\t").append("---------------").append("\n");

        try {
            if (p.keys().length == 0) {
                display.append("No clusters found.\n");
            } else {
                for (String clusterId : p.keys()) {
                    String[] data = getDataArray(clusterId);
                    Calendar launchedCal = Calendar.getInstance();
                    launchedCal.setTimeInMillis(Long.parseLong(data[3]));
                    Date launchedDate = launchedCal.getTime();
                    DateFormat launchedFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                    display.append(clusterId).append("\t").append(launchedFormat.format(launchedDate)).append("\t\t").append(data[2]).append("\n");
                }
            }
            display.append("-------------------------------------------------------");
            display.append("\n");
        } catch (BackingStoreException e) {
            log.error("Could not load current cluster list!");
            return "";
        }
        return display.toString();
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.monitor;

/**
 *
 * @author alueckne
 */
public class MonitoringSettings {
    /**
     * Time to wait after scaling.
     */
    public static int SCALING_COOLDOWN = 60;
    /**
     * Cooldown of last scaling action.
     */
    public static int CURRENT_COOLDOWN = 0;
    /**
     * Defines at which percentage of maximum slots a scale down will be executed.
     */
    public static final float AGGRESSIVENESS = 50f;
    /**
     * Amount of evaluations.
     */
    public static final int EVALUATION_COUNT = 2;
    /**
     * Amount of time between evaluation.
     */
    public static final int EVALUATION_PERIOD = 30;
}

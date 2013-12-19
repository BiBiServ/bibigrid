/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.monitor;

import java.util.Date;

/**
 *
 * @author alueckne
 */
public class QueueEntry {

    private String nodeIp;
    private int slotsTotal;
    private int slotsUsed;
    private Date dateadded;
    private String instanceId;

    public QueueEntry(String nodeIp, String slotsTotal, String slotsUsed, String id) {
        this.nodeIp = nodeIp;
        this.instanceId = id;
        try {
            this.slotsTotal = Integer.parseInt(slotsTotal);
            this.slotsUsed = Integer.parseInt(slotsUsed);

        } catch (NumberFormatException e) {
            this.nodeIp = "NONE";
            this.slotsTotal = -1;
            this.slotsUsed = -1;
        }
    }

    public QueueEntry() {
        nodeIp = "";
    }

    public Date getAdded() {
        return dateadded;
    }

    public void setAdded(Date added) {
        this.dateadded = added;
    }

    public String getNodeIp() {
        return nodeIp;
    }

    public void setNodeIp(String nodeIp) {
        this.nodeIp = nodeIp;
    }

    public int getSlotsTotal() {
        return slotsTotal;
    }

    public void setSlotsTotal(int slotsTotal) {
        this.slotsTotal = slotsTotal;
    }

    public int getSlotsUsed() {
        return slotsUsed;
    }

    public void setSlotsUsed(int slotsUsed) {
        this.slotsUsed = slotsUsed;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }
}

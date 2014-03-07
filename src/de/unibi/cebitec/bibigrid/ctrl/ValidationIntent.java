/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.Request;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alueckne
 */
public class ValidationIntent extends Intent {

    private AmazonEC2 ec2;
    public static final Logger log = LoggerFactory.getLogger(ValidationIntent.class);

    @Override
    public String getCmdLineOption() {
        return "check";
    }

    @Override
    public List<String> getRequiredOptions() {
        return Arrays.asList(new String[]{"ch", "m", "M", "s", "S", "n", "u", "k", "i", "e", "a", "z", "g", "r", "b"});
    }

    @Override
    public boolean execute() throws IntentNotConfiguredException {
        if (getConfiguration() == null) {
            throw new IntentNotConfiguredException();
        }
        log.info("Validating config file...");
        /*
         * Access Key Check
         */
        ec2 = new AmazonEC2Client(this.getConfiguration().getCredentials());
        ec2.setEndpoint("ec2." + this.getConfiguration().getRegion() + ".amazonaws.com");
        boolean success = true;
        try {
            DryRunSupportedRequest<CreateTagsRequest> tryKeys = new DryRunSupportedRequest<CreateTagsRequest>() {
                @Override
                public Request<CreateTagsRequest> getDryRunRequest() {
                    return new CreateTagsRequest().getDryRunRequest();
                }
            };
            DryRunResult dryRunResult = ec2.dryRun(tryKeys);
            if (dryRunResult.isSuccessful()) {
                log.info(I, "Access Key Test successful.");
            } else {
                log.error("AccessKey test not successful. Please check your configuration.");
                return false;
            }

        } catch (AmazonClientException e) {
            log.error("The access or secret key does not seem to valid.");
            return false;

        }

        sleep(1);
        if (checkImages()) {
            log.info(I, "Image check has been successful.");
        } else {
            success = false;
            log.error("There were one or more errors during the last step.");
        }

        sleep(1);
        if (checkSnapshots()) {
            log.info(I, "Snapshot check has been successful.");
        } else {
            success = false;
            log.error("One or more snapshots could not be found.");
        }

        sleep(1);

        if (success) {
            log.info(I, "You can now start your cluster.");
        } else {
            log.error("There were one or more errors. Please adjust your configuration.");
        }
        return true;

    }

    private boolean checkSnapshots() {
        log.info("Checking snapshots");
        boolean allcheck = true;
        this.getConfiguration().getMasterMounts().keySet();
        List<String> snapShotList = new ArrayList<>(this.getConfiguration().getMasterMounts().keySet());
        snapShotList.addAll(this.getConfiguration().getSlaveMounts().keySet());
        for (String e : snapShotList) { //snapshot ids have to be checked individually to find out which one is missing or malformed.
            try {
                if (e.contains(":")) {
                    e = e.substring(0, e.indexOf(":"));
                }
                DescribeSnapshotsRequest snapshotRequest = new DescribeSnapshotsRequest().withSnapshotIds(e);
                DescribeSnapshotsResult snapshotResult = ec2.describeSnapshots(snapshotRequest);
                if (snapshotResult.getSnapshots().get(0).getSnapshotId().equals(e)) {
                    log.info(V, "{} found.", e);
                }
            } catch (AmazonServiceException f) {
                log.error("Snapshot {} could not be found.", e);
                allcheck = false;
            }
        }
        return allcheck;

    }

    private boolean checkImages() {
        log.info("Checking Images...");
        boolean allCheck = true;
        /*
         * Checking for Images in Config File
         */
        try {
            DescribeImagesRequest imageRequest = new DescribeImagesRequest().withImageIds(Arrays.asList(getConfiguration().getMasterImage(), getConfiguration().getSlaveImage()));
            DescribeImagesResult imageResult = ec2.describeImages(imageRequest);
            boolean slave = false, master = false;
            boolean masterType = InstanceInformation.getSpecs(getConfiguration().getMasterInstanceType()).clusterInstance;
            boolean slaveType = InstanceInformation.getSpecs(getConfiguration().getSlaveInstanceType()).clusterInstance;
            /*
             * Checking if both are hvm or paravirtual types
             */
            if (masterType != slaveType) {
                log.error("Both instances have to use hvm or paravirtual images.");
                allCheck = false;
            } else if (masterType) {
                /*
                 * If HVM instances
                 */
                if (getConfiguration().getMasterInstanceType() != getConfiguration().getSlaveInstanceType()) {
                }
            }
            for (Image image : imageResult.getImages()) {
                /*
                 * Checking if Master Image is available.
                 */
                if (image.getImageId().equals(getConfiguration().getMasterImage())) {
                    master = true;
                    if (image.getVirtualizationType().equals("hvm")) { // Image detected is of HVM Type
                        if (masterType) {
                            log.info(I, "Master instance uses HVM images."); // Instance and Image is HVM type
                        } else {
                            log.error("Master Instance type does not support hardware-assisted virtualization."); // HVM Image but instance type is not correct 
                            allCheck = false;
                        }
                    } else {
                        if (masterType) {
                            log.error("Master Instance type does not support paravirtual images."); // Paravirtual Image but cluster instance type
                            allCheck = false;
                        } else {
                            log.info(I, "Master instance uses paravirtual images."); // Instance and Image fits.
                        }
                    }


                }
                /*
                 * Checking if Slave Image is available.
                 */
                if (image.getImageId().equals(getConfiguration().getSlaveImage())) {
                    slave = true;
                    if (image.getVirtualizationType().equals("hvm")) {
                        if (slaveType) {
                            log.info(I, "Slave instance uses HVM images."); // Instance Type and Image uses HVM.
                        } else {
                            log.error("Slave Instance type does not support hardware-assisted virtualization."); // HVM Image but instance type uses paravirtualized images.
                            allCheck = false;
                        }
                    } else {
                        if (slaveType) {
                            log.error("Slave Instance type does not support paravirtual images."); // paravirtual image for slave but instance type is cluster.
                            allCheck = false;
                        } else {
                            log.info(I, "Slave instance uses paravirtual images."); // Slave Instance uses paravirtual images.
                        }
                    }
                }
            }
            if (slave && master) {
                log.info(I, "Master and Slave AMIs have been found.");
            } else {
                log.error("Master and Slave AMIs could not be found.");
                allCheck = false;
            }
            return allCheck;
        } catch (AmazonServiceException e) {
            log.error("Master and Slave AMIs could not be found. Check if the ID is malformed (ami-XXXXXXXX).");
            allCheck = false;
            return allCheck;
        }

    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ie) {
            log.error("Thread.sleep interrupted!");
        }
    }
}

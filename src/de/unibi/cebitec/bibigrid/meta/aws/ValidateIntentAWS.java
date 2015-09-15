/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.Request;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DryRunResult;
import com.amazonaws.services.ec2.model.DryRunSupportedRequest;
import com.amazonaws.services.ec2.model.Image;
import static de.unibi.cebitec.bibigrid.ctrl.ValidationIntent.log;
import de.unibi.cebitec.bibigrid.meta.ValidateIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author jsteiner
 */
public class ValidateIntentAWS implements ValidateIntent {

    private final Configuration conf;

    private AmazonEC2Client ec2;

    public ValidateIntentAWS(final Configuration conf) {
        this.conf = conf;
    }

    @Override
    public boolean validate() {
        log.info("Validating config file...");
        /*
         * Access Key Check
         */
        ec2 = new AmazonEC2Client(conf.getCredentials());
        ec2.setEndpoint("ec2." + conf.getRegion() + ".amazonaws.com");
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
        conf.getMasterMounts().keySet();
        List<String> snapShotList = new ArrayList<>(conf.getMasterMounts().keySet());
        snapShotList.addAll(conf.getSlaveMounts().keySet());
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
            DescribeImagesRequest imageRequest = new DescribeImagesRequest().withImageIds(Arrays.asList(conf.getMasterImage(), conf.getSlaveImage()));
            DescribeImagesResult imageResult = ec2.describeImages(imageRequest);
            boolean slave = false, master = false;
            boolean masterClusterType = InstanceInformation.getSpecs(conf.getMasterInstanceType()).clusterInstance;
            boolean slaveClusterType = InstanceInformation.getSpecs(conf.getSlaveInstanceType()).clusterInstance;
            /*
             * Checking if both are hvm or paravirtual types
             */
            if (masterClusterType != slaveClusterType) {
                log.error("If cluster instances are used please create a homogeneous group.");
                allCheck = false;
            } else if (masterClusterType) {
                /*
                 * If master instance is a cluster instance check if the types are the same
                 */
                if (conf.getMasterInstanceType() != conf.getSlaveInstanceType()) {
                    log.error("If cluster instances are used please create a homogeneous group.");
                    allCheck = false;
                }
            }
            for (Image image : imageResult.getImages()) {
                /*
                 * Checking if Master Image is available.
                 */
                if (image.getImageId().equals(conf.getMasterImage())) {
                    master = true;
                    if (image.getVirtualizationType().equals("hvm")) { // Image detected is of HVM Type
                        if (InstanceInformation.getSpecs(conf.getMasterInstanceType()).hvm) {
                            log.info(I, "Master instance can use HVM images."); // Instance and Image is HVM type
                        } else if (InstanceInformation.getSpecs(conf.getMasterInstanceType()).pvm) {
                            log.error("Master Instance type does not support hardware-assisted virtualization."); // HVM Image but instance type is not correct 
                            allCheck = false;
                        }
                    } else {
                        if (InstanceInformation.getSpecs(conf.getMasterInstanceType()).pvm) {
                            log.info(I, "Master instance can use paravirtual images."); // Instance and Image fits.
                        } else if (InstanceInformation.getSpecs(conf.getMasterInstanceType()).hvm) {
                            log.error("Master Instance type does not support paravirtual images."); // Paravirtual Image but cluster instance type
                            allCheck = false;
                        }
                    }

                }
                /*
                 * Checking if Slave Image is available.
                 */
                if (image.getImageId().equals(conf.getSlaveImage())) {
                    slave = true;
                    if (image.getVirtualizationType().equals("hvm")) { // Image detected is of HVM Type
                        if (InstanceInformation.getSpecs(conf.getSlaveInstanceType()).hvm) {
                            log.info(I, "Slave instance can use HVM images."); // Instance and Image is HVM type
                        } else if (InstanceInformation.getSpecs(conf.getSlaveInstanceType()).pvm) {
                            log.error("Slave Instance type does not support hardware-assisted virtualization."); // HVM Image but instance type is not correct 
                            allCheck = false;
                        }
                    } else {
                        if (InstanceInformation.getSpecs(conf.getSlaveInstanceType()).pvm) {
                            log.info(I, "Slave instance can use paravirtual images."); // Instance and Image fits.
                        } else if (InstanceInformation.getSpecs(conf.getSlaveInstanceType()).hvm) {
                            log.error("Slave Instance type does not support paravirtual images."); // Paravirtual Image but cluster instance type
                            allCheck = false;
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

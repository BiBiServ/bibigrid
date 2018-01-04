package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.util.Arrays;
import java.util.List;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class ValidateIntentAWS extends ValidateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ValidateIntentAWS.class);
    private final ConfigurationAWS config;
    private AmazonEC2Client ec2;

    ValidateIntentAWS(final ConfigurationAWS config) {
        super(config);
        this.config = config;
    }

    @Override
    protected boolean connect() {
        ec2 = IntentUtils.getClient(config);
        try {
            DryRunSupportedRequest<CreateTagsRequest> tryKeys = () -> new CreateTagsRequest().getDryRunRequest();
            DryRunResult dryRunResult = ec2.dryRun(tryKeys);
            if (dryRunResult.isSuccessful()) {
                LOG.info(I, "Access Key Test successful.");
            } else {
                LOG.error("AccessKey test not successful. Please check your configuration.");
                return false;
            }
        } catch (AmazonClientException e) {
            LOG.error("The access or secret key does not seem to valid.");
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkSnapshot(String snapshotId) throws Exception {
        if (snapshotId.contains(":")) {
            snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
        }
        DescribeSnapshotsRequest snapshotRequest = new DescribeSnapshotsRequest().withSnapshotIds(snapshotId);
        DescribeSnapshotsResult snapshotResult = ec2.describeSnapshots(snapshotRequest);
        List<Snapshot> snapshots = snapshotResult.getSnapshots();
        if (snapshots.size() == 0 || !snapshots.get(0).getSnapshotId().equals(snapshotId)) {
            LOG.error("Snapshot {} could not be found.", snapshotId);
            return false;
        }
        LOG.info(V, "Snapshot {} found.", snapshotId);
        return true;
    }

    @Override
    protected boolean checkImages() {
        boolean allCheck = true;
        // Checking for Images in Config File
        try {
            DescribeImagesRequest imageRequest = new DescribeImagesRequest().withImageIds(
                    Arrays.asList(config.getMasterImage(), config.getSlaveImage()));
            DescribeImagesResult imageResult = ec2.describeImages(imageRequest);
            boolean slave = false, master = false;
            boolean masterClusterType = config.getMasterInstanceType().getSpec().isClusterInstance();
            boolean slaveClusterType = config.getSlaveInstanceType().getSpec().isClusterInstance();
            // Checking if both are hvm or paravirtual types
            if (masterClusterType != slaveClusterType) {
                LOG.error("If cluster instances are used please create a homogeneous group.");
                allCheck = false;
            } else if (masterClusterType) {
                // If master instance is a cluster instance check if the types are the same
                if (config.getMasterInstanceType() != config.getSlaveInstanceType()) {
                    LOG.error("If cluster instances are used please create a homogeneous group.");
                    allCheck = false;
                }
            }
            for (Image image : imageResult.getImages()) {
                // Checking if Master Image is available.
                if (image.getImageId().equals(config.getMasterImage())) {
                    master = true;
                    if (image.getVirtualizationType().equals("hvm")) {
                        // Image detected is of HVM Type
                        if (config.getMasterInstanceType().getSpec().isHvm()) {
                            // Instance and Image is HVM type
                            LOG.info(I, "Master instance can use HVM images.");
                        } else if (config.getMasterInstanceType().getSpec().isPvm()) {
                            // HVM Image but instance type is not correct
                            LOG.error("Master Instance type does not support hardware-assisted virtualization.");
                            allCheck = false;
                        }
                    } else {
                        if (config.getMasterInstanceType().getSpec().isPvm()) {
                            // Instance and Image fits.
                            LOG.info(I, "Master instance can use paravirtual images.");
                        } else if (config.getMasterInstanceType().getSpec().isHvm()) {
                            // Paravirtual Image but cluster instance type
                            LOG.error("Master Instance type does not support paravirtual images.");
                            allCheck = false;
                        }
                    }
                }
                // Checking if Slave Image is available.
                if (image.getImageId().equals(config.getSlaveImage())) {
                    slave = true;
                    if (image.getVirtualizationType().equals("hvm")) {
                        // Image detected is of HVM Type
                        if (config.getSlaveInstanceType().getSpec().isHvm()) {
                            // Instance and Image is HVM type
                            LOG.info(I, "Slave instance can use HVM images.");
                        } else if (config.getSlaveInstanceType().getSpec().isPvm()) {
                            // HVM Image but instance type is not correct
                            LOG.error("Slave Instance type does not support hardware-assisted virtualization.");
                            allCheck = false;
                        }
                    } else {
                        if (config.getSlaveInstanceType().getSpec().isPvm()) {
                            // Instance and Image fits.
                            LOG.info(I, "Slave instance can use paravirtual images.");
                        } else if (config.getSlaveInstanceType().getSpec().isHvm()) {
                            // Paravirtual Image but cluster instance type
                            LOG.error("Slave Instance type does not support paravirtual images.");
                            allCheck = false;
                        }
                    }
                }
            }
            if (slave && master) {
                LOG.info(I, "Master and Slave AMIs have been found.");
            } else {
                LOG.error("Master and Slave AMIs could not be found.");
                allCheck = false;
            }
        } catch (AmazonServiceException e) {
            LOG.error("Master and Slave AMIs could not be found. Check if the ID is malformed (ami-XXXXXXXX).");
            allCheck = false;
        }
        return allCheck;
    }
}

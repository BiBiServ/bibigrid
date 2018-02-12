package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;

import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.InstanceImage;
import de.unibi.cebitec.bibigrid.core.model.InstanceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;

import java.util.List;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class ValidateIntentAWS extends ValidateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ValidateIntentAWS.class);
    private final ConfigurationAWS config;
    private AmazonEC2 ec2;

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
    protected boolean checkSnapshot(String snapshotId) {
        if (snapshotId.contains(":")) {
            snapshotId = snapshotId.substring(0, snapshotId.indexOf(":"));
        }
        DescribeSnapshotsRequest snapshotRequest = new DescribeSnapshotsRequest().withSnapshotIds(snapshotId);
        DescribeSnapshotsResult snapshotResult = ec2.describeSnapshots(snapshotRequest);
        List<Snapshot> snapshots = snapshotResult.getSnapshots();
        return snapshots.size() > 0 && snapshots.get(0).getSnapshotId().equals(snapshotId);
    }

    @Override
    protected InstanceImage getImage(Configuration.InstanceConfiguration instanceConfiguration) {
        try {
            DescribeImagesRequest imageRequest = new DescribeImagesRequest().withImageIds(instanceConfiguration.getImage());
            DescribeImagesResult imageResult = ec2.describeImages(imageRequest);
            List<Image> images = imageResult.getImages();
            if (images == null || images.size() == 0) {
                return null;
            }
            if (images.size() > 1) {
                LOG.warn("Multiple images found for id '{}'.", instanceConfiguration.getImage());
            }
            return new InstanceImageAWS(images.get(0));
        } catch (AmazonServiceException ignored) {
        }
        return null;
    }

    @Override
    protected boolean checkProviderImageProperties(InstanceImage image) {
        InstanceType masterClusterType = config.getMasterInstance().getProviderType();
        return checkInstanceVirtualization(masterClusterType.getValue(), masterClusterType, (InstanceImageAWS) image);
    }

    private boolean checkInstanceVirtualization(String specName, InstanceType spec, InstanceImageAWS image) {
        // Checking if both are hvm or paravirtual types
        if (image.isHvm()) {
            // Image detected is of HVM Type
            if (spec.isHvm()) {
                // Instance and Image is HVM type
                LOG.info(I, specName + " instance can use HVM images.");
            } else if (spec.isPvm()) {
                // HVM Image but instance type is not correct
                LOG.error(specName + " instance type does not support hardware-assisted virtualization.");
                return false;
            }
        } else {
            if (spec.isPvm()) {
                // Instance and Image fits.
                LOG.info(I, specName + " instance can use paravirtual images.");
            } else if (spec.isHvm()) {
                // Paravirtual Image but cluster instance type
                LOG.error(specName + " instance type does not support paravirtual images.");
                return false;
            }
        }
        return true;
    }
}

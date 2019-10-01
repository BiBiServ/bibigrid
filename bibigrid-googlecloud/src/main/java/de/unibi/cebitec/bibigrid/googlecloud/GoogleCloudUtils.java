package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for the google cloud.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
final class GoogleCloudUtils {
    private static final Logger LOG = LoggerFactory.getLogger(GoogleCloudUtils.class);
    private static long diskCounter = 1;

    static Instance getInstanceBuilder(Compute compute, ConfigurationGoogleCloud config, String instanceId,
                                       String machineType) {
        MachineType type;
        try {
            type = compute.machineTypes().get(config.getGoogleProjectId(), config.getAvailabilityZone(), machineType).execute();
        } catch (IOException e) {
            LOG.error("Failed to find machine type. {}", e);
            return null;
        }
        return new Instance().setName(instanceId).setZone(config.getAvailabilityZone()).setMachineType(type.getSelfLink());
    }

    private static AttachedDisk createBootDisk(Compute compute, String imageId, String imageProjectId) {
        Image image;
        try {
            image = compute.images().get(imageProjectId, imageId).execute();
        } catch (IOException e) {
            LOG.error("Failed to get image '{}'. {}", imageId, e);
            return null;
        }
        if (image == null) {
            LOG.error("Failed to find boot disk image.");
            return null;
        }
        // Create a simple persistent disk from the image that will be deleted automatically with the instance.
        AttachedDisk disk = new AttachedDisk().setBoot(true).setType("PERSISTENT").setAutoDelete(true);
        if (disk.getInitializeParams() == null) {
            disk.setInitializeParams(new AttachedDiskInitializeParams());
        }
        disk.getInitializeParams().setSourceImage(image.getSelfLink());
        return disk;
    }

    private static AttachedDisk createMountDisk(Compute compute, ConfigurationGoogleCloud config, String snapshotId,
                                                String diskId) {
        String projectId = config.getGoogleProjectId();
        Snapshot snapshot = getSnapshot(compute, projectId, snapshotId);
        if (snapshot == null) {
            LOG.error("Failed to find mount disk snapshot.");
            return null;
        }
        // In order to attach a snapshot, we have to create a persistent disk with the snapshot as disk config.
        String zone = config.getAvailabilityZone();
        Disk disk = new Disk().setSourceSnapshot(snapshot.getSelfLink()).setZone(zone).setType("SNAPSHOT").setName(diskId);
        String diskTargetLink;
        try {
            Operation operation = compute.disks().insert(projectId, zone, disk).execute();
            diskTargetLink = operation.getTargetLink();
            LOG.info(diskTargetLink);
            waitForOperation(compute, config, operation);
        } catch (IOException | InterruptedException e) {
            LOG.error("Creation of mount disk failed. {}", e);
            return null;
        }
        return new AttachedDisk().setType("PERSISTENT").setSource(diskTargetLink);
    }

    static void attachDisks(Compute compute, Instance instance, String imageId, ConfigurationGoogleCloud config,
                            List<Configuration.MountPoint> mounts, String imageProjectId) {
        List<AttachedDisk> attachedDisks = new ArrayList<>();
        // First add the boot disk
        AttachedDisk bootDisk = createBootDisk(compute, imageId, imageProjectId);
        if (bootDisk != null) {
            attachedDisks.add(bootDisk);
        }
        if (mounts != null) {
            for (Configuration.MountPoint mountPoint : mounts) {
                // For the creation of the cluster it's sufficient to add a counter as suffix.
                // The cluster ID is already the greatest difference between multiple clusters.
                String diskId = "disk-" + config.getId() + "-" + mountPoint.getSource() + diskCounter;
                diskCounter++;
                AttachedDisk mountDisk = createMountDisk(compute, config, mountPoint.getSource(), diskId);
                if (mountDisk != null) {
                    attachedDisks.add(mountDisk);
                }
            }
        }
        instance.setDisks(attachedDisks);
    }

    static void setInstanceSchedulingOptions(Instance instance, boolean preemptible) {
        if (preemptible) {
            instance.setScheduling(new Scheduling().setPreemptible(true));
        } else {
            instance.setScheduling(new Scheduling().setAutomaticRestart(true).setOnHostMaintenance("MIGRATE"));
        }
    }

    static void waitForOperation(Compute compute, ConfigurationGoogleCloud config, Operation operation)
            throws InterruptedException {
        String status = operation.getStatus();
        String opId = operation.getName();
        String projectId = config.getGoogleProjectId();
        String zone = config.getAvailabilityZone();
        while (operation != null && !status.equals("DONE")) {
            try {
                Thread.sleep(1000);
                operation = compute.zoneOperations().get(projectId, zone, opId).execute();
                if (operation != null) {
                    status = operation.getStatus();
                }
            } catch (InterruptedException ignored) {
            } catch (IOException e) {
                throw new InterruptedException(e.getMessage());
            }
        }
    }

    static List<Subnetwork> listSubnetworks(Compute compute, String projectId, String region) {
        try {
            return compute.subnetworks().list(projectId, region).execute().getItems();
        } catch (IOException ignored) {
        }
        return new ArrayList<>();
    }

    static Instance reload(Compute compute, ConfigurationGoogleCloud config, Instance instance) {
        try {
            String projectId = config.getGoogleProjectId();
            String zone = config.getAvailabilityZone();
            instance = compute.instances().get(projectId, zone, instance.getName()).execute();
        } catch (IOException ignored) {
        }
        return instance;
    }

    static Snapshot getSnapshot(Compute compute, String projectId, String snapshotId) {
        try {
            return compute.snapshots().get(projectId, snapshotId).execute();
        } catch (IOException e) {
            LOG.error("Failed to get snapshot '{}'. {}", snapshotId, e);
        }
        return null;
    }
}

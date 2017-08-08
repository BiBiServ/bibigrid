package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.compute.*;
import com.google.cloud.compute.spi.v1.HttpComputeRpc;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.util.KEYPAIR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Utility methods for the google cloud.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class GoogleCloudUtils {
    private static final Logger log = LoggerFactory.getLogger(GoogleCloudUtils.class);
    private static final String METADATA_SSH_KEYS = "ssh-keys";

    static Compute getComputeService(final Configuration conf) {
        ComputeOptions.Builder optionsBuilder = ComputeOptions.newBuilder();
        optionsBuilder.setProjectId(conf.getGoogleProjectId());
        try {
            optionsBuilder.setCredentials(GoogleCredentials.fromStream(
                    new FileInputStream(conf.getGoogleCredentialsFile())));
        } catch (IOException e) {
            log.error("{}", e);
            return null;
        }
        return optionsBuilder.build().getService();
    }

    /**
     * Get the internal fully qualified domain name (FQDN) for an instance.
     * https://cloud.google.com/compute/docs/vpc/internal-dns#instance_fully_qualified_domain_names
     */
    public static String getInstanceFQDN(Instance instance) {
        return getInstanceFQDN(instance.getInstanceId().getProject(), instance.getInstanceId().getInstance());
    }

    /**
     * Get the internal fully qualified domain name (FQDN) for an instance.
     * https://cloud.google.com/compute/docs/vpc/internal-dns#instance_fully_qualified_domain_names
     */
    private static String getInstanceFQDN(String projectId, String instanceId) {
        return instanceId + ".c." + projectId + ".internal";
    }

    static String getInstancePrivateIp(Instance instance) {
        List<NetworkInterface> interfaces = instance.getNetworkInterfaces();
        return interfaces.isEmpty() ? null : interfaces.get(0).getNetworkIp();
    }

    static String getInstancePublicIp(Instance instance) {
        List<NetworkInterface> interfaces = instance.getNetworkInterfaces();
        if (interfaces.isEmpty())
            return null;
        List<NetworkInterface.AccessConfig> accessConfigs = interfaces.get(0).getAccessConfigurations();
        return accessConfigs.isEmpty() ? null : accessConfigs.get(0).getNatIp();
    }

    static InstanceInfo.Builder getInstanceBuilder(String zone, String instanceId, String machineType) {
        return InstanceInfo
                .newBuilder(InstanceId.of(zone, instanceId), MachineTypeId.of(zone, machineType));
    }

    private static AttachedDisk createBootDisk(String imageId) {
        AttachedDisk.CreateDiskConfiguration bootDisk = AttachedDisk.CreateDiskConfiguration
                .newBuilder(ImageId.of(imageId))
                .setAutoDelete(true)
                .build();
        return AttachedDisk.of(bootDisk);
    }

    private static AttachedDisk createMountDisk(Compute compute, String zone, String snapshotId, String diskId) {
        SnapshotDiskConfiguration mountDisk = SnapshotDiskConfiguration.of(SnapshotId.of(snapshotId));
        DiskId disk = DiskId.of(zone, diskId);
        DiskInfo diskInfo = DiskInfo.newBuilder(disk, mountDisk).build();
        Operation operation = compute.create(diskInfo);
        try {
            operation.waitFor();
        } catch (InterruptedException | TimeoutException e) {
            log.error("Creation of mount disk failed: {}", operation.getErrors());
        }
        return AttachedDisk.of(AttachedDisk.PersistentDiskConfiguration.of(disk));
    }

    static void attachDisks(Compute compute, InstanceInfo.Builder instanceBuilder, String imageId,
                            String zone, Map<String, String> mounts) {
        List<AttachedDisk> attachedDisks = new ArrayList<>();
        AttachedDisk bootDisk = GoogleCloudUtils.createBootDisk(imageId);
        attachedDisks.add(bootDisk);
        for (String key : mounts.keySet()) {
            String diskId = "disk-" + key; // TODO: think of better disk name for snapshot
            AttachedDisk mountDisk = createMountDisk(compute, zone, key, diskId);
            attachedDisks.add(mountDisk);
        }
        instanceBuilder.setAttachedDisks(attachedDisks);
    }

    static void setInstanceSchedulingOptions(InstanceInfo.Builder builder, boolean preemtible) {
        if (preemtible) {
            builder.setSchedulingOptions(SchedulingOptions.preemptible());
        } else {
            builder.setSchedulingOptions(SchedulingOptions.standard(true,
                    SchedulingOptions.Maintenance.MIGRATE));
        }
    }

    /**
     * https://cloud.google.com/compute/docs/instances/adding-removing-ssh-keys#sshkeyformat
     */
    static void addKeypairToMetadata(Instance instance, KEYPAIR keypair) {
        // TODO: check format
        String sshKey = keypair.getPublicKey();

        Metadata metadata = instance.getMetadata();
        if (metadata == null)
            metadata = Metadata.newBuilder().add(METADATA_SSH_KEYS, sshKey).build();
        else {
            Map<String, String> values = metadata.getValues();
            if (values.containsKey(METADATA_SSH_KEYS)) {
                values.put(METADATA_SSH_KEYS, values.get(METADATA_SSH_KEYS) + "\n" + sshKey);
            } else {
                values.put(METADATA_SSH_KEYS, sshKey);
            }
            metadata = metadata.toBuilder().setValues(values).build();
        }
        instance.setMetadata(metadata);
    }

    /**
     * Using reflection, we can access the internal api library compute instance to make
     * calls like firewall rules creation that are currently not available in the cloud api.
     */
    static com.google.api.services.compute.Compute getInternalCompute(Compute compute) {
        HttpComputeRpc computeRpc = ((HttpComputeRpc) compute.getOptions().getRpc());
        try {
            Field f = computeRpc.getClass().getDeclaredField("compute");
            f.setAccessible(true);
            return (com.google.api.services.compute.Compute) f.get(computeRpc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

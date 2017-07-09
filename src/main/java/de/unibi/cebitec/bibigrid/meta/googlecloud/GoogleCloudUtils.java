package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.compute.*;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.util.KEYPAIR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for the google cloud.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
final class GoogleCloudUtils {
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
    static String getInstanceFQDN(String projectId, String instanceId) {
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

    static AttachedDisk createBootDisk(String imageId) {
        AttachedDisk.CreateDiskConfiguration bootDisk = AttachedDisk.CreateDiskConfiguration
                .newBuilder(ImageId.of(imageId))
                .setAutoDelete(true)
                .build();
        return AttachedDisk.of(bootDisk);
    }

    static InstanceInfo.Builder getInstanceBuilder(String imageId, String zone, String instanceId, String machineType) {
        AttachedDisk bootDisk = GoogleCloudUtils.createBootDisk(imageId);
        return InstanceInfo
                .newBuilder(InstanceId.of(zone, instanceId), MachineTypeId.of(zone, machineType))
                .setAttachedDisks(bootDisk);
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
}

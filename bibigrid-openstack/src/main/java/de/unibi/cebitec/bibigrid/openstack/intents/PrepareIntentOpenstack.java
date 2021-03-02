package de.unibi.cebitec.bibigrid.openstack.intents;

import de.unibi.cebitec.bibigrid.core.intents.PrepareIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.openstack.ClientOpenstack;
import de.unibi.cebitec.bibigrid.openstack.InstanceOpenstack;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.image.DiskFormat;
import org.openstack4j.model.image.Image;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeUploadImage;
import org.openstack4j.model.storage.block.options.UploadImageData;
import org.openstack4j.openstack.storage.block.domain.CinderVolume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * Openstack implementation of the prepare intent.
 * <p/>
 * Example of creating an image from an instance: http://khmel.org/?p=1188
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class PrepareIntentOpenstack extends PrepareIntent {
    private static final Logger LOG = LoggerFactory.getLogger(PrepareIntentOpenstack.class);

    private final OSClient os;

    public PrepareIntentOpenstack(ProviderModule providerModule, Client client, Configuration config) {
        super(providerModule, client, config);
        os = ((ClientOpenstack) client).getInternal();
    }

    @Override
    protected boolean stopInstance(Instance instance) {
        ActionResponse response = os.compute().servers().action(((InstanceOpenstack) instance).getInternal().getId(),
                Action.STOP);
        if (!response.isSuccess()) {
            LOG.error("Failed to stop instance '{}'. {}", instance.getName(), response.getFault());
        }
        return response.isSuccess();
    }

    @Override
    protected void waitForInstanceShutdown(Instance instance) {
        InstanceOpenstack instanceOpenstack = (InstanceOpenstack) instance;
        do {
            Server si = os.compute().servers().get(instanceOpenstack.getId());
            instanceOpenstack.setServer(si);
            Server.Status status = si.getStatus();
            LOG.info(V, "Status of instance '{}': {}", instance.getName(), status);
            if (status != null && status == Server.Status.SHUTOFF) {
                break;
            } else {
                LOG.info(V, "...");
                sleep(10);
            }
        } while (true);
    }

    @Override
    protected boolean createImageFromInstance(Instance instance, String imageName) {
        InstanceOpenstack instanceOpenstack = (InstanceOpenstack) instance;
        String snapshotId = os.compute().servers().createSnapshot(instanceOpenstack.getId(), imageName + "-snap");
        do {
            Volume.Status status = os.blockStorage().snapshots().get(snapshotId).getStatus();
            LOG.info(V, "Status of snapshot '{}': {}", snapshotId, status);
            if (status != null && status == Volume.Status.AVAILABLE) {
                break;
            } else {
                LOG.info(V, "...");
                sleep(2);
            }
        } while (true);
        Volume volume = CinderVolume.builder()
                .snapshot(snapshotId)
                .bootable(true)
                .name(imageName + "-vol")
                .build();
        volume = os.blockStorage().volumes().create(volume);
        do {
            Volume.Status status = os.blockStorage().volumes().get(volume.getId()).getStatus();
            LOG.info(V, "Status of volume '{}': {}", snapshotId, status);
            if (status != null && status == Volume.Status.AVAILABLE) {
                break;
            } else {
                LOG.info(V, "...");
                sleep(2);
            }
        } while (true);
        os.blockStorage().snapshots().delete(snapshotId);
        UploadImageData imageData = UploadImageData.create(imageName).diskFormat(DiskFormat.QCOW2);
        VolumeUploadImage uploadImage = os.blockStorage().volumes().uploadToImage(volume.getId(), imageData);
        do {
            Image.Status status = os.images().get(uploadImage.getImageId()).getStatus();
            LOG.info(V, "Status of image '{}': {}", snapshotId, status);
            if (status != null && status == Image.Status.ACTIVE) {
                break;
            } else {
                LOG.info(V, "...");
                sleep(2);
            }
        } while (true);
        os.blockStorage().volumes().delete(volume.getId());
        // TODO: tag IMAGE_SOURCE_LABEL
        return true;
    }
}

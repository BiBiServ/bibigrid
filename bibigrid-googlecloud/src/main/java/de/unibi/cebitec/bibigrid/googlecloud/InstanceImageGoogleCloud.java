package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.model.Image;
import de.unibi.cebitec.bibigrid.core.model.InstanceImage;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class InstanceImageGoogleCloud extends InstanceImage {
    private final Image image;

    InstanceImageGoogleCloud(Image image) {
        this.image = image;
    }

    @Override
    public long getMinDiskSpace() {
        return image.getDiskSizeGb() * 1024;
    }

    @Override
    public int getMinRam() {
        // TODO
        return 0;
    }

    @Override
    public String getId() {
        return image.getSourceImageId();
    }

    @Override
    public String getName() {
        return image.getSourceImage();
    }
}

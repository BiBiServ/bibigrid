package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.model.Image;
import de.unibi.cebitec.bibigrid.core.model.InstanceImage;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class InstanceImageAWS extends InstanceImage {
    private final Image image;

    InstanceImageAWS(Image image) {
        this.image = image;
    }

    @Override
    public long getMinDiskSpace() {
        // TODO
        return 0;
    }

    @Override
    public int getMinRam() {
        // TODO
        return 0;
    }

    @Override
    public String getId() {
        return image.getImageId();
    }

    @Override
    public String getName() {
        return image.getName();
    }

    public boolean isHvm() {
        return "hvm".equals(image.getVirtualizationType());
    }
}

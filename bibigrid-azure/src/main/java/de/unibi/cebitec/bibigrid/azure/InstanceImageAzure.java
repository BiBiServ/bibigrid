package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.compute.ImageReference;
import de.unibi.cebitec.bibigrid.core.model.InstanceImage;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class InstanceImageAzure extends InstanceImage {
    private final ImageReference image;

    InstanceImageAzure(ImageReference image) {
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
}

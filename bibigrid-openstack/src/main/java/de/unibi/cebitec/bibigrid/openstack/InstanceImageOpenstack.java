package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.InstanceImage;
import org.openstack4j.model.compute.Image;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class InstanceImageOpenstack extends InstanceImage {
    private final Image image;

    InstanceImageOpenstack(Image image) {
        this.image = image;
    }

    @Override
    public long getMinDiskSpace() {
        return image.getMinDisk();
    }

    @Override
    public int getMinRam() {
        return image.getMinRam();
    }

    @Override
    public String getId() {
        return image.getId();
    }

    @Override
    public String getName() {
        return image.getName();
    }
}

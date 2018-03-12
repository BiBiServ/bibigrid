package de.unibi.cebitec.bibigrid.core.model;

import java.util.List;

/**
 * Abstract client for common cloud provider tasks.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public abstract class Client {
    public abstract List<Network> getNetworks();

    public abstract Network getNetworkByName(String networkName);

    public abstract Network getNetworkById(String networkId);

    public abstract List<Subnet> getSubnets();

    public abstract Subnet getSubnetByName(String subnetName);

    public abstract Subnet getSubnetById(String subnetId);

    public abstract InstanceImage getImageByName(String imageName);

    public abstract InstanceImage getImageById(String imageId);

    public abstract Snapshot getSnapshotByName(String snapshotName);

    public abstract Snapshot getSnapshotById(String snapshotId);
}

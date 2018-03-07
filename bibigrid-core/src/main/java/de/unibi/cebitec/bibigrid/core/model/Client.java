package de.unibi.cebitec.bibigrid.core.model;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public abstract class Client {
    public abstract Network getNetworkByName(String networkName);

    public abstract Network getNetworkById(String networkId);

    public abstract Subnet getSubnetByName(String subnetName);

    public abstract Subnet getSubnetById(String subnetId);

    public abstract InstanceImage getImageByName(String imageName);

    public abstract InstanceImage getImageById(String imageId);
}

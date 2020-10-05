package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.NotYetSupportedException;

import java.util.List;

/**
 * Abstract client for common cloud provider tasks.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 * @author jkrueger(at)cebitec.uni-bielefeld.de
 */
public abstract class Client {

    public abstract void authenticate() throws ClientConnectionFailedException;

    public abstract List<Network> getNetworks();

    @Deprecated
    public abstract Network getNetworkByName(String networkName);

    @Deprecated
    public abstract Network getNetworkById(String networkId);

    public abstract Network getNetworkByIdOrName(String network) throws NotYetSupportedException;

    public abstract Network getDefaultNetwork();

    public abstract List<Subnet> getSubnets();

    public abstract List<String> getKeypairNames();

    @Deprecated
    public abstract Subnet getSubnetByName(String subnetName);

    @Deprecated
    public abstract Subnet getSubnetById(String subnetId);

    public abstract Subnet getSubnetByIdOrName(String subnet) throws NotYetSupportedException;

    @Deprecated
    public abstract InstanceImage getImageByName(String imageName);

    @Deprecated
    public abstract InstanceImage getImageById(String imageId);

    public abstract InstanceImage getImageByIdOrName(String image) throws NotYetSupportedException;

    @Deprecated
    public abstract Snapshot getSnapshotByName(String snapshotName);

    @Deprecated
    public abstract Snapshot getSnapshotById(String snapshotId);

    public abstract Snapshot getSnapshotByIdOrName(String snapshot) throws NotYetSupportedException;


    public abstract ServerGroup getServerGroupByIdOrName(String serverGroup) throws NotYetSupportedException;
}

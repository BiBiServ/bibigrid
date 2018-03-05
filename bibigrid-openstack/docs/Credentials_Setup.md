# OpenStack credentials setup
Setting up the OpenStack credentials involves two parts.

## Instance access SSH keys
In order for the BiBiGrid tool to access the master instance during setup and for your
own access after the cluster is created, a keypair needs to be generated or uploaded.
In the horizon dashboard navigate to **Access & Security** (1) and open the tab
**Key Pairs** (2). You can either **create** a new keypair (3) or **import** an existing
one (3). After the key is successfully setup, a new entry will show in the list (4).
The **Key Pair Name** needs to be referenced in the configuration file or command line
in order to create a new cluster.

![Figure 1](img/openstack-horizon-keypair.png)
*Figure 1: Horizon dashboard Key Pairs*

## API credentials
In order for the BiBiGrid tool to access the OpenStack API, several information
need to be collected. Most of them can be found in the horizon dashboard as seen in
Figure 2. In the same menu entry **Access & Security** as before, navigate to the tab
**API Access** (1) and click **View Credentials** (2).

![Figure 2](img/openstack-horizon-credentials.png)
*Figure 2: Horizon dashboard API access*

For a complete list of parameters needed see the [configuration file schema](../../docs/CONFIGURATION_SCHEMA.md).
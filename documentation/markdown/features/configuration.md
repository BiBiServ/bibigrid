# Configuration

> **Note**
> 
> First take a look at our [Hands-On BiBiGrid Tutorial](https://github.com/deNBI/bibigrid_clum2022) and our 
> example [bibigrid.yml](../../../bibigrid.yml).
> This documentation is no replacement for those, but provides greater detail - too much detail for first time users.

The configuration file (often called `bibigrid.yml`) contains important information about cluster creation.
The cluster configuration holds a list of configurations where each configuration has a specific 
cloud (location) and infrastructure (e.g. OpenStack). For single-cloud use cases you just need a single configuration.
However, you can use additional configurations to set up a multi-cloud.

The configuration file is best stored in `~/.config/bibigrid/`. BiBiGrid starts its relative search there.

## Configuration List
If you have a single-cloud use case, you can [skip ahead]().

Only the first configuration holds a `master` key (also called `master configuration`).
Every following configuration must hold a `vpngtw` key.

Later, this vpngtw allows BiBiGrid to connect multiple clouds.

[Here](multi_cloud.md) you can get a technical overview regarding BiBiGrid's multi-cloud setup. 

### General Cluster Information
Apart from the master key the master configuration (first configuration) also holds all information that is -
simply put - true over the entire cluster. We also call those keys `global`. Keys that belong only to a single cloud configuration are called `local`. 

For example whether the master works alongside the workers is a general fact (global).
Therefore, it is stored within the master configuration.

## Keys

### Global

#### sshPublicKeyFiles (optional)

`sshPublicKeyFiles` expects a list of public keyfiles to be registered on every instance. After cluster creation, you
or others can use the corresponding private key to log into the instances.

```yaml
sshPublicKeyFiles:
  - /home/user/.ssh/id_ecdsa_colleague.pub
```

#### autoMount (optional)
> **Warning:** If a volume has an obscure filesystem, this might overwrite your data!

If `True` all [masterMounts](#mastermounts-optional) will be automatically mounted by BiBiGrid if possible.
If a volume is not formatted or has an unknown filesystem, it will be formatted to `ext4`.
Default `False`.

#### masterMounts (optional)

`masterMounts` expects a list of volumes and snapshots. Those will be attached to the master. If any snapshots are
given, volumes are first created from them. Volumes are not deleted after Cluster termination.

<details>
<summary>
 What is mounting?
</summary>

[Mounting](https://man7.org/linux/man-pages/man8/mount.8.html) adds a new filesystem to the file tree allowing access.
</details>

#### nfsShares (optional)

`nfsShares` expects a list of folder paths to share over the network using nfs. 
In every case, `/vol/spool/` is always an nfsShare.

This key is only relevant if the [nfs key](#nfs-optional) is set `True`.

If you would like to share a [masterMount](#mastermounts-optional), take a look [here](../software/nfs.md#mount-volume-into-share).

<details>
<summary>
What is NFS?
</summary>

NFS (Network File System) is a stable and well-functioning network protocol for exchanging files over the local network.
</details>

#### ansibleRoles (optional)

Yet to be explained and implemented.

```yaml
  - file: SomeFile
    hosts: SomeHosts
    name: SomeName
    vars: SomeVars
    vars_file: SomeVarsFile
```

#### ansibleGalaxyRoles (optional)

Yet to be explained and implemented.

```yaml
  - hosts: SomeHost
    name: SomeName
    galaxy: SomeGalaxy
    git: SomeGit
    url: SomeURL
    vars: SomeVars
    vars_file: SomeVarsFile
```

#### localFS (optional)

In general, this key is ignored.
It expects `True` or `False` and helps some specific users to create a filesystem to their liking. Default is `False`.

#### localDNSlookup (optional)

If `True`, master will store DNS information for his workers. Default is `False`.
[More information](https://helpdeskgeek.com/networking/edit-hosts-file/).

#### slurm
If `False`, the cluster will start without the job scheduling system slurm.
This is relevant to the fewest. Default is `True`.

#### zabbix (optional)

If `True`, the monitoring solution [zabbix](https://www.zabbix.com/) will be installed on the master. Default is `False`.

#### nfs (optional)

If `True`, [nfs](../software/nfs.md) is set up. Default is `False`.

#### ide (optional)

If `True`, [Theia Web IDE](../software/theia_ide.md) is installed.
After creation connection information is [printed](../features/create.md#prints-cluster-information).

#### useMasterAsCompute (optional)

If `False`, master will no longer help workers to process jobs. Default is `True`.

#### useMasterWithPublicIP (optional)

If `False`, master will not be created with an attached floating ip. Default is `True`.

#### waitForServices (optional):

Expects a list of services to wait for.
This is required if your provider has any post-launch services interfering with the package manager. If not set,
seemingly random errors can occur when the service interrupts ansible's execution. Services are
listed on [de.NBI Wiki](https://cloud.denbi.de/wiki/) at `Computer Center Specific` (not yet).

#### 
In order to save valuable floating ips, BiBiGrid can also make use of a gateway to create the cluster.
For more information on how to set up a gateway, how gateways work and why they save floating ips please continue reading [here](https://cloud.denbi.de/wiki/Tutorials/SaveFloatingIPs/).

BiBiGrid needs the gateway-ip and a function that maps ips of nodes behind the gateway (private nodes) to the port over which you can connect to said node over the gateway.

In the example below the gateway-ip is 123.123.123.42 (ip of the gateway node) and the port function is 30000 + oct4.
Hereby, Oct4 stands for the fourth octet of the private node's ip (the last element). You can use your own custom port function
using all octets if needed. <br>
A private node with ip "123.123.123.12" is reachable over 123.123.123.42:30012 (because the fourth octet is 12).
```yaml
gateway:
    ip: 123.123.123.42 # IP of gateway to use
    portFunction: 30000 + oct4 # variables are called: oct1.oct2.oct3.oct4
```

Using gateway also automatically sets [useMasterWithPublicIp](#usemasterwithpublicip-optional) to `False`.

### Local

#### infrastructure (required)

`infrastructure` sets the used provider implementation for this configuration. Currently only `openstack` is available.
Other infrastructures would be [AWS](https://aws.amazon.com/) and so on.

#### cloud

`cloud` decides which entry in the `clouds.yaml` is used. When using OpenStack the entry is named `openstack`.
You can read more about the `clouds.yaml` [here](cloud_specification_data.md).

#### workerInstances (optional)

`workerInstances` expects a list of worker groups (instance definitions with `count` key).
If `count` is omitted, `count: 1` is assumed. 

```yaml
workerInstance:
  - type: de.NBI tiny
    image: Ubuntu 22.04 LTS (2022-10-14)
    count: 2
```

- `type` sets the instance's hardware configuration.
- `image` sets the bootable operating system to be installed on the instance.
- `count` sets how many workers of that `type` `image` combination are in this work group

##### Find your active `images`

```commandline
openstack image list --os-cloud=openstack | grep active
```

Currently, images based on Ubuntu 20.04/22.04 (Focal/Jammy) and Debian 11(Bullseye) are supported.

###### Using Regex
Instead of using a specific image you can also provide a regex.
For example if your images are named by following the pattern `Ubuntu 22.04 LTS ($DATE)` and on ly the 
most recent release is active, you can use `Ubuntu 22.04 LTS \(.*\)` so it always picks the right one.

This regex will also be used when starting worker instances on demand
and is therefore mandatory to automatically resolve image updates of the described kind while running a cluster.

There's also a [Fallback Option](#fallbackonotherimage-optional).

##### Find your active `type`s
`flavor` is just the OpenStack terminology for `type`.

```commandline
openstack flavor list --os-cloud=openstack
```

##### features (optional)
You can declare a list of features for a worker group. Those are then attached to each node in the worker group.
For example:
```yaml
workerInstance:
  - type: de.NBI tiny
    image: Ubuntu 22.04 LTS (2022-10-14)
    count: 2
    features:
      - hasdatabase
      - holdsinformation
```

###### What's a feature?
Features allow you to force Slurm to schedule a job only on nodes that meet a certain `bool` constraint.
This can be helpful when only certain nodes can access a specific resource - like a database.

If you would like to know more about how features exactly work, 
take a look at [slurm's documentation](https://slurm.schedmd.com/slurm.conf.html#OPT_Features).

#### Master or vpngtw?

##### masterInstance

Only in the first configuration and only one:

```yaml
  masterInstance:
    type: de.NBI tiny
    image: Ubuntu 22.04 LTS (2022-10-14)
```

You can create features for the master [in the same way](#features-optional) as for the workers:

```yaml
  masterInstance:
    type: de.NBI tiny
    image: Ubuntu 22.04 LTS (2022-10-14) # regex allowed
    features:
      - hasdatabase
      - holdsinformation
```

##### vpngtw:

Exactly one in every configuration but the first:

```yaml
  vpngtw:
    type: de.NBI tiny
    image: Ubuntu 22.04 LTS (2022-10-14) # regex allowed
```

### fallbackOnOtherImage (optional)
If set to `true` and an image is not among the active images, 
BiBiGrid will try to pick a fallback image for you by finding the closest active image by name that has at least 60% name overlap.
This will not find a good fallback every time.

You can also set `fallbackOnOtherImage` to a regex like `Ubuntu 22.04 LTS \(.*\)` in which case BiBiGrid will pick an
active image matching that regex.
This can be combined with the regular regex option from the [image key](#find-your-active-images).
In that case the fallback regex should be more open to be still useful when the original regex failed to find an active image.

This fallback will also be used when starting worker instances on demand
and can be helpful to when image updates occur while running a cluster.

#### sshUser (required)

`sshUser` is the standard user of the installed images. For `Ubuntu 22.04` this would be `ubuntu`.

#### region (required)

Every [region](https://docs.openstack.org/python-openstackclient/rocky/cli/command-objects/region.html) has its own
openstack deployment. Every [avilability zone](#availabilityzone-required) belongs to a region.

Find your `regions`:

```commandline
openstack region list --os-cloud=openstack
```

#### availabilityZone (required)

[availability zones](https://docs.openstack.org/nova/latest/admin/availability-zones.html) allow to logically group
nodes.

Find your `availabilityZones`:

```commandline
openstack region list --os-cloud=openstack
```

#### subnet (required)

`subnet` is a block of ip addresses.

Find available `subnets`:

```commandline
openstack subnet list --os-cloud=openstack
```

#### localDNSLookup (optional)

If no full DNS service for started instances is available, set `localDNSLookup: True`.
Currently, the case in Berlin, DKFZ, Heidelberg and Tuebingen.

#### features (optional)

You can declare a list of [features](#whats-a-feature) that are then attached to every node in the configuration.
If both [worker group](#features-optional) or [master features](#masterInstance) and configuration features are defined, 
they are merged.
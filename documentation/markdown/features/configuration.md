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
Every following configuration must hold a `vpnwkr` key.

Later, this vpnwkr allows BiBiGrid to connect multiple clouds.

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

#### zabbix (optional)

If `True`, the monitoring solution [zabbix](https://www.zabbix.com/) will be installed on the master. Default is `False`.

#### nfs (optional)

If `True`, [nfs](../software/nfs.md) is set up. Default is `False`.

#### useMasterAsCompute (optional)

If `False`, master will no longer help workers to process jobs. Default is `True`.

#### waitForServices (optional):

Expects a list of services to wait for.
This is required if your provider has any post-launch services interfering with the package manager. If not set,
seemingly random errors can occur when the service interrupts ansible's execution. Services are
listed on [de.NBI Wiki](https://cloud.denbi.de/wiki/) at `Computer Center Specific` (not yet).

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

#### Master or vpnWorker?

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
    image: Ubuntu 22.04 LTS (2022-10-14)
    features:
      - hasdatabase
      - holdsinformation
```

##### vpnWorker:

Exactly one in every configuration but the first:

```yaml
  vpnWorker:
    type: de.NBI tiny
    image: Ubuntu 22.04 LTS (2022-10-14)
```

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
Currently the case in Berlin, DKFZ, Heidelberg and Tuebingen.

#### features (optional)

You can declare a list of [features](#whats-a-feature) that are then attached to every node in the configuration.
If both [worker group](#features-optional) or [master features](#masterInstance) and configuration features are defined, 
they are merged.
# Configuration

The configuration file (often called `bibigrid.yml`) contains important information about cluster creation.
The cluster configuration holds a list of configurations where each configuration is assigned to a specific provider 
(location). That allows cluster to stretch over multiple providers. The configuration file is best stored in
`~/.config/bibigrid/` since BiBiGrid starts its relative search there.

## Configuration List
The first configuration is always the master's provider configuration.
Only the first configuration is allowed to have a master key.
Every following configuration describes a provider that is not the master's provider containing a number of worker and a
vpnwkr (vpn worker). The vpnwkr is a worker with a floating IP. That allows the master - that knows all vpnwkrs to access
all workers using the floating IP as an entry point into the other local networks. However, all that will be covered by
an abstraction layer using a virtual network. Therefore, end users can work on a spread cluster without noticing it.

### Master Provider Configuration
As mentioned before, the first configuration has a master key. Apart from that it also holds all information that is -
simply put - true over the entire cluster. We also call those keys global. 
Keys that belong only to a single provider configuration are called local.
For example whether the master works alongside the workers is a general fact.
Therefore, it is stored within the first configuration. The master provider configuration.

## Keys

### Global

#### sshPublicKeyFiles (optional)
`sshPublicKeyFiles` expects a list of public keyfiles to be registered on every node. That allows you to grant access to
created clusters to the owners of the private keyfile. For example, you can add colleges public key to the list and allow
him to access your started cluster later on to debug it.

#### masterMounts (optional)
`masterMounts` expects a list of volumes or snapshots that will then be attached to the master. If any snapshots are
given, the related volumes are first created and then those volumes are used by BiBiGrid. Those volumes are not deleted
after Cluster termination.

[[Link to mounting infomation]] #ToDo

<details>
<summary>
 What is mounting?
</summary>

[Mounting](https://man7.org/linux/man-pages/man8/mount.8.html) adds a new filesystem to the file tree allowing access.

</details>



#### nfsShares (optional)
`nfsShares` expects a list of folder paths to share using nfs. In every case, `/vol/spool/` is always an nfsShare.
This key is only relevant if the [nfs key](#nfs--optional-) is set `True`.

<details>
<summary>
What is NFS?
</summary>

NFS (Network File System) is a stable and well-functioning network protocol for exchanging files over the local network.
</details>

#### ansibleRoles (optional)
Yet to be explained.
```
  - file: SomeFile
    hosts: SomeHosts
    name: SomeName
    vars: SomeVars
    vars_file: SomeVarsFile
```
#### ansibleGalaxyRoles (optional)
Yet to be explained.
```
  - hosts: SomeHost
    name: SomeName
    galaxy: SomeGalaxy
    git: SomeGit
    url: SomeURL
    vars: SomeVars
    vars_file: SomeVarsFile
```

#### localFS (optional)
This key helps some users to create a filesystem to their liking. It is not used in general.

#### localDNSlookup (optional)
If `True`, master will store the link to his workers. This is called 
[Local DNS Lookup](https://helpdeskgeek.com/networking/edit-hosts-file/).

#### zabbix (optional)
If `True`, the monitoring solution [zabbix](https://www.zabbix.com/) will be installed on the master.

#### nfs (optional)
If `True`, nfs is created.

<details>
<summary>
What is NFS?
</summary>

NFS (Network File System) is a stable and well-functioning network protocol for exchanging files over the local network.
</details>

#### useMasterAsCompute (optional)
Default the master always works together with the workers on submitted jobs. If you set `useMasterWithPublicIp`
 to `False` the master will instead no longer support the workers.

#### waitForServices (optional):
Expects a list of services to wait for. This is required if your provider has any post-launch services. If not set,
seemingly random errors can occur when the service interrupts the ansible execution. Providers and their services are
listed on [de.NBI Wiki](https://cloud.denbi.de/wiki/) at `Computer Center Specific`.

### Local

#### infrastructure (required)
`infrastructure` sets the used provider implementation for this configuration. Currently only `openstack` is available.
Other infrastructures would be AWS and so on.

#### cloud
`cloud` decides which entry in the `clouds.yaml` is used. 
When using OpenStack the downloaded `clouds.yaml` is named `openstack`

`cloud: openstack`

####  workerInstances (optional)
`workerInstances` expects a list of workers to be used on this specific provider the configuration is for.
`Instances` are also called `servers`.

```
workerInstance:
  - type: de.NBI tiny
    image: Ubuntu 22.04 LTS (2022-10-14)
    count: 2
```
- `type` sets the instance's hardware configuration. Also called `flavor` sometimes.
- `image` sets the bootable operating system to be installed on the instance.
- `count` sets how many workers of that `type` `image` combination are to be used by the cluster

Find your active `images`:

```
openstack image list --os-cloud=openstack | grep active
```

Currently, images based on Ubuntu 20.04/22.04 (Focal/Jammy) and Debian 11(Bullseye) are supported. 

Find your active `flavors`:

```
openstack flavor list --os-cloud=openstack
```

#### Master or vpnWorker?

##### Master
Only in the first configuration and only one:
```
  masterInstance:
    type: de.NBI tiny
    image: Ubuntu 22.04 LTS (2022-10-14)
```

##### vpnWorker:
Exactly once in every configuration but the first:
```
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
```
openstack region list --os-cloud=openstack
```


#### availabilityZone (required)
[availability zones](https://docs.openstack.org/nova/latest/admin/availability-zones.html) allow to logically group
nodes.

Find your `availabilityZones`:
```
openstack region list --os-cloud=openstack
```

#### subnet (required)
`subnet` is a block of ip addresses.

Find available `subnets`:

```
openstack subnet list --os-cloud=openstack
```

#### localDNSLookup (optional)
If no full DNS service for started instances is available, set `localDNSLookup: True`.
Currently the case in Berlin, DKFZ, Heidelberg and Tuebingen.
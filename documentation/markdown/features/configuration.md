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

#### sshTimeout (optional:5)
Defines the number of attempts that BiBiGrid will try to connect to the master instance via ssh.
Attempts have a pause of `2^(attempts+2)` seconds in between. Default value is 5.

#### customAnsibleCfg (optional:False)
When False, changes in the resources/playbook/ansible.cfg are overwritten by the create action. 
When True, changes are kept - even when you perform a git pull as the file is not tracked. The default can be found at
resources/default/ansible/ansible.cfg.

#### customSlurmTemplate (optional:False)
When False, changes in the resources/playbook/roles/bibigrid/templates/slurm.j2 are overwritten by the create action. 
When True, changes are kept - even when you perform a git pull as the file is not tracked. The default can be found at
resources/default/slurm/slurm.j2.

#### cloudScheduling (optional:5)
This key allows you to influence cloud scheduling. Currently, only a single key `sshTimeout` can be set here. Default is 5.

##### sshTimeout (optional:5)
Defines the number of attempts that the master will try to connect to on demand created worker instances via ssh.
Attempts have a pause of `2^(attempts+2)` seconds in between. Default value is 5.

```yaml
cloudScheduling:
  sshTimeout: 5
```

#### masterMounts (optional:False)

`masterMounts` expects a list of volumes and snapshots. Those will be attached to the master. If any snapshots are
given, volumes are first created from them. Volumes are not deleted after Cluster termination.

```yaml
masterMounts:
    - name: test # name of the volume to be attached
      mountPoint: /vol/spool2 # where attached volume is to be mount to (optional)
```

`masterMounts` can be combined with [nfsshares](#nfsshares-optional).
The following example attaches volume test to our master instance and mounts it to `/vol/spool2`.
Then it creates an nfsshare on `/vol/spool2` allowing workers to access the volume test.

```yaml
masterMounts:
  - name: test # name of the volume to be attached
    mountPoint: /vol/spool2 # where attached volume is to be mount to (optional)

nfsshares:
  - /vol/spool2
```

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

#### userRoles (optional)

`userRoles` takes a list of elements containing the keys `hosts`, `roles` and  

```yaml
userRoles: # see ansible_hosts for all options
    - hosts: 
        - "master"
      roles: # roles placed in resources/playbook/roles_user
        - name: "resistance_nextflow"
          tags:
            - resistance_nextflow
      # varsFiles: # (optional)
      #  - file1
```

#### localFS (optional:False)

In general, this key is ignored.
It expects `True` or `False` and helps some specific users to create a filesystem to their liking. Default is `False`.

#### localDNSlookup (optional:False)

If `True`, master will store DNS information for his workers. Default is `False`.
[More information](https://helpdeskgeek.com/networking/edit-hosts-file/).

#### slurm (optional:True)
If `False`, the cluster will start without the job scheduling system slurm.
For nearly all cases the default value is what you need. Default is `True`.

##### slurmConf (optional)
`slurmConf` contains variable fields in the `slurm.conf`. The most common use is to increase the `SuspendTime`, 
`SuspendTimeout` and the `ResumeTimeout` like:

```yaml
elastic_scheduling:
  SuspendTime: 1800
  SuspendTimeout: 60
  ResumeTimeout: 1800
```

Increasing the `SuspendTime` should only be done with consideration for other users. 
On Demand Scheduling improves resource availability for all users.
If some nodes need to be active during the entire cluster lifetime, [onDemand](#workerinstances) might be the better approach.

###### Defaults
```yaml
slurmConf:
    db: slurm # see task 042-slurm-server.yml
    db_user: slurm
    db_password: changeme
    munge_key: # automatically generated via id_generation.generate_munge_key
    elastic_scheduling:
      SuspendTime: 900  # if a node is not used for SuspendTime seconds, it will shut down  
      SuspendTimeout: 30 # after SuspendTimeout seconds, slurm allows to power up the powered down node again
      ResumeTimeout: 900 # if a node doesn't start in ResumeTimeout seconds, the start is considered failed. See https://slurm.schedmd.com/slurm.conf.html#OPT_ResumeProgram
      TreeWidth: 128 # https://slurm.schedmd.com/slurm.conf.html#OPT_TreeWidth
```

#### zabbix (optional:False)

If `True`, the monitoring solution [zabbix](https://www.zabbix.com/) will be installed on the master. Default is `False`.

#### nfs (optional:False)

If `True`, [nfs](../software/nfs.md) is set up. Default is `False`.

#### ide (optional:False)

If `True`, [Theia Web IDE](../software/theia_ide.md) is installed.
After creation connection information is [printed](../features/create.md#prints-cluster-information). Default is `False`.

#### useMasterAsCompute (optional:True)

If `False`, master will no longer help workers to process jobs. Default is `True`.

#### useMasterWithPublicIP (optional:True)

If `False`, master will not be created with an attached floating ip. Default is `True`.

#### gateway (optional)
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

#### waitForServices (optional):

Expects a list of services to wait for.
This is required if your provider has any post-launch services interfering with the package manager. If not set,
seemingly random errors can occur when the service interrupts ansible's execution. Services are
listed on [de.NBI Wiki](https://cloud.denbi.de/wiki/) at `Computer Center Specific` (not yet).

#### infrastructure (required)

`infrastructure` sets the used provider implementation for this configuration. Currently only `openstack` is available.
Other infrastructures would be [AWS](https://aws.amazon.com/) and so on.

#### cloud (required)

`cloud` decides which entry in the `clouds.yaml` is used. When using OpenStack the entry is named `openstack`.
You can read more about the `clouds.yaml` [here](cloud_specification_data.md).

#### workerInstances

`workerInstances` expects a list of worker groups (instance definitions with `count` key).
If `count` is omitted, `count: 1` is assumed. 

```yaml
workerInstance:
  - type: de.NBI tiny
    image: Ubuntu 22.04 LTS (2022-10-14)
    count: 2
    onDemand: True # optional only on master cloud for now. Default True.
    partitions: # optional. Always adds "all" and the cloud identifier as partitions
      - small
      - onDemand
    features: # optional
      - hasdatabase
      - holdsinformation
```

- `type` sets the instance's hardware configuration.
- `image` sets the bootable operating system to be installed on the instance.
- `count` sets how many workers of that `type` `image` combination are in this work group
- `onDemand` defines whether nodes in the worker group are scheduled on demand (True) or are started permanently (False). Please only use if necessary. On Demand Scheduling improves resource availability for all users. This option only works for single cloud setups for now.
- `partitions` allow you to force Slurm to schedule to a group of nodes (partitions) ([more](https://slurm.schedmd.com/slurm.conf.html#SECTION_PARTITION-CONFIGURATION))
- `features` allow you to force Slurm to schedule a job only on nodes that meet certain `bool` constraints. This can be helpful when only certain nodes can access a specific resource - like a database ([more](https://slurm.schedmd.com/slurm.conf.html#OPT_Features)).

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

### fallbackOnOtherImage (optional:False)
If set to `True` and an image is not among the active images, 
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

#### subnet (required)

`subnet` is a block of ip addresses.

Find available `subnets`:

```commandline
openstack subnet list --os-cloud=openstack
```

#### localDNSLookup (optional:False)

If no full DNS service for started instances is available, set `localDNSLookup: True`.
Currently, the case in Berlin, DKFZ, Heidelberg and Tuebingen.

#### features (optional)

You can declare a list of cloud-wide [features](#whats-a-feature) that are then attached to every node in the cloud described by the configuration.
If both [worker group](#workerinstances) or [master features](#masterInstance) and configuration features are defined, 
they are merged. If you only have a single cloud and therefore a single configuration, this key is not helpful as a feature
that is present at all nodes can be omitted as it can't influence the scheduling.
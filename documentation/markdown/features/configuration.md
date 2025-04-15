# Configuration

> **Note**
> 
> First take a look at our [Hands-On BiBiGrid Tutorial](https://github.com/deNBI/bibigrid_clum2022) and our 
> example [bibigrid.yaml](../../../bibigrid.yaml).
> This documentation is no replacement for those, but provides greater detail - too much detail for first time users.

The configuration file (often called `bibigrid.yaml`) contains important information about cluster creation.
The cluster configuration holds a list of configurations where each configuration has a specific 
cloud (location) and infrastructure (e.g. OpenStack). For single-cloud use cases you just need a single configuration.
However, you can use additional configurations to set up a multi-cloud.

The configuration file is best stored in `~/.config/bibigrid/`. BiBiGrid starts its relative search there.

## Configuration List
If you have a single-cloud use case, you can [skip ahead]().

Only the first configuration holds a `masterInstance` key (also called `master configuration`).
Every following configuration must hold a `vpnInstance` key.

Later, this vpnInstance allows BiBiGrid to connect multiple clouds.

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

#### nfsShares (optional)

`nfsShares` expects a list of folder paths to share over the network using nfs. 
In every case, `/vol/spool/` is always an nfsShare.

This key is only relevant if the [nfs key](#nfs-optionalfalse) is set `True`.

If you would like to share a [masterMount](#mastermounts-optionalfalse), take a look [here](../software/nfs.md#mount-volume-into-share).

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

#### Usually Ignored Keys
##### localFS (optional:False)

In general, this key is ignored.
It expects `True` or `False` and helps some specific users to create a filesystem to their liking. Default is `False`.

##### localDNSlookup (optional:False)

If no full DNS service for started instances is available, set `localDNSLookup: True`.
Currently, the case in Berlin, DKFZ, Heidelberg and Tuebingen.
Given that we use dnsmasq, this might be obsolete.

#### slurm (optional:True)
If `False`, the cluster will start without the job scheduling system slurm.
For nearly all cases the default value is what you need. Default is `True`.

##### slurmConf (optional)
`slurmConf` contains variable fields in the `slurm.conf`. The most common use is to increase the `SuspendTime`, 
`SuspendTimeout` and the `ResumeTimeout` like:

```yaml
elastic_scheduling:
  SuspendTime: 1800
  SuspendTimeout: 90
  ResumeTimeout: 1800
```

Increasing the `SuspendTime` should only be done with consideration for other users. 
On Demand Scheduling improves resource availability for all users.
If some nodes need to be active during the entire cluster lifetime, [onDemand](#workerinstances) might be the better approach.

###### Defaults
```yaml
slurmConf:
    db: slurm # see task 042-slurm-server.yaml
    db_user: slurm
    db_password: changeme
    munge_key: # automatically generated via id_generation.generate_munge_key
    elastic_scheduling:
      SuspendTime: 900  # if a node is not used for SuspendTime seconds, it will shut down  
      SuspendTimeout: 60 # after SuspendTimeout seconds, slurm allows to power up the powered down node again
      ResumeTimeout: 900 # if a node doesn't start in ResumeTimeout seconds, the start is considered failed. See https://slurm.schedmd.com/slurm.conf.html#OPT_ResumeProgram
      TreeWidth: 128 # https://slurm.schedmd.com/slurm.conf.html#OPT_TreeWidth
```

#### zabbix (optional:False)

If `True`, the monitoring solution [zabbix](https://www.zabbix.com/) will be installed on the master. Default is `False`.

#### nfs (optional:False)

If `True`, [nfs](../software/nfs.md) is set up. Default is `False`.

#### ide (optional:False)

If `True`, [Theia Web IDE](../software/theia_ide.md) is installed.
After creation connection information is [printed](../features/create.md). Default is `False`.

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

Using gateway also automatically sets [useMasterWithPublicIp](#usemasterwithpublicip-optionaltrue) to `False`.

#### dontUploadCredentials (optional:True)
Usually, BiBiGrid will upload your credentials to the cluster. This is necessary for on demand scheduling.
However, if all your nodes are permanent (i.e. not on demand), you do not need to upload your credentials.
In such cases you can set `dontUploadCredentials: True`.

This also allows for external node schedulers by using the Slurm REST API to decide whether a new node should be started or not.
[SimpleVM](https://cloud.denbi.de/about/project-types/simplevm/) is scheduling that way.

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
    onDemand: True # optional only on master cloud for now and only for workers. Default True.
    partitions: # optional. Always adds "all" and the cloud identifier as partitions
      - small
      - onDemand
    features: # optional
      - hasdatabase
      - holdsinformation
    volumes: # optional
      - name: volumeName
        snapshot: snapshotName # optional; to create volume from
        # one or none of these
        # permanent: False
        # semiPermanent: False
        # exists: False
        mountPoint: /vol/test
        size: 50
        fstype: ext4
        type: None
    bootVolume: # optional
      name: False
      terminate: True
      size: 50
    securityGroups: # optional
      - list of existing security groups
    meta: # optional (no key or value longer than 256)
      meta_key: meta_value
```

- `type` sets the instance's hardware configuration.
- `image` sets the bootable operating system to be installed on the instance.
- `count` sets how many workers of that `type` `image` combination are in this work group
- `onDemand` (optional:False) defines whether nodes in the worker group are scheduled on demand (True) or are started permanently (False). Please only use if necessary. On Demand Scheduling improves resource availability for all users. This option only works for single cloud setups for now.
- `partitions` (optional:[]) allow you to force Slurm to schedule to a group of nodes (partitions) ([more](https://slurm.schedmd.com/slurm.conf.html#SECTION_PARTITION-CONFIGURATION))
- `features` (optional:[]) allow you to force Slurm to schedule a job only on nodes that meet certain `bool` constraints. This can be helpful when only certain nodes can access a specific resource - like a database ([more](https://slurm.schedmd.com/slurm.conf.html#OPT_Features)).
- `bootVolume` (optional)
  - `name` (optional:None) takes name or id of a boot volume and boots from that volume if given.
  - `terminate` (optional:True) if True, the boot volume will be terminated when the server is terminated.
  - `size` (optional:50) if a boot volume is created, this sets its size.
- `volumes`
- `securityGroups` (optional:[]) a list of existing securityGroups that will be added to the instances
- `meta` a dict of meta key value pairs (no key or value longer than 256)
##### volumes (optional)

You can create a temporary volume (default), a semipermanent volume, a permanent volume and you can do all of those from a snapshot, too.
You can even attach a volume that already exists. However, don't try to add a single existing volume to a group with count >1 as most volumes can't be attached to more than one instance.

- **Semi-permanent** volumes are deleted once their cluster is destroyed not when their server is powered down during the cluster's runtime. By setting `semiPermanent: True`, you create a semi-permanent volume.
- **Permanent** volumes are deleted once you delete them manually. By setting `permanent: True`, you create a permanent volume.
- **Temporary** volumes are deleted once their server is destroyed. By setting `permanent: False` and `semiPermanent: False` (their default value), you create a temporary volume.
- **Existing** volumes can be attached by setting the exact name of that volume as `name` and setting `exists: True`. If you use this to attach the volume to a worker, make sure that the worker group's count is 1. Otherwise, BiBiGrid will try to attach that volume to each instance.
- You can create volumes from **snapshots** by setting `snapshot` to your snapshot's name. You can create all kinds of volumes of them.
- `type` allows you to set the storage option. For Bielefeld there are `CEPH_HDD` (HDD) and `CEPH_NVME` (SSD). 

Termination of these volumes is done by regex looking for the cluster id. For cluster termination: `^bibigrid-(master-{cluster_id}|(worker|vpngtw)-{cluster_id}-(\d+))-(semiperm|tmp)-\d+(-.+)?$`

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

There's also a [Fallback Option](#fallbackonotherimage-optionalfalse).

##### Find your active `type`s
`flavor` is just the OpenStack terminology for `type`.

```commandline
openstack flavor list --os-cloud=openstack
```

#### masterInstance or vpnInstance?

##### masterInstance

Only in the first configuration and only one:

```yaml
  masterInstance:
    type: de.NBI tiny
    image: Ubuntu 22.04 LTS (2022-10-14)
    # ... (see workerInstance)
```

You can apply most keys [in the same way](#features-optional) as for the workers for example create features:

```yaml
  masterInstance:
    type: de.NBI tiny
    image: Ubuntu 22.04 LTS (2022-10-14) # regex allowed
    features:
      - hasdatabase
      - holdsinformation
```

- `type` sets the instance's hardware configuration.
- `image` sets the bootable operating system to be installed on the instance.
- `partitions` (optional:[]) allow you to force Slurm to schedule to a group of nodes (partitions) ([more](https://slurm.schedmd.com/slurm.conf.html#SECTION_PARTITION-CONFIGURATION))
- `features` (optional:[]) allow you to force Slurm to schedule a job only on nodes that meet certain `bool` constraints. This can be helpful when only certain nodes can access a specific resource - like a database ([more](https://slurm.schedmd.com/slurm.conf.html#OPT_Features)).
- `bootVolume` (optional)
  - `name` (optional:None) takes name or id of a boot volume and boots from that volume if given.
  - `terminate` (optional:True) if True, the boot volume will be terminated when the server is terminated.
  - `size` (optional:50) if a boot volume is created, this sets its size.
- `volumes`
- `securityGroups` (optional:[]) a list of existing securityGroups that will be added to the instances
- `meta` a dict of meta key value pairs (no key or value longer than 256)

##### vpnInstance:

Exactly one in every configuration but the first:

```yaml
  vpnInstance:
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

#### features (optional)

Cloud-wide slurm features that are attached to every node in the cloud described by the configuration.
If both [worker group](#workerinstances) or [master features](#masterInstance) and configuration features are defined, 
they are merged. If you only have a single cloud and therefore a single configuration, this key is not helpful as a feature
that is present at all nodes can be omitted as it can't influence the scheduling.

#### bootVolume (optional)

Instead of setting the `bootVolume` for every instance you can also set it cloud wide:

- `bootVolume` (optional)
  - `name` (optional:None) takes name or id of a boot volume and boots from that volume if given.
  - `terminate` (optional:True) if True, the boot volume will be terminated when the server is terminated.
  - `size` (optional:50) if a boot volume is created, this sets its size.

```yaml
bootVolume:
      name: False
      terminate: True
      size: 50
```

#### securityGroups (optional)

Instead of setting the `securityGroups` for every instance you can also set them cloud wide as a list:

```yaml
securityGroups:
  - securityGroup1
  - securityGroup2 
```
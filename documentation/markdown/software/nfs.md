# Network File System (NFS)

NFS is used as an abstraction layer to allow users to work naturally across file systems.
In a cluster setup working across file systems is really important when multiple nodes work on the same data.

Most BiBiGrid users will never really interact consciously with BiBiGrid's underlying NFS, but simply use it.

## How To Configure NFS Shares?

When starting an ansible cluster, at least `/vol/spool` is initialised as an NFS share if the key
[nfs](../features/configuration.md#nfs--optional-) is `True`.
Further NFS shares can then be configured using configuration's
[nfsshares](../features/configuration.md#nfsshares--optional-) key.

### Manually Creating NFS Shares

We discourage bypassing BiBiGrid's [configuration](../features/configuration.md#nfsshares--optional-) by creating
additional NFS shares manually, because they will not be automatically registered by scheduled workers.

## Useful Commands

|       Summary       |        Command        | Explanation & Comment |
|:-------------------:|:---------------------:|:---------------------:|
| List all nfs shares | `showmount --exports` |                       |

### NFS commands

See [nfs' manpage](https://man7.org/linux/man-pages/man5/nfs.5.html).

## How To Share an Attached Volume

By mounting a volume into a shared directory, volumes can be shared.

### Configuration

Let's assume our configuration holds (among others) the keys:

```yaml
nfs: True
masterMounts:
  - testMount

nfsShares:
  - testShare
```

Where `testMount` is an existing, formatted volume with a filesystem type (for example: ext4, ext3, ntfs, ...).

During cluster creation...

1. BiBiGrid sets up the nfsShare `/testShare`.
2. BiBiGrid attached the volume `testMount` to our master instance. The volume is not mounted yet.

3. We call the cluster `bibigrid-master-ournfsclusterid` in the following.

### Mounting a Volume Into a Shared Directory

In order to mount a volume into a shared directory, we first need to identify where our volume was attached.

#### Find Where Volume Has Been Attached

Executing this openstack client command will give us a list of volumes.
Most likely it is best run from your local machine.

```sh
openstack volume list --os-cloud=openstack
```

Result:

|                  ID                  |          Name |  Status   | Size |                       Attached to                       |
|:------------------------------------:|--------------:|:---------:|:----:|:-------------------------------------------------------:|
| 42424242-4242-4242-4242-424242424242 | notTestVolume | available |  X   |                                                         |
| 42424242-4242-4242-4242-424242424221 |     testMount |  in-use   |  Y   | Attached to bibigrid-master-ournfsclusterid on /dev/vdd |

As you can see, the volume `testMount` was attached to `/dev/vdd`.
We can double-check whether `/dev/vdd` really exists by executing `lsblk` or `lsblk | grep /dev/vdd` on the master.

#### Mount Volume into Share

As our NFS share is `/testShare`, we now need to mount `dev/vdd` into `testShare`:

```sh
sudo mount -t auto /dev/vdd /testShare
```

The volume `testMount` is now successfully shared using NFS.
Workers can access the volume now by using `/testShare`, too.

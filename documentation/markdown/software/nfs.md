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

|        Summary         |        Command         |                 Explanation & Comment                  |
|:----------------------:|:----------------------:|:------------------------------------------------------:|
|  List all nfs shares   | `showmount --exports`  |                                                        |

### NFS commands
See [nfs' manpage](https://man7.org/linux/man-pages/man5/nfs.5.html).

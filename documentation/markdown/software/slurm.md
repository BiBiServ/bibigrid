# Slurm

## Slurm Client

### Useful commands

For more options see [slurm client's manpage](https://manpages.debian.org/testing/slurm-client/slurm-wlm.1).

|        Summary         |                                   Command                                    |                                                                            Explanation & Comment                                                                            |
|:----------------------:|:----------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
| List all present nodes |                                   `sinfo`                                    | Cloud nodes that are powered down are marked`~`. Knowing [Node State Codes](https://manpages.debian.org/testing/slurm-client/sinfo.1.en.html#NODE_STATE_CODES) helps a lot. |
|  Shutdown an instance  | `sudo scontrol update NodeName=[node-name] state=POWER_DOWN reason=[reason]` |                                                             Powers down the node. The instance will be deleted.                                                             |
|  Powerup an instance   |  `sudo scontrol update NodeName=[node-name] state=POWER_UP reason=[reason]`  |                                                              Powers up the node. An instance will be created.                                                               |
| Lists all running jobs |                                   `squeue`                                   |                                                           Allows you to see whether everything runs as expected.                                                            |

### Read more

|                                      Summary                                      |                 Explanation                  |
|:---------------------------------------------------------------------------------:|:--------------------------------------------:|
| [NODE STATE CODES](https://slurm.schedmd.com/sinfo.html#SECTION_NODE-STATE-CODES) | Very helpful to interpret `sinfo` correctly. |

## REST API

BiBiGrids configures Slurm's REST API Daemon listening on `0.0.0.0:6420`.

Get token for user slurm

```shell
$ scontrol token -u slurm
SLURM_JWT=eyJhbGc...
```

Get openapi specification

```shell
$ token=eyJhbGc...
$ user=slurm
$ curl -H "X-SLURM-USER-NAME: $user" -H "X-SLURM-USER-TOKEN: $token" localhost:6820/openapi
...
```

### Read more

|                                  Summary                                   |             Explanation              |
|:--------------------------------------------------------------------------:|:------------------------------------:|
|           [Slurm REST API](https://slurm.schedmd.com/rest.html)            |     Slurm REST API documentation     |
| [JSON Web Tokens (JWT) Authentication](https://slurm.schedmd.com/jwt.html) | Helpful to understand JWT/SlurmRestD |

## Slurm Packages

You may have noticed that BiBiGrid doesn't use Slurm packages provided by the supported operating systems.
To be independent of the distributions release cycle we decided to build Slurm by ourselves. For those
who want to run a specific Slurm version the following documentation might be helpful.

### Prepare build system

At time of writing Slurm, 22.05.7 was the latest version available. Debian 11, Ubuntu 20.04/22.04
as build system were successfully tested.

```
$ apt install tmux git build-essential vim curl

$ mkdir build
$ cd build
 
$ curl https://download.schedmd.com/slurm/slurm-22.05.7.tar.bz2 --output slurm-22.05.7.tar.bz2
$ tar -xjf slurm-22.05.7.tar.bz2
```

### Install build dependencies

To enable source code repositories uncomment the lines starting with deb-src running :

```shell
$ sed -i.bak 's/^# *deb-src/deb-src/g' /etc/apt/sources.list && \
apt-get update
```

With source repositories enabled install build dependencies of slurm-wlm.

```shell
$ apt build-dep -y slurm-wlm
```

and for Ubuntu 20.04 only additionally

```shell
$ apt install libyaml-dev libjson-c-dev libhttp-parser-dev libjwt-dev
```

To make use of [Control Group v2](https://slurm.schedmd.com/cgroup_v2.html) the development
files from dBus API must be installed additionally.

```shell
apt install libdbus-1-dev
```

### Build slurm

Building slurm is now an easy job.

```shell
$ ./configure --prefix=/usr --sysconfdir=/etc/slurm --with-systemdsystemunitdir=/lib/systemd/system

$ make -j 8
```

### Create deb package

#### Determine dependencies

```shell
$ python3 get_deb_dependencies.py --checkinstall --substr slurm slurm-wlm
libc6 \(\>= 2.29\), libhwloc15 \(\>= 2.1.0+dfsg\), liblz4-1 \(\>= 0.0~r130\), ...
```

#### Run Checkinstall

Checkinstall makes it easy to create a debian package:

```shell
$ checkinstall --type=debian --install=no --fstrans=no \
--pkgname=slurm-full \
--pkgversion=22.05.7 \
--pkgrelease=1 \
--pkglicense=GPL \
--maintainer=jkrueger@cebitec.uni-bielefeld.de \
--replaces=slurm-wlm \
--requires="libc6 \(\>= 2.29\), libhwloc15 \(\>= 2.1.0+dfsg\), liblz4-1 \(\>= 0.0~r130\), libnuma1 \(\>= 2.0.11\), libpam0g \(\>= 0.99.7.1\), zlib1g \(\>= 1:1.2.0\), libcurl4 \(\>= 7.16.2\), libfreeipmi17 \(\>= 1.4.4\), libhdf5-103, libipmimonitoring6 \(\>= 1.1.5\), liblua5.1-0, libmunge2 \(\>= 0.5.8\), libmysqlclient21 \(\>= 8.0.11\), librrd8 \(\>= 1.3.0\), adduser, ucf, munge, lsb-base \(\>= 3.2-12\), libncurses6 \(\>= 6\), libreadline8 \(\>= 6.0\), libtinfo6 \(\>= 6\)" \
--replaces="slurm-client, slurm-wlm, slurm-wlm-basic-plugins, slurmctld, slurmd, slurmdbd, slurmrestd" \
--conflicts="slurm-client, slurm-wlm, slurm-wlm-basic-plugins, slurmctld, slurmd, slurmdbd, slurmrestd"
```

### Read more

|                                              Summary                                               |                 Explanation                 |
|:--------------------------------------------------------------------------------------------------:|:-------------------------------------------:|
| [Woongbin's blog](https://wbk.one/%2Farticle%2F42a272c3%2Fapt-get-build-dep-to-install-build-deps) | Exhausted example for building deb packages |
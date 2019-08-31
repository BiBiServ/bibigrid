## Version 2.0.7 < 2.0.6 (08/31/2019)

### Features
- add full support for Debian Stretch (9) and Debian Buster (10) (#85)
- make Slurm workers return to service automatically after reboot (#168)
- add support for local DNS lookup (#170)

### Fixes
- source code documentation & clean up (#151)
- fixed line ending replacement command corrupts custom ansible role .tar.gz files (#151)
- documentation (#141)
- more helpful error message in case of unsupported configuration properties (#141)
- update naming scheme for worker nodes -> replace slave[s] with worker (#155)
- upgrade 3rd party lib dependencies
- improve error handling (#154) - rewrite/restructure CreateCluster functionality
- bind mount ephemeral disks to prevent filesystem corruption after reboot (#160)
- Openstack : display message in case of auth failure (#165)
- Slurm : respect useMasterAsCompute setting (#167)
- improve wording of printed messages (#169)

## Version 2.0.6 < 2.0.5 (08/07/2019)

### Features
- Configuration option to include ansible (galaxy) files (#100)
- Added support for Apache Cassandra via ansible (#93)

### Fixes
- Update documentation for ansible (#143, #145)
- Fixed help option (-h or --help)

## Version 2.0.5 < 2.0.4 (04/22/2019)

### Fixes
- Update documentation (#127,#131)
- Fix a bug concerning multiple slaves types (#130)

## Version 2.0.4 <- 2.0.3 (04/12/2019)

### Features
- GridEngine: supports user specific configuration of gridengine master

## Version 2.0.3 <- 2.0.2 (03/28/2019)

## Features
- IDE: Evaluated and added Theia as alternative to Cloud9 (#107, #120) 
- Limit port access to restricted number of hosts via ipRange in Config #108

## Fixes

- Update documentation with commented configuration file #98
- Removed config parameters from Command Line Documentation #98

## Version 2.0.2 <- 2.0.1 (03/14/2019)

### Features
- Monitoring: Replace Ganglia with Zabbix (#90) 

### Fixes
- Ubuntu 16.04 Ganglia fails after master reboot (#112) 
- Update or remove support for Apache Mesos (#92) 
- Code cleanup #109 
- (Openstack) Using image "names" instead of "ids" causes a runtime exception #106 
- Add available memory to slurm config #105

## Version 2.0.1 <- 2.0 (02/22/2019)

- Remove native support for cassandra and mesos (#92,#93)
- support server groups (#89)
- tag each single ansible tag for easier debugging (#86)
- add support for Ubuntu 18.04 (partly #85)
- fix bug concerning mounting volumes when using config-drives (#84)
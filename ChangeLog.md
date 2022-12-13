## Version 2.3.1 (3/8/2021)

This will probably the latest version of BiBigrid depended on Java.   We are currently working on a complete 
reimplementation: BiBiGrid2.

## Fixes
- minor ansible configuration cleanup

## Features
- (#327)

## Version 2.3 (3/2/2021)

## Fixes
- solves problem with locked dpkg lock file (#297)
- security: adjust default ip range for opened ports (#285)
- set to theia ide default workspace if wrong path provided (#217)
- bumps [jackson-databind](https://github.com/FasterXML/jackson) from 2.9.9.3 to 2.9.10.5 due to vulnerability issues

## Features
- restructure: move client to provider module (#300)
- update theia-ide (#289)
- determine project quotas (#257)

## Version 2.2.2 (11/7/2020)

### Fixes
- Print possible configuration errors also to STDOUT (#222)
- Add option to overwrite service CIDR mask (#283)

### Features
- Add Support for cloud site regions (#281)

## Version 2.2.1 (10/20/2020)

### Fixes 
- Bumps junit from 4.12 to 4.13.1
- Adjust RealMemory option of Slurm configuration (#178)
- Add date to logback configuration


## Version 2.2 (10/05/2020)

### Fixes
- Add (correct) RealMemory option to Slurm configuration (#178)
- Adjust documentation
- Fix theia-ide build configuration (#260)
- Bumps [jackson-databind](https://github.com/FasterXML/jackson) from 2.9.9.3 to 2.9.10.5 due to vulnerability issues

### Features
- Add support to install prebuild theia-ide (#260)
- Add full REST API Server (experimental) - thanks to Tim Rose / David Weinholz 

## Version 2.1.1 < 2.1.0 (08/06/2020)

### Fixes
- Fix various bugs concerning scale-up/down a cluster (#246,#250)
- Fix slurm consumable resources config 
- Adjust documentation

## Version 2.1.0 < 2.0.10 (07/02/2020)

### Features
- Added support for Ubuntu 20.04 (Focal Fossa) (#228)
- Manual Upscaling via su (scale-up) parameter (#229)
- Manual Downscaling via sd (scale-down) parameter (#229)
- Upgrade TheiaIDE to current stable 1.3 Release (#227)
- Upgrade Zabbix to current stable 5.0 Release (#228)

### Fixes
- Fix slurm & oge configuration for Ubuntu 16.04 (#228)
- Displaying only BiBiGrid clusters with list parameter
- Sort list option order to enhance usability (#231) 
- Add ansible role subfolder, prevents duplicate host role name (#234)
- Enhanced Logging, e.g. for Cluster Configuration Loading Progress (#236)
- Minor error message fixes

### General
- Added documentation
- Terminate multiple clusters separated by white space instead of ','
- Split configuration on remote
- Restructuring of loading cluster configuration
- Ansible Configuration made static

## Version 2.0.10 < 2.0.9 (12/13/2019)

### Features
- Added IdeConf parameter to integrate usage of self-assigned port(s) for TheiaIDE forwarding 
(prevents already bound ports) (#180)

### Fixes
- Various Theia bugs (#211,#213,#214)
- Update & upgrade entire system (#193)
- Improve error handling (#191)
- Pin theia-ide version (#207)
- Fix credentials handling (#208)
- Fix zabbix with Ubuntu 16.04 (#203)

## Version 2.0.9 < 2.0.8 (10/01/2019)

### Features
- Restructure SSH-Key handling, use One-Time SSH keys (#184, fixes #181)

### Fixes
- IDE arg: Port Forwarding to "private" ips, if property "UseMasterWithPublicIp" unset (#188)

## Version 2.0.8 < 2.0.7 (09/12/2019)

### Features
- Show cleanup reminder if user hits ctrl+c during setup (#172)
- Openstack Provider: support Openstack RC File  (Identity API 3)

### Fixes
- Cluster list: optimize column widths and ellipsize usernames (#173)

### General
- Internal restructuring of (cmdline) configuration (#177)

## Version 2.0.7 < 2.0.6 (08/31/2019)

### Features
- Add full support for Debian Stretch (9) and Debian Buster (10) (#85)
- Make Slurm workers return to service automatically after reboot (#168)
- Add support for local DNS lookup (#170)

### Fixes
- Source code documentation & clean up (#151)
- Fixed line ending replacement command corrupts custom ansible role .tar.gz files (#151)
- Extend / Adapt documentation (#141)
- Error message more helpful in case of unsupported configuration properties (#141)
- Update naming scheme for worker nodes -> replace slave[s] with worker (#155)
- Upgrade 3rd party lib dependencies
- Improve error handling (#154) - rewrite/restructure CreateCluster functionality
- Bind mount ephemeral disks to prevent filesystem corruption after reboot (#160)
- Openstack : display message in case of auth failure (#165)
- Slurm : respect useMasterAsCompute setting (#167)
- Improve wording of printed messages (#169)

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
- Support server groups (#89)
- Tag each single ansible tag for easier debugging (#86)
- Add support for Ubuntu 18.04 (partly #85)
- Fix bug concerning mounting volumes when using config-drives (#84)
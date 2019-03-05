# Configuration File Schema
The configuration file is written in YAML format. In contrast to the command line
parameters, a configuration file is easier to maintain and in some cases provides
more detailed configuration possibilities.

**Shared schema**
```
# Cloud Usage
mode: enum                      [aws, googlecloud, openstack, azure]

# Access
user: string
sshUser: string
keypair: string
sshPublicKeyFile: string
sshPrivateKeyFile: string
credentialsFile: string

gridPropertiesFile: string

region: string
availabilityZone: string

# Network
network: string
subnet: string
ports:
  - type: enum                  [TCP, UDP, ICMP]
    number: integer             [1 - 65535]
    ipRange: string
  - ...

# Master
masterInstance:
  type: string                  [flavor, e.g. de.NBI.default ]
  image: string                 [image id]
  
useMasterAsCompute: boolean     [yes, "no"]
useMasterWithPublicIp: boolean  [yes, "no"]

# [List of] Slave[s]
slaveInstances:
  - type: string                [flavor, e.g. de.NBI.default ]
    count: integer              [number of instances]
    image: string               [image id]
  - ...

masterAnsibleRoles:   # ???
  - string
  - ...
slaveAnsibleRoles:    # ???
  - string
  - ...

# Services

# HPC cluster software
slurm: boolean                  [yes, "no"]
oge: boolean                    [yes, "no"] # deprecated - supported for Ubuntu 16.04

# monitoring
zabbix: boolean                 [yes, "no"]
zabbixConf:
    db: string                  ["zabbix"]
    db_user: string             ["zabbix"]
    db_password: string         ["zabbix"]
    timezone: string            ["Europe/Berlin"]
    servername:                 ["bibigrid"]
    admin_password:             ["bibigrid"] # should be changed
    
ganglia: boolean                [yes, "no"] # deprecated - supported for Ubuntu 16.04 only

# web ide
cloud9: boolean                 [yes, "no"]
cloud9Workspace: string         ["~"]

# Network FS
nfs: boolean                    [yes, "no"]
nfsShares:
  - string
  - ...
extNfsShares:
  - source: string
    target: string
  - ...

masterMounts:
  - source: string
    target: string
  - ...

localFS: enum                   [EXT2, EXT3, "EXT4", XFS]

#Misc
debugRequests: boolean          [yes, "no"]


```

**Google Compute specific schema**
```
googleProjectId: string
googleImageProjectId: string
```

**Openstack specific schema**

```
router: string
securityGroup: string
openstackCredentials:
  tenantName: string
  username: string
  password: string
  endpoint: string
  domain: string
  tenantDomain: string
```

**AWS specific schema**
```
bidPrice: double
bidPriceMaster: double
publicSlaveIps: boolean [yes, no]
useSpotInstances: boolean [yes, no]
```

**Azure specific schema**

There are currently no azure specific parameters.






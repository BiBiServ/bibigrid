# Configuration File Schema
The configuration file is written in YAML format. In contrast to the command line
parameters, a configuration file is easier to maintain and in some cases provides
more detailed configuration possibilities.

**Shared schema**
```
# Comment

mode: enum [aws, googlecloud, openstack, azure]

user: string
sshUser: string
keypair: string
sshPublicKeyFile: string
sshPrivateKeyFile: string
credentialsFile: string

gridPropertiesFile: string

region: string
availabilityZone: string

network: string
subnet: string
ports:
  - type: enum [TCP, UDP, ICMP]
    number: integer [1 - 65535]
    ipRange: string
  - ...

useMasterAsCompute: boolean [yes, no]
useMasterWithPublicIp: boolean [yes, no]
useSpotInstances: boolean [yes, no]

masterInstance:
  type: string
  image: string

slaveInstances:
  - type: string
    count: integer
    image: string
  - ...

masterAnsibleRoles:
  - string
  - ...
slaveAnsibleRoles:
  - string
  - ...

cassandra: boolean [yes, no]
mesos: boolean [yes, no]
oge: boolean [yes, no]
hdfs: boolean [yes, no]
spark: boolean [yes, no]
nfs: boolean [yes, no]
cloud9: boolean [yes, no]

cloud9Workspace: string

debugRequests: boolean [yes, no]

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

localFS: enum [EXT2, EXT3, EXT4, XFS]
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
```

**Azure specific schema**

There are currently no azure specific parameters.






# Configuration File Schema

```
mode: string

user: string
sshUser: string
keypair: string
identityFile: string

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

cassandra: boolean [yes, no]
mesos: boolean [yes, no]
oge: boolean [yes, no]
hdfs: boolean [yes, no]
spark: boolean [yes, no]
nfs: boolean [yes, no]
cloud9: boolean [yes, no]

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


# Google
googleCredentialsFile: string
googleProjectId: string
googleImageProjectId: string

# Openstack
router: string
securityGroup: string
openstackCredentials:
  tenantName: string
  username: string
  password: string
  endpoint: string
  domain: string
  tenantDomain: string

# AWS
awsCredentialsFile: string
bidPrice: double
bidPriceMaster: double
publicSlaveIps: boolean [yes, no]

# Azure
azureCredentialsFile: string
```
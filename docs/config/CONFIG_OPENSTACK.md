## Openstack Configuration File Schema

**YAML Config-File**
```
# Comment

mode: openstack

user: string
sshUser: string
keypair: string
sshPublicKeyFile: string    # */.ssh/id_rsa.pub
sshPrivateKeyFile: string   # */.ssh/id_rsa
credentialsFile: string     # commonly ends with 'credentials.yml'

gridPropertiesFile: string

region: string
availabilityZone: string

network: string     # network-id or network-name
subnet: string      # subnet-id or subnet-name
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

workerInstances:
  - type: string
    count: integer
    image: string
  - ...

# Not supported yet
# masterAnsibleRoles:
#  - string
#  - ...
# workerAnsibleRoles:
#  - string
#  - ...

# currently not supported
# cassandra: boolean [yes, no]
# mesos: boolean [yes, no]
# hdfs: boolean [yes, no]
# spark: boolean [yes, no]

oge: boolean [yes, no]
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

**Specific schema**
```
router: string
securityGroup: string
```

**credentials.yml**
```
projectName: string
username: string
password: string
endpoint: string
domain: string
projectDomain: string
```
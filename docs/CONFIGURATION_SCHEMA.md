# Configuration File Schema
The configuration file is written in YAML format. In contrast to the command line
parameters, a configuration file is easier to maintain and in some cases provides
more detailed configuration possibilities.

**Shared schema**

```
# Cloud Usage
mode: enum [aws, googlecloud, openstack, azure]     # Provider mode

# Access
user: string                                        # User name (just for VM tagging)
sshUser: string                                     # SSH user name
keypair: string                                     # Keypair name for authentication (aws and openstack only)
sshPublicKeyFile: string                            # SSH public key file (e.g.: .ssh/id_rsa.pub)
sshPrivateKeyFile: string                           # SSH private key file (e.g.: .ssh/id_rsa)
credentialsFile: string                             # credentials file (e.g. */.bibigrid.credentials.yml)

region: string                                      # Specific region
availabilityZone: string                            # e.g. default, maintenance, ...

gridPropertiesFile: string                          # 

# Network
network: string                                     # name / id of network (e.g. 0a217b61-4c67-...)
subnet: string                                      # name / id of subnet
ports:
  - type: enum [TCP, UDP, ICMP]                     # Transmission Protocol, TCP recommended
    number: integer [1 - 65535]                     # Port number, (e.g. TCP-Port 80 - HTTP)
    ipRange: string
  - ...

# Master 
masterInstance:
  type: string                                      # Instance Flavor, self-assigned (e.g. m1.small)
  image: string                                     # Image ID (e.g. 802e0abe-ac6c-...) or Image name

# Slaves
slaveInstances:
  - type: string                                    # Instance Flavor, self-assigned (e.g. m1.small)
    image: string                                   # Image ID (e.g. 802e0abe-ac6c-...) or Image name
    count: integer                                  # Number of Slave Instances
  - ...
  
# Services
useMasterAsCompute: boolean [yes, no]               # 
useMasterWithPublicIp: boolean [yes, no]            # Usage of public IP. Default is No
useSpotInstances: boolean [yes, no]                 # Only usable with Google Compute and AWS, offered unused Instances

masterAnsibleRoles:
  - string
  - ...
slaveAnsibleRoles:
  - string
  - ...

oge: boolean [yes, no]
nfs: boolean [yes, no]
cloud9: boolean [yes, no]

cloud9Workspace: string

debugRequests: boolean [yes, no]

# FileSystem
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

**Google Compute specific schema**
```
googleProjectId: string
googleImageProjectId: string
```

**AWS specific schema**
```
bidPrice: double
bidPriceMaster: double
publicSlaveIps: boolean [yes, no]
```

**Azure specific schema**

There are currently no azure specific parameters.

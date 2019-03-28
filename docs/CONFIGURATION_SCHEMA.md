# Configuration File Schema
The configuration file is written in YAML format. In contrast to the command line
parameters, a configuration file is easier to maintain and in some cases provides
more detailed configuration possibilities.

**Shared schema**

```
# Cloud Usage
mode: enum [openstack, aws, googlecloud, azure]     # Provider mode

# Access
user: string                                        # User name (just for VM tagging)
sshUser: string                                     # SSH user name, default is "ubuntu"
keypair: string                                     # Keypair name for authentication (aws and openstack only)
sshPublicKeyFile: string                            # SSH public key file (e.g.: .ssh/id_rsa.pub)
sshPrivateKeyFile: string                           # SSH private key file (e.g.: .ssh/id_rsa)
credentialsFile: string                             # credentials file (e.g.: */.bibigrid.credentials.yml)

region: string                                      # Specific region
availabilityZone: string                            # e.g.: default, maintenance, ...

# Network
network: string                                     # name / id of network (e.g.: 0a217b61-4c67-...)
subnet: string                                      # name / id of subnet
ports:
  - type: enum [TCP, UDP, ICMP]                     # Transmission Protocol, Default is TCP (recommended)
    number: integer [1 - 65535]                     # Port number, (e.g. TCP-Port 80 - HTTP)
    ipRange: string                                 # "current" or CIDR mask to restrict access, (e.g.: 129.60.50.0/24)
  - ...

# Master 
masterInstance:
  type: string                                      # Instance Flavor, self-assigned (e.g.: m1.small)
  image: string                                     # Image ID (e.g.: 802e0abe-ac6c-...) or Image name

# Slaves
slaveInstances:
  - type: string                                    # Instance Flavor, self-assigned (e.g.: m1.small)
    image: string                                   # Image ID (e.g.: 802e0abe-ac6c-...) or Image name
    count: integer                                  # Number of Slave Instances
  - ...
  
# Services
useMasterAsCompute: boolean [yes, no]               # Use master as compute instance, Default is no
useMasterWithPublicIp: boolean [yes, no]            # Usage of public IP. Default is yes
useSpotInstances: boolean [yes, no]                 # Only usable with Google Compute and AWS, offered unused Instances

masterAnsibleRoles:                                 # Ansible roles to run on master
  - string                                          # Path to role, e.g.:
  - ...
slaveAnsibleRoles:                                  # Ansible roles to run on slaves
  - string                                          # Path to role, e.g.:
  - ...

# HPC Cluster Software
slurm: boolean [yes, no]                            # Enable / Disable SLURM Workload Manager. Default is no
oge: boolean [yes, no]                              # deprecated - supported for Ubuntu 16.04 only. Default is no

# Monitoring
zabbix: boolean [yes, "no"]                         # Use zabbix monitoring tool. Default is no
zabbixConf:
    db: string                                      # Database name. Default is "zabbix"
    db_user: string                                 # User name for Database. Default is "zabbix"
    db_password: string                             # Password of user. Default is "zabbix"
    timezone: string                                # Default is "Europe/Berlin"
    servername: string                              # Name of Server. Default is "bibigrid"
    admin_password: string                          # Admin password. Default is "bibigrid". Change hardly recommended!
ganglia: boolean [yes, "no"]                        # deprecated - supported for Ubuntu 16.04 only. Default is no
    
# Network FileSystem
nfs: boolean ["yes", no]                            # Enable / Disable Network File System, Default is yes
nfsShares:                                          # Shared File Systems
  - string                                          # Path to File System, e.g.: "/vol/spool" as Default Shared File System
  - ...

extNfsShares:                                       # Uses external Shared File Systems
  - source: string                                  # IP address of external File System
    target: string                                  # Path to File System, e.g.: /path/to/filesystem
  - ...

masterMounts:                                       # Mount volumes to master node, e.g.: 
  - source: string                                  # ec200b48-1b13-4124-... - Volume id
    target: string                                  # /vol/xxx - Volume path (example volume 'xxx' in /vol/ directory)
  - ...

localFS: enum [EXT2, EXT3, EXT4, XFS]               # Local FileSystem. Default is XFS

# Web IDE Usage
theia: boolean [yes, "no"]                          # Enable / Disable Theia Web IDE, Default is no
cloud9: boolean [yes, "no"]                         # deprecated - Enable / Disable Cloud9 Web IDE, Default is no
workspace: string                                   # Configure IDE workspace path, Default is "~/"

# Misc
debugRequests: boolean [yes, no]                    # Provides debug information. Default is no
```

"- ..." means, that there can be offered more than one item, for example:
```
masterMounts:
  - source: string
    target: string
  - ...
```
One could enter more source and target values to provide more volumes to be mounted.

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
useSpotInstances: boolean [yes, no]
```

**Azure specific schema**

There are currently no azure specific parameters.

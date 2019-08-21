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

# Workers
workerInstances:
  - type: string                                    # Instance Flavor, self-assigned (e.g.: m1.small)
    image: string                                   # Image ID (e.g.: 802e0abe-ac6c-...) or Image name
    count: integer                                  # Number of Worker Instances
  - ...
  
# Services
useMasterAsCompute: boolean [yes, no]               # Use master as compute instance, Default is no
useMasterWithPublicIp: boolean [yes, no]            # Usage of public IP. Default is yes
useSpotInstances: boolean [yes, no]                 # Only usable with Google Compute and AWS, offered unused Instances

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
    server_name: string                             # Name of Server. Default is "bibigrid"
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

# Ansible Usage
ansibleRoles:
  - name: string                                    # Name of role, used only as description in config file
    hosts: string                                   # One of 'master', 'workers' or 'all' to roll out ansible roles to specified hosts
    file: string                                    # path/to/file.tar.gz - File on local machine
    vars:
        key : value                                 # Environment variables, if default configuration is not the preferred option
        ...
    vars_file: string                               # Yaml file when many variables are necessary
  - name: ...                                       # Add as many roles as you want

ansibleGalaxyRoles:
  - name: string                                    # Name of role, used to redefine role name
    hosts: string                                   # One of 'master', 'workers' or 'all' to roll out ansible roles to specified hosts
    galaxy: string                                  # Galaxy name of role like 'author.rolename'
    git: string                                     # GitHub role repository like 'https://github.com/bennojoy/nginx'
    url: string                                     # Webserver file url like 'https://some.webserver.example.com/files/master.tar.gzpath/to/file.tar.gz'
    vars:
        key : value                                 # Environment variables, if default configuration is not the preferred option
        ...
    vars_file: string                               # Yaml file when many variables are necessary
  - name: ...                                       # Add as many roles as you want

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

**OpenStack specific schema**
```
router: string                                      # Logical component, forwards data packets between networks
securityGroup: string                               # Like a virtual firewall in network  
serverGroup: string                                 # provides a mechanism to group servers according to certain policy
openstackCredentials:
  tenantName: string                                # OpenStack project name
  username: string                                  # Name of user
  password: string                                  # Password set by user
  endpoint: string                                  # API endpoint
  domain: string                                    # Name of ID of user domain
  tenantDomain: string                              # OpenStack user project domain
```

**Google Compute specific schema**
```
googleProjectId: string                             # ID of Google Compute Engine Project
googleImageProjectId: string                        # ID of Image Project 
```

**AWS specific schema**
```
bidPrice: double                                    # Bid Price in USD ($) for each Amazon EC2 instance when launched as Spot Instances
bidPriceMaster: double
publicWorkerIps: boolean [yes, no]                   # Every worker gets public IP
useSpotInstances: boolean [yes, no]                 # Usage of unused EC2 capacity in AWS cloud at discount price
```

**Azure specific schema**

There are currently no azure specific parameters.

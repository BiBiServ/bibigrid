# Command Line Arguments
Most configuration parameters can be provided to BiBiGrid using the command line. 
However, this becomes messy fast and parameters like the password should not be 
provided in plain text via the command line anyways. 
The recommended alternative is to use a [configuration file](CONFIGURATION_SCHEMA.md).

### 
The parameters you might have to add outside the config YAML are explained in the following list:

| Long parameter | Short parameter | Values           | Description                        |
|----------------|-----------------|------------------|------------------------------------|
| check          | ch              | -                | validate cluster setup             |
| cloud9         | c9              | cluster-id       | establish a secured connection to running grid running cloud9 |
| create         | c               | -                | create cluster environment         |
| config         | o               | path/to/config   | YAML configuration file            |
| help           | h               | -                | Display help message               |
| list           | l               | -                | lists all started clusters         |
| prepare        | p               | -                | prepares cluster setup             |
| terminate      | t               | cluster-id       | terminate cluster                  |
| verbose        | v               | -                | increases logging level during setup |
| version        | V               | -                | Check version                      |

### Configuration parameters
If you decide to use some or all command line parameters for the configuration, a complete list is provided below:  

| Config Parameter       | Long parameter             | Short parameter | Environment variable | Values        | Description |
|------------------------|----------------------------|-----------------|----------------------|---------------|-------------|
| # Cloud Usage          |                            |                 |                      |               |                   |
| mode                   | meta-mode                  | mode            | -                    | [openstack, aws, googlecloud, (azure)] | Provider mode | 
| credentialsFile        | credentials-file           | cf              | -                    | path/credentials | credentials file (e.g. */.bibigrid.credentials.yml |
| # Access               |                            |                 |                      |        |                   |
| sshPublicKeyFile       | ssh-public-key-file        | spu             | -                    | path/public | SSH public key file (e.g.: .ssh/id_rsa.pub)|
| sshPrivateKeyFile      | ssh-private-key-file       | spr             | -                    | path/private | SSH private key file (e.g.: .ssh/id_rsa) |
| user                   | user                       | u               | -                    | string | User name (just for VM tagging) |
| sshUser                | ssh-user                   | su              | -                    | string | SSH user name |
| keypair                | keypair                    | k               | -                    | string | Keypair name for authentication (aws and openstack only) |
| region                 | region                     | e               | OS_REGION_NAME       | string | Specific region |
| availabilityZone       | availability-zone          | z               | -                    | string | e.g. default, maintenance, ... |
| # Network              |                            |                 |                      |        |                   |
| network                | network                    | network         | -                    | e.g. *0a217b61-4c67-...* | name / id of network |
| subnet                 | subnet                     | subnet          | -                    |  | name / id of subnet |
| # Master               |                            |                 |                      |        |                   |
| masterInstance         |                            |                 | -                    |  | |
| &nbsp; &nbsp; - type   | master-instance-type       | m               | -                    | e.g. *m1.small* | Instance Flavor, self-assigned |
| &nbsp; &nbsp; &nbsp; image  | master-image          | M               | -                    | e.g. *802e0abe-ac6c-...* | Image ID |
| masterMounts           | master-mounts              | d               | -                    | source volume id and target address | Mount volume with id to specified target |
| # Slaves               |                            |                 |                      |        |                   |
| slaveInstances         |                            |                 | -                    | | |
| &nbsp; &nbsp; - type   | slave-instance-type        | s               | -                    | e.g. *m1.small* | Instance Flavor, self-assigned |
| &nbsp; &nbsp; &nbsp; image | slave-image            | S               | -                    | e.g. *802e0abe-ac6c-...* | Image ID |
| &nbsp; &nbsp; &nbsp; count | slave-instance-count   | n               | -                    | integer | Number of Instances |
| # Services             |                            |                 |                      |        |                   |
| useMasterAsCompute     | use-master-as-compute      | b               | -                    | [yes, no] | Usage of master as compute |
| useMasterWithPublicIp  | use-master-with-public-ip  | pub             | -                    | [yes, no] | Usage of public IP. Default is No |
| useSpotInstances       | use-spot-instance-request  | usir            | -                    | [yes, no] | |
| oge                    | oge                        | oge             | -                    | [yes, no] | OpenGridEngine support |
|                        | cloud9-workspace           | c9w             | -                    | | Path for cloud9 to use as workspace. Default is ~/ |
| # Firewall/Security Group                           |                 |                      |                      |        |                   |
| ports                  | ports                      | p               | -                    | | |
| &nbsp; &nbsp; - type   |                            |                 | -                    | [TCP, UDP] | Transmission Protocol, TCP recommended |
| &nbsp; &nbsp; &nbsp; number |                       |                 | -                    | e.g. 80 | Port number, 80 recommended |
| # FileSystem           |                            |                 | -                    |        |                   |
| nfs                    | nfs                        | nfs             | -                    | [yes, no] | NFS support |
| localFS                | local-fs                   | lfs             | -                    | [EXT2, EXT3, EXT4, XFS] | Type of Linux filesystem |
| nfsShares              | nfs-shares                 | g               | -                    | /remote/volume | Volume to share in NFS |
| extNfsShares           | ext-nfs-shares             | ge              | -                    | | |
|                        | debug-requests             | dr              |                      | [yes, no]    | Log HTTP requests (currently openstack and googlecloud) |
|                        | grid-properties-file       | gpf             |                      |                      | |
|                        | list-instance-types        | lit             |                      |                      | "--help -lit" lists all available instance types |

### Openstack specific parameters
| Long parameter         | Short parameter | Environment variable | Values | Description                                          |
|------------------------|-----------------|----------------------|--------|------------------------------------------------------|
| openstack-username     | osu             | OS_USERNAME         | | |
| openstack-projectname  | ost             | OS_PROJECT_NAME     | | |
| openstack-password     | osp             | OS_PASSWORD         | | The password can only be provided as environment variable or config file for security reasons |
| openstack-endpoint     | ose             | OS_AUTH_URL         | | |
| openstack-domain       | osd             | OS_USER_DOMAIN_NAME | | |
| openstack-projectdomain | ostd            |                     | | |
| security-group         | sg              |                     | | |
| router                 | router          |                     | | |

### Google Compute specific parameters
| Long parameter         | Short parameter | Environment variable | Values | Description                                          |
|------------------------|-----------------|----------------------|--------|------------------------------------------------------|
| google-projectid       | gpid  |         | | The compute engine project ID |
| google-image-projectid | gipid |         | | The compute engine project ID hosting the images to be used |

### Amazon AWS specific parameters
| Long parameter  | Short parameter | Environment variable | Values | Description                                          |
|---------------- |-----------------|----------------------|--------|------------------------------------------------------|
| public-slave-ip | psi             |                      | [yes, no] | |
| bidprice        | bp              |                      | | |
| bidprice-master | bpm             |                      | | |

### Azure specific parameters
There are currently no azure specific parameters.

# Command Line Arguments
Most configuration parameters 
can be provided to BiBiGrid using the command line. However, this becomes messy fast 
and parameters like the password should not be provided in plain text via the command line anyways. 
The recommended alternative is to use a [configuration file](CONFIGURATION_SCHEMA.md).

If you decide to use some or all command line parameters, a complete list is provided below:

## Shared parameters
| Config Parameter    | Long parameter             | Short parameter | Environment variable | Values | Description       |
|---------------------|----------------------------|-----------------|----------------------|--------|-------------------|
| # Cloud Usage       |                            |                 |                      |        |                   |
| mode                | meta-mode                  | mode            |                      | [aws, googlecloud, openstack, (azure)] | Provider mode | 
| credentialsFile     | credentials-file           | cf              |                      | | Path to the credentials file |
| # Access            |                            |                 |                      |        |                   |
| sshPublicKeyFile    | ssh-public-key-file        | spu             |                      | | SSH public key file |
| sshPrivateKeyFile   | ssh-private-key-file       | spr             |                      | | SSH private key file |
|                     | user                       | u               |                      | | User name (just for VM tagging) |
| sshUser             | ssh-user                   | su              |                      | | SSH user name |
| keypair             | keypair                    | k               |                      | | Keypair name for authentication (aws and openstack only) |
| region              | region                     | e               | OS_REGION_NAME | | |
| availabilityZone    | availability-zone          | z               |                      | | |
| # Network           |                            |                 |                      |        |                   |
| network             | network                    | network         |                      | | |
| subnet              | subnet                     | subnet          |                      | | |
| # Master            |                            |                 |                      |        |                   |
| masterInstance      |       |                |                      | | |
| type                | master-instance-type       | m               |                      | | |
| image               | master-image               | M               |                      | | |
| masterMounts        | master-mounts              | d               |                      | | |
| # Slaves            |                            |                 |                      |        |                   |
| slaveInstances      |       |                |                      | | |
| type                | slave-instance-type        | s               |                      | | |
| image               | slave-image                | S               |                      | | |
| count               | slave-instance-count       | n               |                      | | |
| # Services          |                            |                 |                      |        |                   |
| useMasterAsCompute  | use-master-as-compute      | b               |                      | [yes, no] | |
| useMasterWithPublicIp| use-master-with-public-ip  | pub             |                      | [yes, no] | |
| useSpotInstances    | use-spot-instance-request  | usir            |                      | [yes, no] | |
| nfs                 | nfs                        | nfs             |                      | [yes, no] | NFS support |
| oge                 | oge                        | oge             |                      | [yes, no] | OpenGridEngine support |
| cloud9              | cloud9                     | c9              |                      | [yes, no] | cloud9 IDE. Default is no. |
|                     | cloud9-workspace           | c9w             |                      | | Path for cloud9 to use as workspace. Default is ~/ |
| # Firewall/Security Group          |                            |                 |                      |        |                   |
| ports               | ports                      | p               |                      | | |
| # Services          |                            |                 |                      |        |                   |
| localFS             | local-fs                   | lfs             |                      | [EXT2, EXT3, EXT4, XFS] | |
| nfsShares           | nfs-shares                 | g               |                      | | |
| | ext-nfs-shares             | ge              |                      | | |
| | debug-requests             | dr              |                      | [yes, no] | Log HTTP requests (currently openstack and googlecloud) |
| | verbose                    | v               |                      | - | Increase the logging level to verbose |
| | config                     | o               |                      | | YAML configuration file |
| | grid-properties-file       | gpf             |                      | | |
| | list-instance-types        | lit             |                      | | "--help -lit" lists all available instance types |

## Openstack specific parameters
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

## Google Compute specific parameters
| Long parameter         | Short parameter | Environment variable | Values | Description                                          |
|------------------------|-----------------|----------------------|--------|------------------------------------------------------|
| google-projectid       | gpid  |         | | The compute engine project ID |
| google-image-projectid | gipid |         | | The compute engine project ID hosting the images to be used |

## Amazon AWS specific parameters
| Long parameter  | Short parameter | Environment variable | Values | Description                                          |
|---------------- |-----------------|----------------------|--------|------------------------------------------------------|
| public-slave-ip | psi             |                      | [yes, no] | |
| bidprice        | bp              |                      | | |
| bidprice-master | bpm             |                      | | |

## Azure specific parameters
There are currently no azure specific parameters.

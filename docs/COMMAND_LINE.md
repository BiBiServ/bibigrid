# Command Line Arguments
Most configuration parameters can be provided to BiBiGrid using the command line.
However this becomes messy fast and parameters like the password shouldn't be provided
in plain text via the command line anyways. An alternative is a [configuration file](CONFIGURATION_SCHEMA.md).

If you decide to use some or all command line parameters, a complete list is provided below:

## Shared parameters
| Long parameter             | Short parameter | Environment variable | Values | Description                                          |
|----------------------------|-----------------|----------------------|--------|------------------------------------------------------|
| meta-mode                  | mode            |                      | [aws, googlecloud, openstack, (azure)] | Provider mode | 
| region                     | e               | OS_REGION_NAME | | |
| availability-zone          | z               |                      | | |
| user                       | u               |                      | | User name (just for VM tagging) |
| ssh-user                   | su              |                      | | SSH user name |
| keypair                    | k               |                      | | Keypair name for authentication (aws and openstack only) |
| ssh-public-key-file        | spu             |                      | | SSH public key file |
| ssh-private-key-file       | spr             |                      | | SSH private key file |
| credentials-file           | cf              |                      | | Path to the credentials file |
| master-instance-type       | m               |                      | | |
| master-image               | M               |                      | | |
| master-mounts              | d               |                      | | |
| max-master-ephemerals      | mme             |                      | | |
| use-master-as-compute      | b               |                      | [yes, no] | |
| use-master-with-public-ip  | pub             |                      | [yes, no] | |
| slave-instance-type        | s               |                      | | |
| slave-image                | S               |                      | | |
| max-slave-ephemerals       | mse             |                      | | |
| slave-instance-count       | n               |                      | | |
| use-spot-instance-request  | usir            |                      | [yes, no] | |
| ports                      | p               |                      | | |
| network                    | network         |                      | | |
| subnet                     | subnet          |                      | | |
| cloud9-workspace           | c9w             |                      | | Path for cloud9 to use as workspace. Default is ~/ |
| nfs                        | nfs             |                      | [yes, no] | NFS support |
| oge                        | oge             |                      | [yes, no] | GridEngine support |
| spark                      | spark           |                      | [yes, no] | Spark support |
| hdfs                       | hdfs            |                      | [yes, no] | HDFS support |
| mesos                      | me              |                      | [yes, no] | Mesos support |
| cassandra                  | db              |                      | [yes, no] | Cassandra support |
| local-fs                   | lfs             |                      | [EXT2, EXT3, EXT4, XFS] | |
| nfs-shares                 | g               |                      | | |
| ext-nfs-shares             | ge              |                      | | |
| debug-requests             | dr              |                      | [yes, no] | Log HTTP requests (currently openstack and googlecloud) |
| verbose                    | v               |                      | - | Increase the logging level to verbose |
| config                     | o               |                      | | YAML configuration file |
| grid-properties-file       | gpf             |                      | | |
| list-instance-types        | lit             |                      | | "--help -lit" lists all available instance types |

## Openstack specific parameters
| Long parameter         | Short parameter | Environment variable | Values | Description                                          |
|------------------------|-----------------|----------------------|--------|------------------------------------------------------|
| openstack-username     | osu             | OS_USERNAME         | | |
| openstack-tenantname   | ost             | OS_PROJECT_NAME     | | |
| openstack-password     |                 | OS_PASSWORD         | | The password can only be provided as environment variable or config file for security reasons |
| openstack-endpoint     | ose             | OS_AUTH_URL         | | |
| openstack-domain       | osd             | OS_USER_DOMAIN_NAME | | |
| openstack-tenantdomain | ostd            |                     | | |
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

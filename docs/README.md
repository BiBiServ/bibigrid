# Documentation

- [Instance configuration with Ansible](../bibigrid-core/src/main/resources/README.md)

# Getting Started
Starting a cluster requires a valid configuration file and credentials. Following are the necessary steps with detailed information for each cloud provider.

## Setting up credentials
For communication with the cloud provider API, credentials have to be setup.
Additionally during cluster creation the master instance will handle software updates and installations for all cluster instances using ansible.
In order to upload and execute commands a valid ssh-keypair needs to be setup, too.
- [OpenStack credentials setup](../bibigrid-openstack/docs/Credentials_Setup.md)
- [Google compute credentials setup](../bibigrid-googlecloud/docs/Credentials_Setup.md)
- [Amazon AWS credentials setup](../bibigrid-aws/docs/Credentials_Setup.md)

## Writing the configuration file
The configuration file specifies the composition of the requested cluster. Many parameters are shared across all cloud providers, however some parameters are provider specific.

| Long parameter             | Short parameter | Environment variable | Description                                          |
|----------------------------|-----------------|----------------------|------------------------------------------------------|
| meta-mode                  | mode            |                      | Provider mode [aws, googlecloud, openstack, (azure)] | 
| region                     | e               | OS_REGION_NAME       | |
| availability-zone          | z               |                      | |
| user                       | u               |                      | |
| keypair                    | k               |                      | |
| identity-file              | i               |                      | |
| master-instance-type       | m               |                      | |
| master-image               | M               |                      | |
| master-mounts              | d               |                      | |
| max-master-ephemerals      | mme             |                      | |
| use-master-as-compute      | b               |                      | |
| slave-instance-type        | s               |                      | |
| slave-image                | S               |                      | |
| slave-mounts               | f               |                      | |
| max-slave-ephemerals       | mse             |                      | |
| slave-instance-count       | n               |                      | |
| use-spot-instance-request  | usir            |                      | |
| ports                      | p               |                      | |
| vpc                        | vpc             |                      | |
| subnet                     | subnet          |                      | |
| nfs                        | nfs             |                      | |
| oge                        | oge             |                      | |
| spark                      | spark           |                      | |
| hdfs                       | hdfs            |                      | |
| mesos                      | me              |                      | |
| cassandra                  | db              |                      | |
| execute-script             | x               |                      | |
| early-execute-script       | ex              |                      | |
| early-slave-execute-script | esx             |                      | |
| local-fs                   | lfs             |                      | |
| nfs-shares                 | g               |                      | |
| ext-nfs-shares             | ge              |                      | |
| debug-requests             | dr              |                      | Log HTTP requests (currently openstack and googlecloud) |
| verbose                    | v               |                      | Increase the logging level to verbose |
| config                     | o               |                      | |
| grid-properties-file       | gpf             |                      | |

### Openstack specific parameters
| Long parameter             | Short parameter | Environment variable | Description                                          |
|----------------------------|-----------------|----------------------|------------------------------------------------------|
| openstack-username         | osu             | OS_USERNAME          | |
| openstack-tenantname       | ost             | OS_PROJECT_NAME      | |
| openstack-password         | osp             | OS_PASSWORD          | |
| openstack-endpoint         | ose             | OS_AUTH_URL          | |
| openstack-domain           | osd             | OS_USER_DOMAIN_NAME  | |
| openstack-tenantdomain     | ostd            |                      | |
| security-group             | sg              |                      | |
| router                     | router          |                      | |
| network                    | network         |                      | |

### Google Compute specific parameters
| Long parameter             | Short parameter | Environment variable | Description                                          |
|----------------------------|-----------------|----------------------|------------------------------------------------------|
| google-projectid           | gpid            |                      | |
| google-image-projectid     | gipid           |                      | |
| google-credentials-file    | gcf             |                      | |

### Amazon AWS specific parameters
| Long parameter             | Short parameter | Environment variable | Description                                          |
|----------------------------|-----------------|----------------------|------------------------------------------------------|
| aws-credentials-file       | a               |                      | |
| public-slave-ip            | psi             |                      | |
| bidprice                   | bp              |                      | |
| bidprice-master            | bpm             |                      | |

### Azure specific parameters
| Long parameter             | Short parameter | Environment variable | Description                                          |
|----------------------------|-----------------|----------------------|------------------------------------------------------|
| azure-credentials-file     | acf             |                      | |

## Validating the cluster configuration
STUB

## Starting the cluster
STUB

## Cluster maintenance
### List running clusters
STUB

### Terminate the cluster
STUB

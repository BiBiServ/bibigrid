# Getting Started

**This is work in progress, mainly meaning that the documentation is incomplete.**

Starting a cluster requires a valid configuration file and credentials. Following are the necessary steps with detailed information for each cloud provider.

## Setting up credentials
For communication with the cloud provider API, credentials have to be setup.
Additionally during cluster creation the master instance will handle software updates and installations for all cluster instances using ansible.
In order to upload and execute commands a valid ssh-keypair needs to be setup, too.
- [OpenStack credentials setup](../bibigrid-openstack/docs/Credentials_Setup.md)
- [Google compute credentials setup](../bibigrid-googlecloud/docs/Credentials_Setup.md)
- [Amazon AWS credentials setup](../bibigrid-aws/docs/Credentials_Setup.md)
- [Microsoft Azure credentials setup](../bibigrid-azure/docs/Credentials_Setup.md)

## Writing the configuration file
The configuration file specifies the composition of the requested cluster. Many parameters are shared across all cloud providers, however some parameters are provider specific.
You can either provide the necessary parameters via the command line, by using a configuration file or in some cases by using environment variables.
The parameters provided via command line are listed below as "short parameter". Using them in a configuration file is done with the "long parameter".

| Long parameter             | Short parameter | Environment variable | Description                                          |
|----------------------------|-----------------|----------------------|------------------------------------------------------|
| meta-mode                  | mode            |                      | Provider mode [aws, googlecloud, openstack, (azure)] | 
| region                     | e               | OS_REGION_NAME       | |
| availability-zone          | z               |                      | |
| user                       | u               |                      | User name (just for VM tagging) |
| ssh-user                   | su              |                      | SSH user name |
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

### Writing and using a configuration file
The configuration file is a plain text file. A short example would be:

```
#use google cloud compute
mode=googlecloud
google-projectid=XXXXX
google-credentials-file=XXXXX
[...]
```

This file can now be used with the "-o" command line parameter and the path to the configuration file.

## Validating the cluster configuration
Before starting the cluster directly after writing the configuration file, several components can be validated beforehand.

```
> bibigrid -ch -o ~/config.properties
```

STUB

## Starting the cluster
STUB

```
> bibigrid -c -o ~/config.properties
```

## Cluster maintenance
### List running clusters
STUB

```
> bibigrid -l -o ~/config.properties
```

### Terminate the cluster
When you're finished using the cluster, you can terminate it using the following command and the logged cluster-id when the cluster was created.

```
> bibigrid -t [cluster-id] -o ~/config.properties
```

If necessary multiple clusters can be terminated at once.

```
> bibigrid -t [id1]/[id2]/[id3] -o ~/config.properties
```

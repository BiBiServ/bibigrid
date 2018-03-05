# Command Line Arguments

| Long parameter             | Short parameter | Environment variable | Values | Description                                          |
|----------------------------|-----------------|----------------------|--------|------------------------------------------------------|
| meta-mode                  | mode            |                      | [aws, googlecloud, openstack, (azure)] | Provider mode | 
| region                     | e               | OS_REGION_NAME       | | |
| availability-zone          | z               |                      | | |
| user                       | u               |                      | | User name (just for VM tagging) |
| ssh-user                   | su              |                      | | SSH user name |
| keypair                    | k               |                      | | Keypair name for authentication (aws and openstack only) |
| identity-file              | i               |                      | | SSH private key file |
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
| nfs                        | nfs             |                      | [yes, no] | NFS support |
| oge                        | oge             |                      | [yes, no] | GridEngine support |
| spark                      | spark           |                      | [yes, no] | Spark support |
| hdfs                       | hdfs            |                      | [yes, no] | HDFS support |
| mesos                      | me              |                      | [yes, no] | Mesos support |
| cassandra                  | db              |                      | [yes, no] | Cassandra support |
| local-fs                   | lfs             |                      | | |
| nfs-shares                 | g               |                      | | |
| ext-nfs-shares             | ge              |                      | | |
| debug-requests             | dr              |                      | [yes, no] | Log HTTP requests (currently openstack and googlecloud) |
| verbose                    | v               |                      | | Increase the logging level to verbose |
| config                     | o               |                      | | YAML configuration file |
| grid-properties-file       | gpf             |                      | | |

### Openstack specific parameters
| Long parameter             | Short parameter | Environment variable | Values | Description                                          |
|----------------------------|-----------------|----------------------|--------|------------------------------------------------------|
| openstack-username         | osu             | OS_USERNAME          | | |
| openstack-tenantname       | ost             | OS_PROJECT_NAME      | | |
| openstack-password         | osp             | OS_PASSWORD          | | |
| openstack-endpoint         | ose             | OS_AUTH_URL          | | |
| openstack-domain           | osd             | OS_USER_DOMAIN_NAME  | | |
| openstack-tenantdomain     | ostd            |                      | | |
| security-group             | sg              |                      | | |
| router                     | router          |                      | | |

### Google Compute specific parameters
| Long parameter             | Short parameter | Environment variable | Values | Description                                          |
|----------------------------|-----------------|----------------------|--------|------------------------------------------------------|
| google-projectid           | gpid            |                      | | The compute engine project ID |
| google-image-projectid     | gipid           |                      | | The compute engine project ID hosting the images to be used |
| google-credentials-file    | gcf             |                      | | Path to the service account credentials file |

### Amazon AWS specific parameters
| Long parameter             | Short parameter | Environment variable | Values | Description                                          |
|----------------------------|-----------------|----------------------|--------|------------------------------------------------------|
| aws-credentials-file       | a               |                      | | |
| public-slave-ip            | psi             |                      | [yes, no] | |
| bidprice                   | bp              |                      | | |
| bidprice-master            | bpm             |                      | | |

### Azure specific parameters
| Long parameter             | Short parameter | Environment variable | Values | Description                                          |
|----------------------------|-----------------|----------------------|--------|------------------------------------------------------|
| azure-credentials-file     | acf             |                      | | Path to the credentials file |

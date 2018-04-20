# Command Line Arguments
Most configuration parameters can be provided to BiBiGrid using the command line.
However this becomes messy fast and parameters like the password shouldn't be provided
in plain text via the command line anyways. An alternative is a [configuration file](CONFIGURATION_SCHEMA.md).

If you decide to use some or all command line parameters, a complete list is provided below:

## Shared parameters
| Long parameter                            | Short parameter                | Environment variable | Values | Description                                          |
|-------------------------------------------|--------------------------------|----------------------|--------|------------------------------------------------------|
| <small>meta-mode</small>                  | <small>mode</small>            |                               | <small>[aws, googlecloud, openstack, (azure)]</small> | <small>Provider mode</small> | 
| <small>region</small>                     | <small>e</small>               | <small>OS_REGION_NAME</small> | | |
| <small>availability-zone</small>          | <small>z</small>               |                               | | |
| <small>user</small>                       | <small>u</small>               |                               | | <small>User name (just for VM tagging)</small> |
| <small>ssh-user</small>                   | <small>su</small>              |                               | | <small>SSH user name</small> |
| <small>keypair</small>                    | <small>k</small>               |                               | | <small>Keypair name for authentication (aws and openstack only)</small> |
| <small>ssh-public-key-file</small>        | <small>spu</small>             |                               | | <small>SSH public key file</small> |
| <small>ssh-private-key-file</small>       | <small>spr</small>             |                               | | <small>SSH private key file</small> |
| <small>credentials-file</small>           | <small>cf</small>              |                               | | <small>Path to the credentials file</small> |
| <small>master-instance-type</small>       | <small>m</small>               |                               | | |
| <small>master-image</small>               | <small>M</small>               |                               | | |
| <small>master-mounts</small>              | <small>d</small>               |                               | | |
| <small>max-master-ephemerals</small>      | <small>mme</small>             |                               | | |
| <small>use-master-as-compute</small>      | <small>b</small>               |                               | <small>[yes, no]</small> | |
| <small>use-master-with-public-ip</small>  | <small>pub</small>             |                               | <small>[yes, no]</small> | |
| <small>slave-instance-type</small>        | <small>s</small>               |                               | | |
| <small>slave-image</small>                | <small>S</small>               |                               | | |
| <small>max-slave-ephemerals</small>       | <small>mse</small>             |                               | | |
| <small>slave-instance-count</small>       | <small>n</small>               |                               | | |
| <small>use-spot-instance-request</small>  | <small>usir</small>            |                               | <small>[yes, no]</small> | |
| <small>ports</small>                      | <small>p</small>               |                               | | |
| <small>network</small>                    | <small>network</small>         |                               | | |
| <small>subnet</small>                     | <small>subnet</small>          |                               | | |
| <small>nfs</small>                        | <small>nfs</small>             |                               | <small>[yes, no]</small> | <small>NFS support</small> |
| <small>oge</small>                        | <small>oge</small>             |                               | <small>[yes, no]</small> | <small>GridEngine support</small> |
| <small>spark</small>                      | <small>spark</small>           |                               | <small>[yes, no]</small> | <small>Spark support</small> |
| <small>hdfs</small>                       | <small>hdfs</small>            |                               | <small>[yes, no]</small> | <small>HDFS support</small> |
| <small>mesos</small>                      | <small>me</small>              |                               | <small>[yes, no]</small> | <small>Mesos support</small> |
| <small>cassandra</small>                  | <small>db</small>              |                               | <small>[yes, no]</small> | <small>Cassandra support</small> |
| <small>local-fs</small>                   | <small>lfs</small>             |                               | <small>[EXT2, EXT3, EXT4, XFS]</small> | |
| <small>nfs-shares</small>                 | <small>g</small>               |                               | | |
| <small>ext-nfs-shares</small>             | <small>ge</small>              |                               | | |
| <small>debug-requests</small>             | <small>dr</small>              |                               | <small>[yes, no]</small> | <small>Log HTTP requests (currently openstack and googlecloud)</small> |
| <small>verbose</small>                    | <small>v</small>               |                               | - | <small>Increase the logging level to verbose</small> |
| <small>config</small>                     | <small>o</small>               |                               | | <small>YAML configuration file</small> |
| <small>grid-properties-file</small>       | <small>gpf</small>             |                               | | |
| <small>list-instance-types</small>        | <small>lit</small>             |                               | | <small>"--help -lit" lists all available instance types</small> |

## Openstack specific parameters
| Long parameter                        | Short parameter       | Environment variable | Values | Description                                          |
|---------------------------------------|-----------------------|----------------------|--------|------------------------------------------------------|
| <small>openstack-username</small>     | <small>osu</small>    | <small>OS_USERNAME</small>         | | |
| <small>openstack-tenantname</small>   | <small>ost</small>    | <small>OS_PROJECT_NAME</small>     | | |
| <small>openstack-password</small>     |                       | <small>OS_PASSWORD</small>         | | <small>The password can only be provided as environment variable or config file for security reasons</small> |
| <small>openstack-endpoint</small>     | <small>ose</small>    | <small>OS_AUTH_URL</small>         | | |
| <small>openstack-domain</small>       | <small>osd</small>    | <small>OS_USER_DOMAIN_NAME</small> | | |
| <small>openstack-tenantdomain</small> | <small>ostd</small>   |                                    | | |
| <small>security-group</small>         | <small>sg</small>     |                                    | | |
| <small>router</small>                 | <small>router</small> |                                    | | |

## Google Compute specific parameters
| Long parameter                        | Short parameter      | Environment variable | Values | Description                                          |
|---------------------------------------|----------------------|----------------------|--------|------------------------------------------------------|
| <small>google-projectid</small>       | <small>gpid</small>  |                      | | <small>The compute engine project ID</small> |
| <small>google-image-projectid</small> | <small>gipid</small> |                      | | <small>The compute engine project ID hosting the images to be used</small> |

## Amazon AWS specific parameters
| Long parameter                 | Short parameter    | Environment variable | Values | Description                                          |
|--------------------------------|--------------------|----------------------|--------|------------------------------------------------------|
| <small>public-slave-ip</small> | <small>psi</small> |                      | <small>[yes, no]</small> | |
| <small>bidprice</small>        | <small>bp</small>  |                      | | |
| <small>bidprice-master</small> | <small>bpm</small> |                      | | |

## Azure specific parameters
There are currently no azure specific parameters.

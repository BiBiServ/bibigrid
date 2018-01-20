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

| Long parameter        | Short parameter | Description                                 |
|-----------------------|-----------------|---------------------------------------------|
| mode                  |                 | Provider mode [aws, googlecloud, openstack] | 
| region                | e               | |
| availability-zone     | z               | |
| user                  | u               | |
| master-instance-type  | m               | |
| master-image          | M               | |
| use-master-as-compute | b               | |
| slave-instance-type   | s               | |
| slave-image           | S               | |
| slave-instance-count  | n               | |
| (aws) public-slave-ip | psi             | |
| nfs                   | nfs             | |
| oge                   | oge             | |
| spark                 | spark           | |
| hdfs                  | hdfs            | |
| mesos                 | me              | |
| cassandra             | db              | |
| local-fs              | lfs             | |
| debug-requests        | dr              | |
| verbose               | v               | |

## Starting the cluster
STUB

## Cluster maintenance
STUB

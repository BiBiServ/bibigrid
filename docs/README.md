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
You can either provide the necessary parameters via the command line, by using a configuration file in yaml format or in some cases by using environment variables.

A complete list of parameters can be found [here](COMMAND_LINE.md).

A complete schema for a configuration file can be found [here](CONFIGURATION_SCHEMA.md).

### Writing and using a configuration file
The configuration file is a plain text file in YAML format. A short example would be:

```
#use google cloud compute
mode: googlecloud
google-projectid: XXXXX
google-credentials-file: XXXXX

region: europe-west1

network: default
subnet: default

user: testuser
sshUser: testuser
identityFile: ~/cloud.ppk

masterInstance:
  type: f1-micro
  image: ubuntu-1604-xenial-v20171212

slaveInstances:
  - type: f1-micro
    count: 2
    image: ubuntu-1604-xenial-v20171212

ports:
  - type: TCP
    number: 80
  - type: TCP
    number: 443
```

This file can now be used with the "-o" command line parameter and the path to the configuration file.

## Validating the cluster configuration
Before starting the cluster directly after writing the configuration file, several components can be validated beforehand.

```
> bibigrid -ch -o ~/config.yml
```

STUB

## Starting the cluster
STUB

```
> bibigrid -c -o ~/config.yml
```

## Cluster maintenance
### List running clusters
STUB

```
> bibigrid -l -o ~/config.yml
```

### Terminate the cluster
When you're finished using the cluster, you can terminate it using the following command and the logged cluster-id when the cluster was created.

```
> bibigrid -t [cluster-id] -o ~/config.yml
```

If necessary multiple clusters can be terminated at once.

```
> bibigrid -t [id1]/[id2]/[id3] -o ~/config.yml
```

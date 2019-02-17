# Getting Started
Starting a cluster requires a valid configuration file and credentials. Following are the necessary steps with 
detailed information for each cloud provider. For an up-to-date user Setup-Tutorial for the BiBiGrid take a look at the [de.NBI Wiki](https://cloud.denbi.de/wiki/Tutorials/BiBiGrid/).

## Setting up credentials
For communication with the cloud provider API, credentials have to be setup.
Additionally during cluster creation the master instance will handle software updates and installations for all cluster instances using ansible.
In order to upload and execute commands a valid ssh-keypair needs to be setup, too.

When using the ssh public key parameter in config or command line, the setup of ssh keys in the credentials setup can be skipped!
* [OpenStack credentials setup](../bibigrid-openstack/docs/Credentials_Setup.md)  
* [Google Compute credentials setup](../bibigrid-googlecloud/docs/Credentials_Setup.md) *
* [Amazon AWS credentials setup](../bibigrid-aws/docs/Credentials_Setup.md) *
* [Microsoft Azure credentials setup](../bibigrid-azure/docs/Credentials_Setup.md) *

## Writing the configuration file
The configuration file specifies the composition of the requested cluster. Many parameters are shared across all cloud providers, however some parameters are provider specific.
You can either provide the necessary parameters via the command line, by using a configuration file in yaml format or in some cases by using environment variables.

A complete list of **command line parameters** can be found [here](COMMAND_LINE.md).

A complete schema for a **configuration file** can be found [here](CONFIGURATION_SCHEMA.md).

**Configuration File Schema**  
The configuration file is written in YAML format. In contrast to the command line
parameters, a configuration file is easier to maintain and in some cases provides
more detailed configuration possibilities.

A complete schema for a specific **configuration file** can be found on:
* [OpenStack Configuration File](config/CONFIG_OPENSTACK.md)  
* [Google Compute Configuration File](config/CONFIG_GOOGLE_COMPUTE.md) *
* [Amazon AWS Configuration File](config/CONFIG_AWS.md) *
* [Azure Configuration File](config/CONFIG_AZURE.md) *

Provider specific examples representing the minimal required parameters:
* [OpenStack Examples](examples/EXAMPLES_OPENSTACK.md)  
* [Google Compute Examples](examples/EXAMPLES_GOOGLECLOUD.md) *
* [AWS Examples](examples/EXAMPLES_AWS.md) *
* [Azure Examples](examples/EXAMPLES_AZURE.md) *
  
**Google Compute, AWS and Azure will currently not be tested.*

### Writing and using a configuration file
The configuration file is a plain text file in YAML format. A short example would be:

```
#use google cloud compute
mode: googlecloud
googleProjectId: XXXXX
googleImageProjectId: ubuntu-os-cloud
credentialsFile: ~/google-credentials.json

region: europe-west1

network: default
subnet: default

user: testuser
sshUser: testuser
sshPrivateKeyFile: ~/cloud.ppk

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

## Creating a bibigrid alias
To keep the cluster setup process simpler you can set an alias for the BiBiGrid JAR file installed before. 
The Unix command should look like the following (depending on JAR filename):
```
> alias bibigrid="java -jar bibigrid-openstack-2.0.jar"
```

Instead of the java command there will be used the 'bibigrid' command just created instead.

## Validating the cluster configuration
Before starting the cluster directly after writing the configuration file, several components can be validated beforehand.
This prevents the majority of possible errors or typos, resulting in incomplete cluster setups.

```
> bibigrid -ch -v -o ~/config.yml
```
The command will be executed by default when creating a new cluster.

## Starting the cluster
Once the configuration is validated, the creation of the cluster can be started. Depending on the parameters
this may take some time.

```
> bibigrid -c -v -o ~/config.yml
```

## Starting the cloud9 IDE
If you activated the cloud9 feature in configuration, the IDE can be started with a simple command.
The process will run until an input is provided or it's terminated. The IDE is available under [http://localhost:8181](http://localhost:8181)

```
> bibigrid -cloud9 [cluster-id] -v -o ~/config.yml
```

## Cluster maintenance
### List running clusters
Once a cluster is created, it can be listed with the following command. All clusters found
with the selected provider will be listed, including some detail information.

```
> bibigrid -l -o ~/config.yml
```

Example output:

```
     cluster-id |       user |         launch date |             key name |       public-ip |  # inst |    group-id |   subnet-id |  network-id
-----------------------------------------------------------------------------------------------------------------------------------------------
fkiseokf34ekfeo |   testuser |   20/02/18 09:25:10 |          cluster-key |    XXX.XX.XX.XX |       3 | a45b6a63-.. |           - |           -
```

### Terminate the cluster
When you're finished using the cluster, you can terminate it using the following command and the logged cluster-id when the cluster was created.

```
> bibigrid -t [cluster-id] -v -o ~/config.yml
```

If necessary multiple clusters can be terminated at once.

```
> bibigrid -t [id1]/[id2]/[id3] -v -o ~/config.yml
```

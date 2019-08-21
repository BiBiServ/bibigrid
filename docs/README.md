# Getting Started
Starting a cluster requires a valid configuration file and credentials. Following are the necessary steps with 
detailed information for each cloud provider. For an up-to-date user Setup-Tutorial for the BiBiGrid take a look at the [de.NBI Wiki](https://cloud.denbi.de/wiki/Tutorials/BiBiGrid/).

### Setting up credentials
For communication with the cloud provider API, credentials have to be setup.
Additionally during cluster creation the master instance will handle software updates and installations for all cluster instances using ansible.
In order to upload and execute commands a valid ssh-keypair needs to be setup, too.

When using the ssh public key parameter in config or command line, the setup of ssh keys in the credentials setup can be skipped!
* [OpenStack credentials setup](../bibigrid-openstack/docs/Credentials_Setup.md)  
* [Google Compute credentials setup](../bibigrid-googlecloud/docs/Credentials_Setup.md) *
* [Amazon AWS credentials setup](../bibigrid-aws/docs/Credentials_Setup.md) *
* [Microsoft Azure credentials setup](../bibigrid-azure/docs/Credentials_Setup.md) *

### Writing the configuration file
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
  
**Google Compute, AWS and Azure will currently not be tested.*

#### Writing and using a configuration file
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

workerInstances:
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

### Creating a bibigrid alias
To keep the cluster setup process simpler you can set an alias for the BiBiGrid JAR file installed before. 
The Unix command should look like the following (depending on JAR filename):
```
> alias bibigrid="java -jar /path/to/bibigrid-*.jar"
```

Instead of the java command there will be used the 'bibigrid' command just created. 
Since the alias only applies in the current terminal session, it is recommended to add it to the 
'.bashrc' file in the home directory.

### Validating the cluster configuration
Before starting the cluster directly after writing the configuration file, several components can be validated beforehand.
This prevents the majority of possible errors or typos, resulting in incomplete cluster setups.
```
> bibigrid -ch -v -o ~/config.yml
```
The command will be executed by default when creating a new cluster.

### Starting the cluster
Once the configuration is validated, the creation of the cluster can be started. Depending on the parameters
this may take some time.
```
> bibigrid -c -v -o ~/config.yml
```

### Starting the Web IDE
At first you need to have activated the 'theia' or 'cloud9' feature in the configuration file.
Although both IDEs are supported, Theia IDE is recommended. Cloud9 support is deprecated and 
will not be further integrated in future.
The IDE can be started with a simple command:
```
> bibigrid --ide [cluster-id] -v -o ~/config.yml
```
The process will run until an input is provided or it's terminated. 
The IDE is available under [http://localhost:8181](http://localhost:8181) and will be started automatically.

### Monitoring via Zabbix Web Frontend
If you want to monitor a cluster, you can integrate the zabbix support into your configuration file:
```
zabbix: yes
zabbixConf:
    db: string                    # Database name. Default is "zabbix"
    db_user: string               # User name for Database. Default is "zabbix"
    db_password: string           # Password of user. Default is "zabbix"
    timezone: string              # Default is "Europe/Berlin"
    server_name: string           # Name of Server. Default is "bibigrid"
    admin_password: string        # Change to an unique and secure password
```
If you don't want to change the default values you can leave out the terms and only change the `admin_password`. 
After starting the cluster you can visit the Zabbix Web Frontend by opening in a browser:
```
http://ip.of.your.master/zabbix
```
The 'Username' to enter is `admin`, the following 'Password' is the previously specified admin password.

### GridEngine Configuration
If you decide to enable GridEngine (deprecated, supported for Ubuntu 16.04 only - you may use SLURM instead)
you have to use the `oge` configuration parameter. 
See the [sge_conf(5) man page](http://gridscheduler.sourceforge.net/htmlman/htmlman5/sge_conf.html) to get 
an overview as well as a description of the possible parameters.  

As an example you can set the max number of dynamic event clients (jobs submitted via qsub sync):
```
ogeConf:
    qmaster_params: MAX_DYN_EC=1000
```
The given value(s) will be overwritten in or added to the default configuration. 
Check `qconf -sconf global` on master to proof the configuration.

## Including Ansible (Galaxy) Roles
You can include ansible roles from your own machine (compressed as .tar.gz files) automatically into your cluster setup by defining following configuration settings:

```
ansibleRoles:
  - name: string            # Name of role, used only as description in config file
    hosts: string           # One of 'master', 'workers' or 'all' to roll out ansible roles to specified hosts
    file: string            # path/to/file.tar.gz - File on local machine
    vars:
        key : value         # Environment variables, if default configuration is not the preferred option
        ...
    vars_file: string       # Yaml file when many variables are necessary
  - name: ...               # Add as many roles as you want
```

To get a quick overview of the procedure, you can make your own 'Hello World' example role.  

``` > ansible-galaxy init example_role ```  

creates a basic role structure with 'defaults', 'files', 'vars', 'tasks' and other folders. Go into 'tasks' and change the 'main.yml':
``` main.yml
- debug:
    msg: 
    - "Hello {{ ansible_user }}!"
```

You then have to archive your role folder (in this case: 'example_role') with the command below.
Please make sure that the '.tar.gz' file name and the role folder name are identical.

``` > tar -czvf example_role.tar.gz example_role ```  

You only need the '.tar.gz' file in the next steps. 'example_role' - or whatever you call the archived file - 
will be the name of the role in your cluster. Now include the following lines into your configuration file:

```
ansibleRoles:
  - name: Example role for test purposes	# Name of role, used only as description in config file
    hosts: master                   		# One of 'master', 'workers' or 'all' to roll out ansible roles to specified hosts
    file: example_role.tar.gz       		# path/to/file.tar.gz - File on local machine
```
You can download an easy working example [here](examples/example.tar.gz).  

If you want to include roles from Ansible Galaxy, Git or from a Webserver (as .tar.gz files), add the following lines to your configuration file:

```
ansibleGalaxyRoles:
  - name: string            # Name of role, used to redefine role name
    hosts: string           # One of 'master', 'workers' or 'all' to roll out ansible roles to specified hosts
    galaxy: string          # Galaxy name of role like 'author.rolename'
    git: string             # GitHub role repository like 'https://github.com/bennojoy/nginx'
    url: string             # Webserver file url like 'https://some.webserver.example.com/files/master.tar.gzpath/to/file.tar.gz'
    vars:
        key : value         # Environment variables, if default configuration is not the preferred option
        ...
    vars_file: string       # Yaml file when many variables are necessary
  - name: ...               # Add as many roles as you want
```
Be aware of using only one of 'galaxy', 'git' or 'url'.

### Set up Apache Cassandra on your cluster
If you want to start Apache Cassandra on your cluster, you may include it as follows:
```
ansibleGalaxyRoles:
  - name: Cassandra 
    hosts: all
    galaxy: locp.cassandra
    varsFile: cassandra-vars.yml
```
Because Cassandra needs quite a lot additional variables, you may include these via an own Yaml file (e.g. cassandra-vars.yml)
to keep your configuration file transparent.
The author (locp) provides variables for a very basic configuration:  

*cassandra-vars.yml*
```
cassandra_configuration:
  authenticator: PasswordAuthenticator
  cluster_name: MyCassandraCluster
  commitlog_directory: /data/cassandra/commitlog
  commitlog_sync: periodic
  commitlog_sync_period_in_ms: 10000
  data_file_directories:
    - /data/cassandra/data
  endpoint_snitch: GossipingPropertyFileSnitch
  hints_directory: "/data/cassandra/hints"
  listen_address: "{{ ansible_default_ipv4.address }}"
  partitioner: org.apache.cassandra.dht.Murmur3Partitioner
  saved_caches_directory: /data/cassandra/saved_caches
  seed_provider:
    - class_name: "org.apache.cassandra.locator.SimpleSeedProvider"
      parameters:
        - seeds: "{{ ansible_default_ipv4.address }}"
      start_native_transport: true
cassandra_configure_apache_repo: true
cassandra_dc: DC1
# Create an alternative directories structure for the Cassandra data.
# In this example, the will be a directory called /data owned by root
# with rwxr-xr-x permissions.  It will have a series of sub-directories
# all of which will be defaulted to being owned by the cassandra user
# with rwx------ permissions.
cassandra_directories:
    root:
    group: root
    mode: "0755"
    owner: root
    paths:
        - /data
    data:
    paths:
        - /data/cassandra
        - /data/cassandra/commitlog
        - /data/cassandra/data
        - /data/cassandra/hints
        - /data/cassandra/saved_caches
cassandra_rack: RACK1
cassandra_repo_apache_release: 311x
```  

To check if your configuration went well you can try to use the Cassandra Query Language Shell:  

```
> cqlsh
```

It's as simple as this! Now you can create databases among the cluster nodes.

### Set up MySQL on the master instance
To configure the MySQL Server on your cluster environment, simply add a suitable role within your ansible galaxy configuration:
```
ansibleGalaxyRoles:
  - name: mysql                                     # Redefine role name
    hosts: master                                   # Install role on master
    galaxy: geerlingguy.mysql                       # Galaxy name of role
    vars:                                           # Necessary variables
      mysql_root_password: super-secure-password
      mysql_databases:                              # Here you may specify yiour database
        - name: example_db
          encoding: latin1
          collation: latin1_general_ci
      mysql_users:                                  # Create user(s) with permissions
        - name: example_user
          host: "%"
          password: similarly-secure-password
          priv: "example_db.*:ALL"                  # permission on specified database
```
This ansible galaxy role is from author geerlingguy who provides many useful ansible roles. 
After cluster setup and login on master you can check, if everything is going right:  

``` > mysql --user example_user -p ```  

Replace 'example_user' and type in your password afterwords - You should end up connected to the MySQL Server.

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

If necessary multiple clusters can be terminated at once:

```
> bibigrid -t [id1]/[id2]/[id3] -v -o ~/config.yml
```
There is also the possibility to terminate all clusters of a user at once:
```
> bibigrid -t [user] -v -o ~/config.yml
```
Here you have to insert your username instead of '[user]'. This may save time, 
if you are absolutely certain you don't need any of your clusters anymore.
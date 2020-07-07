# Getting Started
Starting a cluster requires a valid configuration file and credentials of the cloud provider. Following are the necessary steps with 
detailed information. BiBiGrid is based on OpenStack, if you're using another cloud provider take a look at the [specific setup](cloud-providers/CONFIGURATION.md).
For an up-to-date user Setup-Tutorial for the BiBiGrid visit [de.NBI Wiki](https://cloud.denbi.de/wiki/Tutorials/BiBiGrid/).

### Setting up credentials
For communication with the cloud provider API, credentials have to be setup.
Therefore you have to place a public SSH Key on the OpenStack server and set up the API credentials.
Visit [OpenStack credentials setup](../bibigrid-openstack/docs/Credentials_Setup.md) for a detailed credentials setup tutorial.
Additionally during cluster creation the master instance will handle software updates and installations for all cluster instances using ansible.

### Writing the configuration file
The configuration file specifies the composition of the requested cluster. Many parameters are shared across all cloud providers, however some parameters are provider specific.
You can either provide the necessary parameters via the command line, by using a configuration file in yaml format or in some cases by using environment variables.

A complete list of **command line parameters** can be found [here](COMMAND_LINE.md).

A complete schema for a **configuration file** can be found [here](CONFIGURATION_SCHEMA.md).

**Configuration File Schema**  
The configuration file is written in YAML format. In contrast to the command line
parameters, a configuration file is easier to maintain and in some cases provides
more detailed configuration possibilities.

#### Writing and using a configuration file
The configuration file is a plain text file in YAML format.  
  
*example.yml*

```
#configuration.yml
mode: openstack
credentialsFile: /HOME_DIR/.bibigrid/credentials.yml

#Access
sshPublicKeyFile: 
  - "/HOME_DIR/.ssh/id_rsa.pub"
sshUser: ubuntu
region: Bielefeld
availabilityZone: default

network: default
subnet: default

masterInstance:
  type: f1-micro
  image: 'Ubuntu 18.04 LTS (2019-08-08)'

workerInstances:
  - type: f1-micro
    count: 2
    image: 'Ubuntu 18.04 LTS (2019-08-08)'

ports:
  - type: TCP
    number: 80
  - type: TCP
    number: 443

nfs: yes
theia: yes
slurm: yes
```

This file can now be included using the "-o" command line parameter and the path to the configuration file.

### Creating a bibigrid alias
To keep the cluster setup process simple you can set an alias for the BiBiGrid JAR file installed before. 
The Unix command should look like the following (depending on JAR filename):
```
> alias bibigrid="java -jar /path/to/bibigrid-*.jar"
```

Instead of the java command there will be used the 'bibigrid' command just created. 
Since the alias only applies in the current terminal session, it is thus recommended to add it to the 
'.bashrc' file in the home directory to use it persistently.

### Validating the cluster configuration
Before starting the cluster directly after writing the configuration file, several components can be validated via the check command '-ch' beforehand.
This prevents the majority of possible errors or typos, resulting in incomplete cluster setups.
```
> bibigrid -ch -v -o config.yml
```
The command will be executed by default when creating a new cluster.

### Starting the cluster
Once the configuration is validated, the creation of the cluster can be started. Depending on the parameters
this may take some time.
```
> bibigrid -c -v -o config.yml
```

### Starting the Web IDE
Enable the Theia IDE in the configuration file using `theia: yes`. The IDE can be started with the following command:
```
> bibigrid --ide [cluster-id] -v -o config.yml
```
The process will run until an input is provided or it's terminated. 
The IDE is available under [http://localhost:8181](http://localhost:8181) and will be started automatically. 
If you are using multiple sessions on the same server the port will be incremented until an upper limit. 
For more flexibility you can set the IDE ports in the configuration.

### Monitoring via Zabbix Web Frontend
If you want to monitor a cluster, you can integrate the Zabbix support into your configuration file:
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

### Including Ansible (Galaxy) Roles
You can include ansible roles from your local machine (compressed as .tar.gz files) automatically into your cluster setup by defining following configuration settings:

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
Apache Cassandra is a NoSQL database management system which you can use on your cluster. You may include it as follows:
```
ansibleGalaxyRoles:
  - name: Cassandra 
    hosts: all
    galaxy: locp.cassandra
    varsFile: cassandra-vars.yml
```
Cassandra needs quite a lot additional variables, thus you may include these via an own Yaml file (e.g. cassandra-vars.yml)
to keep your configuration file transparent.
The author of the galaxy role (locp) provides variables for a very [basic configuration](https://galaxy.ansible.com/locp/cassandra):  

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

To check if your configuration went well, you can try to use the Cassandra Query Language Shell:  

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
          priv: "example_db.*:ALL"                  # Permission on specified database
```
This ansible galaxy role is from the author geerlingguy who provides many other useful ansible roles, too. 

You can check, if everything went right (after cluster setup and login on master) this way:  

``` > mysql --user <example_user> -p ```  

Replace 'example_user' and type in your password afterwords - You should end up connected to the MySQL Server.

## Cluster maintenance
### List running clusters
Once a cluster is created, it can be listed with the following command. All clusters found
with the selected provider will be listed, including some detail information.

```
> bibigrid -l -o config.yml
```

Example output:

```
     cluster-id |       user |         launch date |           key name |       public-ip |  # inst |    group-id |   subnet-id |  network-id
-----------------------------------------------------------------------------------------------------------------------------------------------
fkiseokf34ekfeo |   testuser |   20/08/19 09:25:10 |   bibigrid-fkis... |    XXX.XX.XX.XX |       3 | a45b6a63-.. |           - |           -
```

To get additional information about the nodes a specific cluster, you can add the cluster-id to the previous command:  

`> bibigrid -l <cluster-id> -o config.yml`

### Scaling the cluster manually
If you want to shut down single worker instances of the cluster or append some new, you may do so in the following way:  

**Upscaling**  

Append new instances with the configuration of a specified batch in the following way:
```
> bibigrid -su <bibigrid-id> <batch-index> <count>
```
Use the bibigrid id of the cluster you want to scale up. You than take the batch-index of worker-set to 
specify the configuration of the new one(s). You can look it up via `bibigrid -l <cluster-id>`. At last you can specify, 
with how many instances you want to expand your cluster.  

**Downscaling**  

You can scale down the cluster the same way you scale it up, just replace `-su` or `--scale-up` with `-sd` or `--scale-down`.
```
> bibigrid -sd <bibigrid-id> <batch-index> <count>
```
### Terminate the cluster
When you're finished using the cluster, you can terminate it using the following command and the logged cluster-id 
when the cluster was created. The SSH Key Pair in the *.bibigrid/keys* folder will be deleted since they are only used once per cluster.

```
> bibigrid -t <cluster-id> -v -o config.yml
```

You can also terminate multiple clusters at once:

```
> bibigrid -t <id1> <id2> <id3> -v -o config.yml
```

Additionally, you have the possibility to terminate all clusters of a specific user at once:

```
> bibigrid -t <user> -v -o config.yml
```

Here you have to insert your username instead of '[user]'. This may save time, 
if you are absolutely certain you don't need any of your clusters anymore.
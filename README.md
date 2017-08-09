# BiBiGrid
BiBiGrid is a tool for an easy cluster setup inside a cloud environment.
It is written in Java and run on any OS a Java runtime is provided - any 
Java 8 is supported. BiBiGrid and its Cmdline UI based on a general cloud 
provider api. Currently there exists implementations for Amazon (AWS EC2 
using the official AWS SDK) and OpenStack (openstack4j). BiBiGrid offers 
an easy configuration and maintenance of a started cluster via command-line.

BiBiGrid instance images are based on a standard Ubuntu 14.04 LTS distribution 
with a preinstalled software for Grid Computing, distributed databases and 
filesystems and many more. During resource instantiation BiBigrid configures 
the network, local and network volumes , (network) file systems and the 
preinstalled software for an immediately usage of the started cluster. A full 
configured and ready to use cluster is available within a few minutes.


## Compile & Package

Java >= 8 and Maven >= 3.0.4  is required to build and run BiBiGrid.

~~~BASH
> git clone https://github.com/BiBiServ/bibigrid.git
> cd bibigrid
> mvn clean package
~~~

## Development-Guidelines

https://github.com/BiBiServ/Development-Guidelines

## Usage:

Sourcing the bibigrid.sh script could make your life a bit easier.

~~~BASH
> source bibigrid.sh
~~~

Before using BiBiGrid you have to create a configuration (default: `~/.bibigrid.properties`)

### Start a new cluster

	> bibigrid -c 
	
### List running cluster

	> bibigrid -l
	
###  Terminate a cluster
	
	> bibigrid -t

## example .bibigrid.properties

- 1 master node 
- 4 worker nodes
- Gridengine, Spark and NFS services enabled
- do not use the master node for compute purpose
- opens port 80/443  (webserver) additional to default ssh port


~~~BASH
#use openstack
mode=openstack

#Access
identity-file=/Users/juser/.ssh/id_rsa
keypair=juser
region=RegionOne
availability-zone=nova

#Network
subnet=my-subnet

#BiBiGrid-Master
master-instance-type=de.NBI.medium
master-image=<ID of bibigrid-master image>

#BiBiGrid-Slave
slave-instance-type=de.NBI.medium
slave-instance-count=4
slave-image=<ID of bibigrid-slave image>

#Firewall/Security Group
ports=80,443

use-master-as-compute=no
#services
nfs=yes
oge=yes
spark=yes
~~~
The properties file **must** adjusted to your cloud configuration. See homepage for a more detail usage description

## Hompage
https://wiki.cebitec.uni-bielefeld.de/bibiserv-1.25.2/index.php/BiBiGrid

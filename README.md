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

~~~BASJ
java -jar BiBiGrid-1.0.jar <options>
~~~

See homepage for a more detail usage description

## Hompage
https://wiki.cebitec.uni-bielefeld.de/bibiserv-1.25.2/index.php/BiBiGrid

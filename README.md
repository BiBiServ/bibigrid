# BiBiGrid
BiBiGrid is a tool for an easy cluster setup inside a cloud environment.
It is written in Java and run on any OS a Java runtime is provided - any 
Java 8 is supported. BiBiGrid and its Cmdline UI based on a general cloud 
provider api. Currently the implementation is based on OpenStack ([Openstack4j](http://openstack4j.com)).  
There also exists implementations for Google (Compute Engine, using the official Google Cloud SDK), 
Amazon (AWS EC2 using the official AWS SDK) and Microsoft (Azure using the official Azure SDK) (WIP)
which are currently not provided tested.
BiBiGrid offers an easy configuration and maintenance of a started cluster via command-line.

BiBiGrid uses [Ansible](https://www.ansible.com) to configure standard Ubuntu 16.04 LTS 
as well as Ubuntu 18.04 LTS cloud images. Depending on your configuration BiBiGrid can set up
an HCP cluster for grid computing ([Open Grid Engine](http://gridscheduler.sourceforge.net)), 
a shared filesystem (on local discs and attached volumes), a cloud IDE for writing, running and debugging 
([Cloud9](https://github.com/c9/core)) and many more.

During resource instantiation BiBiGrid configures the network, local and network volumes, (network) file systems and also the software for an immediately usage of the started cluster. 

When using preinstalled images a full configured and ready to use cluster is available within a few minutes.


## Compile, Build & Package

*Requirements: Java >= 8, Maven >= 3.3.9*

Each cloud provider SDK comes with a set of dependencies which often conflicts with dependencies of other SDK 
(same library, different major version) when building a shaded (fat) jar. The BiBiGrid POM supports Maven profiles 
to avoid such dependency conflicts.  

~~~BASH
> git clone https://github.com/BiBiServ/bibigrid.git
> cd bibigrid
~~~

### Default profile (all supported cloud provider)
*Attention: Building a package supporting all cloud provider is possible but not recommended. 
The Maven package action put the first occurance of a library/class into the builded shaded jar. 
This could lead to an unpredictable behaviour when running BiBiGrid. 
The default profile is mainly used by IDEs with Maven support.*

~~~BASH
> mvn clean package
~~~

### OpenStack

~~~BASH
> mvn -P openstack clean package
~~~

### Amazon Web Services (AWS)

~~~BASH
> mvn -P aws clean package
~~~

### Microsoft Azure

~~~BASH
> mvn -P azure clean package
~~~

### GoogleCloud

~~~BASH
> mvn -P googlecloud clean package
~~~

## Getting Started 
Using BiBiGrid requires a valid cluster configuration and credentials for your cloud provider. 
The setup can differ a bit depending on the used cloud backend. See [BiBiGrid documentation](docs/README.md) 
for detailed configuration and usage information.  
Notice also the [deNBI Wiki](https://cloud.denbi.de/wiki/Tutorials/BiBiGrid/) for a complete setup guide.

## Development-Guidelines

[https://github.com/BiBiServ/Development-Guidelines](https://github.com/BiBiServ/Development-Guidelines)




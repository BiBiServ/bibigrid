# Command Line Arguments
Most configuration parameters can be provided to BiBiGrid using the command line. 
However, this becomes messy fast and parameters like the password should not be 
provided in plain text via the command line anyways. 
The recommended alternative is to use a [configuration file](CONFIGURATION_SCHEMA.md).

### 
The parameters you might have to add outside the config YAML are explained in the following list:

| Long parameter | Short parameter | Values           | Description                        |
|----------------|-----------------|------------------|------------------------------------|
| check          | ch              | -                | validate cluster setup             |
| cloud9         | c9              | cluster-id       | establish a secured connection to running grid running cloud9 [deprecated] |
| ide            | ide             | cluster-id       | establish a secured connection to specified ide |
| create         | c               | -                | create cluster environment         |
| config         | o               | path/to/config   | YAML configuration file            |
| help           | h               | -                | Display help message               |
| list           | l               | -                | lists all started clusters         |
| prepare        | p               | -                | prepares cluster setup             |
| terminate      | t               | cluster-id       | terminate cluster                  |
| verbose        | v               | -                | increases logging level during setup |
| version        | V               | -                | Check version                      |
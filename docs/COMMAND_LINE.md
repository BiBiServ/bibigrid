# Command Line Arguments
Most configuration parameters can be provided to BiBiGrid using the command line. 
However, this becomes messy fast and parameters like the password should not be 
provided in plain text via the command line anyways. 
The recommended alternative is to use a [configuration file](CONFIGURATION_SCHEMA.md).

### 
The parameters you might have to add outside the config YAML are explained in the following list:

| Long parameter | Short parameter | Values           | Description                        |
|----------------|-----------------|------------------|------------------------------------|
| help           | h               | -                | Display help message               |
| version        | V               | -                | Display version                    |
| list           | l               | -                | Lists all running clusters         |
| verbose        | v               | -                | Increases logging level during setup |
| check          | ch              | -                | Validate cluster configuration     |
| debug          | d               | -                | Prevent BiBiGrid to shutdown a failed cluster setup |
| create         | c               | -                | Create cluster                     |
| config         | o               | path/to/config   | YAML configuration file            |
| ide            | ide             | cluster-id       | Establish a secured connection to specified ide |
| scale-down     | sd              | cluster-id batch-index count | scale down a running cluster |
| scale-up       | su              | cluster-id batch-index count | scale up a running cluster |
| terminate      | t               | cluster-id       | Terminate cluster                  |

# Alternative Cloud Provider Configuration 
*Using alternative cloud providers is not recommended as they will not remain supported since BiBiGrid Version 2.0.8.*

## Setting up credentials
For the BiBiGrid to communicate with the Cloud Provider API you have to setup credentials. 
Use the One-Time SSH Public Key Setup analogously.

* [Google Compute credentials setup](../../bibigrid-googlecloud/docs/Credentials_Setup.md)
* [Amazon AWS credentials setup](../../bibigrid-aws/docs/Credentials_Setup.md)
* [Microsoft Azure credentials setup](../../bibigrid-azure/docs/Credentials_Setup.md)

## Writing the configuration file
For the [configuration file](../CONFIGURATION_SCHEMA.md) you may have to setup a few provider specific parameters:

**Google Compute specific schema**
```
googleProjectId: string                             # ID of Google Compute Engine Project
googleImageProjectId: string                        # ID of Image Project 
```

**AWS specific schema**
```
bidPrice: double                                    # Bid Price in USD ($) for each Amazon EC2 instance when launched as Spot Instances
bidPriceMaster: double
publicWorkerIps: boolean [yes, no]                   # Every worker gets public IP
useSpotInstances: boolean [yes, no]                 # Usage of unused EC2 capacity in AWS cloud at discount price
```

**Azure specific schema**

There are currently no azure specific parameters.
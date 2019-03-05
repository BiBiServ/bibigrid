# Examples OpenStack

In order to get a feeling for writing OpenStack specific configuration files or command lines for the different intent modes,
the following examples represent minimal required parameters.

## Validate
No parameter is necessary for validation, however the same parameters should be used as for the creation of the cluster
to validate the configuration to be used.

## Create & Prepare
*Configuration file*
```
# Direcly in cofiguration file or using the credentialsFile parameter
openstackCredentials:
    projectName: string
    username: string
    password: string
    endpoint: string

sshUser: string
keypair: string
sshPrivateKeyFile: string

region: string
availabilityZone: string

useMasterAsCompute: boolean
masterInstance:
  type: string
  image: string

slaveInstances:
  - type: string
    count: integer
    image: string
```

## List
*Configuration file*
```
# Direcly in cofiguration file or using the credentialsFile parameter
openstackCredentials:
    projectName: string
    username: string
    password: string
    endpoint: string

region: string
```

## Terminate
*Command line*
```
bibigrid -t [cluster-id] [...]
```

*Configuration file*
```
# Direcly in cofiguration file or using the credentialsFile parameter
openstackCredentials:
    projectName: string
    username: string
    password: string
    endpoint: string

region: string
```

## Cloud9
*Command line*
```
bibigrid -cloud9 [cluster-id] [...]
```

*Configuration file*
```
# Direcly in cofiguration file or using the credentialsFile parameter
openstackCredentials:
    projectName: string
    username: string
    password: string
    endpoint: string

sshUser: string
keypair: string
sshPrivateKeyFile: string
```
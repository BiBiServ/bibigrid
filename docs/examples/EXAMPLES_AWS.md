# Examples AWS

In order to get a feeling for writing AWS specific configuration files or command lines for the different intent modes,
the following examples represent minimal required parameters.

## Validate
*Configuration file*
```
credentialsFile: string
keypair: string

sshPrivateKeyFile: string

region: string
availabilityZone: string

masterInstance:
  type: string
  image: string

slaveInstances:
  - type: string
    count: integer
    image: string
```

## Create & Prepare
*Configuration file*
```
credentialsFile: string
keypair: string

sshUser: string
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
credentialsFile: string
keypair: string

region: string
```

## Terminate
*Command line*
```
bibigrid -t [cluster-id] [...]
```

*Configuration file*
```
credentialsFile: string

region: string
```

## Cloud9
*Command line*
```
bibigrid -cloud9 [cluster-id] [...]
```

*Configuration file*
```
credentialsFile: string
keypair: string

sshUser: string
sshPrivateKeyFile: string
```

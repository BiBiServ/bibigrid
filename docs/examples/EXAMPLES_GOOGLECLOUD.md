# Examples Google Compute

In order to get a feeling for writing Google Compute specific configuration files or command lines for the different intent modes,
the following examples represent minimal required parameters.

## Validate
*Configuration file*
```
credentialsFile: string
googleProjectId: string
googleImageProjectId: string

sshPrivateKeyFile: string

region: string
availabilityZone: string

masterInstance:
  type: string
  image: string

workerInstances:
  - type: string
    count: integer
    image: string
```

## Create & Prepare
*Configuration file*
```
credentialsFile: string
googleProjectId: string
googleImageProjectId: string

sshUser: string
sshPrivateKeyFile: string

region: string
availabilityZone: string

useMasterAsCompute: boolean
masterInstance:
  type: string
  image: string

workerInstances:
  - type: string
    count: integer
    image: string
```

## List
*Configuration file*
```
credentialsFile: string
googleProjectId: string
```

## Terminate
*Command line*
```
bibigrid -t [cluster-id] [...]
```

*Configuration file*
```
credentialsFile: string
googleProjectId: string

region: string
availabilityZone: string
```

## Cloud9
*Command line*
```
bibigrid -cloud9 [cluster-id] [...]
```

*Configuration file*
```
credentialsFile: string
googleProjectId: string

sshUser: string
sshPrivateKeyFile: string
```

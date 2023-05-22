# Create

Creates the cluster and prints information regarding further actions.
Temporary cluster keys will be stored in `~/.config/bibigrid/keys`.

## Generates a keypair
Using `ssh-keygen -t ecdsa` a keypair is generated.
This keypair is injected into every started instance and is used by BiBiGrid to connect to instances.

## Generates security groups
### Default Security Group
- allows SSH from everywhere
- 
| Direction | Ethertype | Protocol | Port Range Min | Port Range Max | Remote IP Prefix |
|:---------:|:---------:|:--------:|:--------------:|:--------------:|:----------------:|
|  Ingress  |   IPv4    |   None   |      None      |      None      |       None       |
|  Ingress  |   IPv4    |   TCP    |       22       |       22       |    0.0.0.0/0     |


#### Multi-Cloud
When running a multi-cloud additionally the following rules are set:
- allows every TCP connection from the VPN (10.0.0.0/24)
- allows every TCP connection from other cidrs (other clouds)

| Direction | Ethertype | Protocol | Port Range Min | Port Range Max | Remote IP Prefix |
|:---------:|:---------:|:--------:|:--------------:|:--------------:|:----------------:|
|  Ingress  |   IPv4    |   TCP    |      None      |      None      |   10.0.0.0/24    |
|  Ingress  |   IPv4    |   TCP    |      None      |      None      |   other_cidrs    |

### Wireguard Security Group
Only created when multi-cloud is used (more than one configuration in [configuration](configuration.md) file).

| Direction | Ethertype | Protocol | Port Range Min | Port Range Max | Remote IP Prefix |
|:---------:|:---------:|:--------:|:--------------:|:--------------:|:----------------:|
|           |           |          |                |                |                  | TODO


## Starts master and vpnwkrs

## Configures network

## Uploads Data

## Executes Ansible

## Prints Cluster Information
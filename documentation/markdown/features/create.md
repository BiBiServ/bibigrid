# Create

Creates the cluster and prints information regarding further actions.
Temporary cluster keys will be stored in `~/.config/bibigrid/keys`.

## Generates a keypair
Using `ssh-keygen -t ecdsa` a keypair is generated.
This keypair is injected into every started instance and is used by BiBiGrid to connect to instances.


## Configure Network
### Generates security groups
- When `Remote Security Group ID` is set, the rule only applies to nodes within that group id. 
The rule cannot apply to nodes outside the cloud.
#### Default Security Group
- allows SSH from everywhere
- allows everything within the same security group
- 
| Direction | Ethertype | Protocol | Port Range Min | Port Range Max | Remote IP Prefix | Remote Security Group ID |
|:---------:|:---------:|:--------:|:--------------:|:--------------:|:----------------:|:------------------------:|
|  Ingress  |   IPv4    |   None   |      None      |      None      |       None       |  Default Security Group  |
|  Ingress  |   IPv4    |   TCP    |       22       |       22       |    0.0.0.0/0     |           None           |


##### Default Security Group - Extra Rules: Multi-Cloud
When running a multi-cloud additionally the following rules are set:
- allows every TCP connection from the VPN (10.0.0.0/24)
- allows every TCP connection from other cidrs (other clouds)

| Direction | Ethertype | Protocol | Port Range Min | Port Range Max | Remote IP Prefix | Remote Security Group ID |
|:---------:|:---------:|:--------:|:--------------:|:--------------:|:----------------:|:------------------------:|
|  Ingress  |   IPv4    |   TCP    |      None      |      None      |   10.0.0.0/24    |           None           |
|  Ingress  |   IPv4    |   TCP    |      None      |      None      |   other_cidrs    |           None           |

#### Wireguard Security Group
Only created when multi-cloud is used (more than one configuration in [configuration](configuration.md) file).
- allow every UDP connection from the other clouds over 51820 (necessary for [WireguardVPN](../software/wireguard.md)).

| Direction | Ethertype | Protocol | Port Range Min | Port Range Max | Remote IP Prefix | Remote Security Group ID |
|:---------:|:---------:|:--------:|:--------------:|:--------------:|:----------------:|:------------------------:|
|  Ingress  |   IPv4    |   UDP    |     51820      |     51820      |   other_cidrs    |           None           |

### Allowed Addresses
- For every cloud C, all other clouds' cidr is set as an `allowed_address` with the mac address of C.
This prevents outgoing addresses with the "wrong" mac address, ip combination from getting stopped by port security.

## Starts master and vpnwkrs

For the first configuration a master, for all others a vpnwkr is started.

## Uploads Data

The [playbook](../../../resources/playbook) and [bin](../../../resources/bin) is uploaded.

## Executes Ansible

### Preparation
- Automatic updates are deactivated on host machine
- Python is installed
- Move playbook contents to new home `/opt/playbook/` and set rights accordingly
- Wait until dpkg lock is released
- Install `ansible.cfg` to `/etc/ansible/ansible.cfg`

### Execution

The playbook is executed. Read more about the exact steps of execution [here](bibigrid_ansible_playbook.md).

## Prints Cluster Information

At the end the cluster information is printed:
- cluster id
- master's public ip
- How to connect via SSH
- How to terminate the cluster
- How to print detailed cluster info
- How to connect via IDE Port Forwarding (only if [ide](configuration.md#ide-optional))
- Duration

### Print Example
```
Cluster myclusterid with master 123.45.67.89 up and running!
SSH: ssh -i '/home/user/.config/bibigrid/keys/tempKey_bibi-myclusterid' ubuntu@123.45.67.89
Terminate cluster: ./bibigrid.sh -i '/home/xaver/.config/bibigrid/hybrid.yml' -t -cid myclusterid
Detailed cluster info: ./bibigrid.sh -i '/home/xaver/.config/bibigrid/hybrid.yml' -l -cid myclusterid
IDE Port Forwarding: ./bibigrid.sh -i '/home/xaver/.config/bibigrid/hybrid.yml' -ide -cid myclusterid
--- 12 minutes and 0.9236352443695068 seconds ---
```

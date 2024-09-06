# Multi-Cloud

Multi-Cloud BiBiGrid allows for an easy cluster creation and management across multiple clouds.
With this configuration slurm will span over all given clouds and NFS share will be accessible by every node independent of its cloud.
Due to the high level of abstraction (VPN), using BiBiGrid's multi-cloud clusters is no more difficult than BiBiGrid's single cloud cluster.
However, the [configuration](configuration.md#configuration-list) (which contains all relevant information for most users) 
of course needs to contain two cloud definitions, and you need access to both clouds.
Due to BiBiGrid's cloud separation by partition, users can specifically address individual clouds.

Slides briefly covering the development: [ELIXIR Compute 2023 -- Multi-Cloud - BiBiGrid.pdf](../../pdfs/ELIXIR%20Compute%202023%20--%20Multi-Cloud%20-%20BiBiGrid.pdf).

What follows are implementation details that are not relevant for most users.

## DNS Server
DNS is provided by [dnsmasq](../software/dnsmasq.md). All instances are added whether they are started once (master, vpngtw)
or on demand (workers). Explicitly, BiBiGrid manages adding workers to [dnsmasq](../software/dnsmasq.md) on creation
triggered by [create_server](../../../resources/playbook/roles/bibigrid/files/slurm/create_server.py) and executed by ansible 
by task [003-dns.yaml](../../../resources/playbook/roles/bibigrid/tasks/003-dns.yaml).

## VPN - Wireguard
[Wireguard](../software/wireguard.md) creates a VPN between all vpngtw and the master node.

### Keypair
A single keypair (X25519 encrypted and base64 encoded) is [generated](../../../bibigrid/core/utility/wireguard/wireguard_keys.py) by BiBiGrid on cluster 
creation and distributed via SSH to the master and every vpngtw.  

### Interface
Using systemd-network a persistent wg0 interface is [created by ansible](../../../resources/playbook/roles/bibigrid/tasks/002-wireguard-vpn.yaml)
in order to enable Wireguard.

## Port Security
Default, OpenStack prevents packages from **leaving** instances, if IP and MAC do not match.
This mismatch happens, because vpngtws are forwarding packages from master to remote workers and from workers back to master.
While forwarding the MAC Address changes, but the IP remains the IP of the worker/master. Therefore, IP and MAC mismatch.

By adding `Allowed Address Pairs` OpenStack knows that it should allow those mismatch packages.
These `Allowed Address Pairs` are added to the master and to every vpngtw.

For that Ansible creates [userdata](../../../resources/playbook/roles/bibigrid/tasks/042-slurm-server.yaml)
files that are later [injected](../../../resources/playbook/roles/bibigrid/files/slurm/create_server.py)
into started worker instances by `create_server.py` triggered by slurm.

## MTU Probing
MTU Probing is necessary, because MTU might be different across networks. [Ansible handles that]([created by ansible](../../../resources/playbook/roles/bibigrid/tasks/002-wireguard-vpn.yaml).

## IP Routes
In order to allow workers to communicate over the vpn, they need to know how to use it.
Therefore, IP routes are [set by ansible]([deactivated by ansible](../../../resources/playbook/roles/bibigrid/tasks/000-add-ip-routes.yaml)
for the workers, telling them how to contact the master. 

## Deactivating Netplan
Netplan is [deactivated by ansible](../../../resources/playbook/roles/bibigrid/tasks/000-add-ip-routes.yaml)
in order to avoid set ip routes being overwritten.
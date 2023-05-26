# Terminate Cluster
Terminates a cluster. Asks for confirmation if debug mode is active or no local keypair matching cluster id can be found.

## Delete Local Keypairs
Local keypairs are deleted, because they are no longer needed after cluster termination.
Keypairs are stored in `~/.config/bibigrid/keys`

## Terminate Servers
All servers from all clusters are deleted.

## Delete Remote Keypairs
All remote keypairs are deleted.

## Delete Application Credentials
Application credentials are deleted - if application credentials have been created.

## Security Groups
Security Groups are deleted (default and wireguard (if multi-cloud)).
BiBiGrid will attempt security group deletion multiple times, because providers need a little time betweens server deletion and
security group deletion.
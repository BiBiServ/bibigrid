# BiBiGrid Cluster Commands

## [bibiname](../../../resources/playbook/roles/bibigrid/templates/bin/bibiname.j2)[m|v|default: w] [number]

This command creates node names for the user without them needing to copy the cluster-id.
Takes two arguments. The first defines whether a master, vpnwkr or worker is meant. Worker is the default.
The second parameter - if vpnwkr or worker is selected - defines which vpnwkr or worker is meant.

### Examples
Assume the cluster-id `20ozebsutekrjj4`.

```sh
bibiname m
# bibigrid-master-20ozebsutekrjj4
```

```sh
bibiname v 0
# bibigrid-vpnwkr-20ozebsutekrjj4-0
```

```sh
bibiname 0 # or bibiname w 0
# bibigrid-worker-20ozebsutekrjj4-0
```

A more advanced use would be to use the generated name to login into a worker:
```sh
ssh $(bibiname 0) # or bibiname w 0
# ssh bibigrid-worker-20ozebsutekrjj4-0
```
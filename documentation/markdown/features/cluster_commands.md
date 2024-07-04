# BiBiGrid Cluster Commands

## [bibiinfo](../../../resources/bin/bibiinfo)
Similar to `sinfo` but shows detailed information regarding node features.

## [bibilog](../../../resources/bin/bibilog)
`bibilog` executes `tail -f` on the most recent worker creation out log.
Thereby, it helps you with understanding any worker startup issues.

## [bibiplay](../../../resources/bin/bibiplay)
`bibiplay` is mainly a shortcut for `ansible-playbook /opt/playbook/site.yaml -i /opt/playbook/ansible_hosts`
which allows you to execute the ansible playbook more easily.

### Examples
You have changed something in the common configuration and want to propagate this change to the master.
```sh
bibiplay -l master
# executes the playbook only for the master
```

You have changed something in the slurm configuration and want to propagate this change to the master.
```sh
bibiplay -l master -t slurm
```

## [bibiname](../../../resources/playbook/roles/bibigrid/templates/bin/bibiname.j2)[m|v|default: w] [number]

This command creates node names for the user without them needing to copy the cluster-id.
Takes two arguments. The first defines whether a master, vpngtw or worker is meant. Worker is the default.
The second parameter - if vpngtw or worker is selected - defines which vpngtw or worker is meant.

### Examples
Assume the cluster-id `20ozebsutekrjj4`.

```sh
bibiname m
# bibigrid-master-20ozebsutekrjj4
```

```sh
bibiname v 0
# bibigrid-vpngtw-20ozebsutekrjj4-0
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
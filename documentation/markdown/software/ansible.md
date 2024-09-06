# Ansible

## Ansible Tutorial

- [Ansible Workshop Presentation](https://docs.google.com/presentation/d/1W4jVHLT8dB1VsdtxXqtKlMqGbeyEWTQvSHh0WMfWo2c/edit#slide=id.p10)
- [de.NBI Cloud's Ansible Course](https://gitlab.ub.uni-bielefeld.de/denbi/ansible-course)

## Executing BiBiGrid's Playbook Manually

Only execute BiBiGrid's playbook manually when no worker is up. The playbook is executed automatically for workers
powering up.

If you've implemented changes to BiBiGrid's playbook, you might want to execute BiBiGrid's playbook manually to see how
those changes play out. For this we need the preinstalled `bibigrid-playbook` command. However, BiBiGrid has a handy
shortcut for that called `bibiplay`.

### bibiplay

To make things easier we wrote the [bibiplay](..%2F..%2F..%2Fresources%2Fbin%2Fbibiplay) wrapper. It's used like this:

```sh
bibiplay
```

is the same as:

```sh
ansible-playbook /opt/playbook/site.yaml /opt/playbook/ansible_hosts/
```

any additional arguments are passed to `ansible-playbook`:

```sh
bibiplay -l master
```

is the same as:

```sh
ansible-playbook /opt/playbook/site.yaml /opt/playbook/ansible_hosts/ -l master
```

### Useful commands

For more options see [ansible-playbook's manpage](https://linux.die.net/man/1/ansible-playbook).

|                Summary                |            Command            |
|:-------------------------------------:|:-----------------------------:|
|        Prepare master manually        |     `bibiplay -l master`      |
| Prepare only slurm on master manually | `bibiplay -l master -t slurm` |

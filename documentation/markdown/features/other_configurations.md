# Other Configurations
Besides the general BiBiGrid configuration there is also an `ansible.cfg` and a `slurm.conf`.
For 99% of all users those never need to be touched. However, some use cases require changes to those configurations.
For that purpose we store defaults in `resources/defaults` and on the first run copy copies to the actual locations
`resources/playbook/ansible.cfg` and `resources/playbook/bibigrid/templates/slurm/slurm.j2`.
That way you can make changes and if something doesn't work, you can just delete the configuration to go back to our
default one.

## slurm.j2
The `slurm.j2` is not a static configuration file, but instead a [jinja](https://jinja.palletsprojects.com/en/3.1.x/) template for the actual configuration that is
generated during runtime. That is necessary because it contains the actual instance names that are only known at runtime.
The jinja template is converted to the actual configuration by ansible in the `042-slurm.yaml` task.

The `slurm.j2` also takes certain information from your BiBiGrid configuration (see [slurmConf](configuration.md#slurmconf-optional)).

Read more about the `slurm.conf` [here](https://slurm.schedmd.com/slurm.conf.html). 

## ansible.cfg
The `ansible.cfg` defines how ansible behaves during runtime. A key that sometimes need to be adapted is `timeout` which
is the timeout for the connection plugin. If your host answers very slowly, a low timeout might cause issues.

Read more about the `ansible.cfg` [here](https://docs.ansible.com/ansible/latest/reference_appendices/config.html).
#!/bin/bash

# redirect stderr and stdout
exec >> /var/log/slurm/create.out.log
exec 2>> /var/log/slurm/create.err.log


hosts=$(scontrol show hostnames "$1")


# create and configure requested instances
python3 /usr/local/bin/create_server.py "${hosts}"
exit $?
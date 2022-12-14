#!/bin/bash

# redirect stderr and stdout
exec >> /var/log/slurm/fail.out.log
exec 2>> /var/log/slurm/fail.err.log

# $1 is in slurm node format for example: bibigrid-worker0-cid-[0-1],bibigrid-worker1-cid-0 and needs no converting
scontrol update NodeName="$1" state=RESUME reason=FailedStartup # no sudo needed cause executed by slurm user

exit $?

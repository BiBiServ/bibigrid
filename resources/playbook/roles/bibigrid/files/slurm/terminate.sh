#!/bin/bash
# redirect stderr and stdout
exec >> /var/log/slurm/terminate.out.log
exec 2>> /var/log/slurm/terminate.err.log

function log {
   echo "$(date) $*"
}

log "Terminate-Script started"

# $1 is in slurm node format for example: bibigrid-worker0-cid-[0-1],bibigrid-worker1-cid-0 and needs no converting
scontrol update NodeName="$1" state=RESUME reason=FailedStartup # no sudo needed cause executed by slurm user

hosts=$(scontrol show hostnames "$1")

# delete servers
python3 /usr/local/bin/delete_server.py "${hosts}"

exit $?

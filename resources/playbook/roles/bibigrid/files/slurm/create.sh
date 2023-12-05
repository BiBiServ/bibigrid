#!/bin/bash

mkdir -p worker_logs

# redirect stderr and stdout
exec >> "/var/log/slurm/worker_logs/create$(date '+%Y-%m-%d_%H:%M:%S').out.log"
exec 2>> "/var/log/slurm/worker_logs/create$(date '+%Y-%m-%d_%H:%M:%S').err.log"

function log {
   echo "$(date) $*"
}

log "Create-Script started"

hosts=$(scontrol show hostnames "$1")


# create and configure requested instances
python3 /usr/local/bin/create_server.py "${hosts}"
exit $?
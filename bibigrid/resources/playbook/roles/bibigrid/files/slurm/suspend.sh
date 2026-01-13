#!/bin/bash
# redirect stderr and stdout
process_string() {
    # Split the input string by "-"
    IFS='-' read -ra elements <<< "$1"

    # Extract the second, fourth, and fifth elements
    second=${elements[1]}
    fourth=${elements[3]}
    fifth=${elements[4]}

    # Replace undesired characters in the second element
    second=$(echo "$second" | sed -E 's/worker-/worker_/; s/vpngtw-/vpngtw_/')

    # Check if the fifth element is not empty
    if [[ ! -z $fifth ]]; then
        echo "${second}_${fourth}-${fifth}"
    else
        echo "${second}_${fourth}"
    fi
}

mkdir -p worker_logs
mkdir -p worker_logs/suspend
mkdir -p worker_logs/suspend/out
mkdir -p worker_logs/suspend/err

# redirect stderr and stdout
exec >> "/var/log/slurm/worker_logs/suspend/out/$(process_string "$1")_$(date '+%Y-%m-%d_%H:%M:%S').log"
exec 2>> "/var/log/slurm/worker_logs/suspend/err/$(process_string "$1")_$(date '+%Y-%m-%d_%H:%M:%S').log"

function log {
   echo "$(date) $*"
}

log "Suspend-Script started"

# $1 is in slurm node format for example: bibigrid-worker0-cid-[0-1],bibigrid-worker1-cid-0 and needs no converting
scontrol update NodeName="$1" state=RESUME reason=SuspendProgram # no sudo needed cause executed by slurm user

hosts=$(scontrol show hostnames "$1")

# use python bibigrid environment
source /opt/bibigrid-venv/bin/activate

# delete servers
python3 /usr/local/bin/delete_server.py "${hosts}"

exit_code=$?
log "delete_server.py exited with code $exit_code"
exit $?
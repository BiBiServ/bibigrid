#!/bin/bash
# redirect stderr and stdout
exec >> /var/log/slurm/terminate.out.log
exec 2>> /var/log/slurm/terminate.err.log

function log {
   echo "$(date) $*"
}

log "Terminate invoked $0 $*"
# extract all hosts from argumentlist
hosts=$(scontrol show hostnames "$1")
for host in $hosts
do
   # ToDo: Implement better logging in case of an error
   log "Delete instance ${host} from Zabbix host list."
   python3 /usr/local/bin/zabbix_host_delete.py --pwd bibigrid "${host}"
   log "Terminate instance ${host}"
   openstack --os-cloud master server delete "${host}"
   log "done"
done

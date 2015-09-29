mode=openstack
##### META
os-tenantname=bibiserv
os-username=bibiserv
os-password=OpenstackBibiserv
os-endpoint=http://172.21.32.13:5000/v2.0/
##### PEM FILE (AWS)
identity-file=/homes/jsteiner/bibigrid.properties
##### KEYPAIR AND REGION
keypair=jsteiner
region=regionOne
##### AVAILABILITY ZONE (AWS)
availability-zone=nova-x86_64
##### MASTER INSTANCE
master-instance-type=t1.micro
master-image=2c151de8-3c23-4e1c-81af-05e78ae2dcf8
#### SLAVE INSTANCE
slave-instance-type=t1.micro
slave-instance-count=2
slave-image=2c151de8-3c23-4e1c-81af-05e78ae2dcf8
####
ports=80,443,8080,8443
nfs-shares=/vol/biotools/,/vol/biodb/
use-master-as-compute=yes

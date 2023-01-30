#!/usr/bin/env python3
"""
Deletes one or more instances from comma separated name list.
Is called automatically by fail.sh (called by slurm user automatically) which sources a virtual environment.
"""

import logging
import math
from openstack.exceptions import OpenStackCloudException
import re
import sys
import time
import subprocess

import os_client_config
import yaml

LOGGER_FORMAT = "%(asctime)s [%(levelname)s] %(message)s"
logging.basicConfig(format=LOGGER_FORMAT, filename="/var/log/slurm/create_server.log", level=logging.INFO)

logging.info("create_server.py started")
start_time = time.time()

if len(sys.argv) < 2:
    logging.warning("usage:  $0 instance1_name[,instance2_name,...]")
    logging.info("Your input % with length %s", sys.argv, len(sys.argv))
    sys.exit(1)
start_instances = sys.argv[1].split("\n")
logging.info("Starting instances %s", start_instances)

# read instances configuration
with open("/opt/playbook/vars/instances.yml", mode="r") as f:
    instances = yaml.safe_load(f)["instances"]

# read common configuration
with open("/opt/playbook/vars/common_configuration.yml", mode="r") as f:
    common_config = yaml.safe_load(f)

# read clouds.yaml
with open("/etc/openstack/clouds.yaml", mode="r") as f:
    clouds = yaml.safe_load(f)["clouds"]

connections = {}  # connections to cloud providers
for cloud in clouds:
    connections[cloud] = os_client_config.make_sdk(cloud=cloud)

instances_by_cloud_dict = [(key, value) for key, value in instances.items() if key != 'master']

for cloud_name, instances_of_cloud in instances_by_cloud_dict:
    for worker_type in instances_of_cloud["workers"]:
        for worker in start_instances:
            # check if worker in instance is described in instances.yml as part of a worker_type
            result = subprocess.run(["scontrol", "show", "hostname", worker_type["name"]],
                                    stdout=subprocess.PIPE)  # get all workers in worker_type
            possible_workers = result.stdout.decode("utf-8").strip().split("\n")
            if worker in possible_workers:
                result = connections["cloud_name"].delete_server(worker)
            if not result:
                logging.warning(f"Couldn't delete worker {worker}"
            else:
                logging.info(f"Deleted {worker}")
logging.info("Successful delete_server.py execution!")
time_in_s = time.time() - start_time
logging.info(f"--- %s minutes and %s seconds ---", math.floor(time_in_s / 60), time_in_s % 60)
logging.info("Exit Code 0")
exit(0)
#!/usr/bin/env python3
"""
Deletes one or more instances from comma separated name list.
Is called automatically by fail.sh (called by slurm user automatically) which sources a virtual environment.
"""

import logging
import math
import os
import subprocess
import sys
import time

import os_client_config
import yaml

LOGGER_FORMAT = "%(asctime)s [%(levelname)s] %(message)s"
logging.basicConfig(format=LOGGER_FORMAT, filename="/var/log/slurm/delete_server.log", level=logging.INFO)

logging.info("delete_server.py started")
start_time = time.time()

if len(sys.argv) < 2:
    logging.warning("usage:  $0 instance1_name[,instance2_name,...]")
    logging.info("Your input %s with length %s", sys.argv, len(sys.argv))
    sys.exit(1)
terminate_workers = sys.argv[1].split("\n")
logging.info("Deleting instances %s", terminate_workers)

GROUP_VARS_PATH = "/opt/playbook/group_vars"
worker_groups = []
for filename in os.listdir(GROUP_VARS_PATH):
    if filename != "master.yml":
        f = os.path.join(GROUP_VARS_PATH, filename)
        # checking if it is a file
        if os.path.isfile(f):
            with open(f, mode="r", encoding="utf-8") as worker_group:
                worker_groups.append(yaml.safe_load(worker_group))

# read common configuration
with open("/opt/playbook/vars/common_configuration.yml", mode="r", encoding="utf-8") as common_configuration_file:
    common_config = yaml.safe_load(common_configuration_file)

# read clouds.yaml
with open("/etc/openstack/clouds.yaml", mode="r", encoding="utf-8") as clouds_file:
    clouds = yaml.safe_load(clouds_file)["clouds"]

connections = {}  # connections to cloud providers
for cloud in clouds:
    connections[cloud] = os_client_config.make_sdk(cloud=cloud)

for worker_group in worker_groups:
    for terminate_worker in terminate_workers:
        # terminate all servers that are part of the current worker group
        result = subprocess.run(["scontrol", "show", "hostname", worker_group["name"]],
                                stdout=subprocess.PIPE, check=True)  # get all workers in worker_type
        possible_workers = result.stdout.decode("utf-8").strip().split("\n")
        if terminate_worker in possible_workers:
            result = connections[worker_group["cloud_identifier"]].delete_server(terminate_worker)
        if not result:
            logging.warning(f"Couldn't delete worker {terminate_worker}")
        else:
            logging.info(f"Deleted {terminate_worker}")
logging.info("Successful delete_server.py execution!")
time_in_s = time.time() - start_time
logging.info("--- %s minutes and %s seconds ---", math.floor(time_in_s / 60), time_in_s % 60)
logging.info("Exit Code 0")
sys.exit(0)

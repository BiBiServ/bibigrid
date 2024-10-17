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
import re

import os_client_config
import requests
import yaml
from pyzabbix import ZabbixAPI, ZabbixAPIException

LOGGER_FORMAT = "%(asctime)s [%(levelname)s] %(message)s"
logging.basicConfig(format=LOGGER_FORMAT, filename="/var/log/slurm/delete_server.log", level=logging.INFO)

logging.info("delete_server.py started")
start_time = time.time()

logging.info(f"Terminate parameter: {sys.argv[1]}")

if len(sys.argv) < 2:
    logging.warning("usage:  $0 instance1_name[(,\\n)instance2_name,...]")
    logging.info("Your input %s with length %s", sys.argv, len(sys.argv))
    sys.exit(1)

SEPERATOR = ','
if '\n' in sys.argv[1]:
    SEPERATOR = '\n'

terminate_workers = sys.argv[1].split(SEPERATOR)
logging.info("Deleting instances %s", terminate_workers)

GROUP_VARS_PATH = "/opt/playbook/group_vars"
worker_groups = []
for filename in os.listdir(GROUP_VARS_PATH):
    if filename != "master.yaml":
        f = os.path.join(GROUP_VARS_PATH, filename)
        # checking if it is a file
        if os.path.isfile(f):
            with open(f, mode="r", encoding="utf-8") as worker_group:
                worker_groups.append(yaml.safe_load(worker_group))

# read common configuration
with open("/opt/playbook/vars/common_configuration.yaml", mode="r", encoding="utf-8") as common_configuration_file:
    common_config = yaml.safe_load(common_configuration_file)

# read clouds.yaml
with open("/etc/openstack/clouds.yaml", mode="r", encoding="utf-8") as clouds_file:
    clouds = yaml.safe_load(clouds_file)["clouds"]

connections = {}  # connections to cloud providers
for cloud in clouds:
    connections[cloud] = os_client_config.make_sdk(cloud=cloud, volume_api_version="3")

for worker_group in worker_groups:
    for terminate_worker in terminate_workers:
        # terminate all servers that are part of the current worker group
        result = subprocess.run(["scontrol", "show", "hostname", worker_group["name"]], stdout=subprocess.PIPE,
                                check=True)  # get all workers in worker_type
        possible_workers = result.stdout.decode("utf-8").strip().split("\n")
        if terminate_worker in possible_workers:
            connection = connections[worker_group["cloud_identifier"]]
            result = connection.delete_server(terminate_worker)
            logging.info(f"Deleting Volumes")
            volume_list = connection.list_volumes()
            volume_regex = re.compile(fr"^{terminate_worker}-(\d+)$")
            for volume in volume_list:
                if volume_regex.match(volume["name"]):
                    logging.info(f"Trying to delete volume {volume['name']}: {0}") #connection.delete_volume(volume)}")
        if not result:
            logging.warning(f"Couldn't delete worker {terminate_worker}: Server doesn't exist")
        else:
            logging.info(f"Deleted {terminate_worker}")

# -------------------------------
# Remove hosts from Zabbix
# -------------------------------

# connect to Zabbix API
if common_config["enable_zabbix"]:
    try:
        # Connect to Zabbix API
        zapi = ZabbixAPI(server='http://localhost/zabbix')

        # Authenticate
        zapi.login("Admin", common_config["zabbix_conf"]["admin_password"])

        # Iterate over terminate_workers list
        for terminate_worker in terminate_workers:
            try:
                # Get list of hosts that matches the hostname
                hosts = zapi.host.get(output=["hostid", "name"], filter={"name": terminate_worker})
                if not hosts:
                    logging.warning(f"Can't remove host '{terminate_worker}' from Zabbix: Host doesn't exist.")
                else:
                    # Remove host from Zabbix
                    zapi.host.delete(hosts[0]["hostid"])
                    logging.info(f"Removed host '{terminate_worker}' from Zabbix.")
            except ZabbixAPIException as e:
                logging.error(f"Error while handling host '{terminate_worker}': {e}")
    except requests.exceptions.RequestException as e:
        logging.error(f"Cannot connect to Zabbix server: {e}")

logging.info(f"Successful delete_server.py execution ({sys.argv[1]})!")
time_in_s = time.time() - start_time
logging.info("--- %s minutes and %s seconds ---", math.floor(time_in_s / 60), time_in_s % 60)
logging.info("Exit Code 0")
sys.exit(0)

#!/usr/bin/env python3
"""
Creates one or more instances from comma separated name list.
Is called automatically by create.sh (called by slurm user automatically) which sources a virtual environment.
"""
import logging
import math
from openstack.exceptions import OpenStackCloudException
import re
import sys
import time
import subprocess

import ansible_runner
import os_client_config
import paramiko
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


def start_server(worker, connection, server_start_data):
    try:
        logging.info("Create server %s.", worker)
        # create server and ...
        server = connection.create_server(
            name=worker,
            flavor=worker_type["flavor"]["name"],
            image=worker_type["image"],
            network=worker_type["network"],
            key_name=f"tempKey_bibi-{common_config['cluster_id']}",
            wait=False)
        # ... add it to server
        server_start_data["started_servers"].append(server)
        try:
            connection.wait_for_server(server, auto_ip=False, timeout=600)
            server = connection.get_server(server["id"])
            print(server["private_v4"])
        except OpenStackCloudException as exc:
            logging.warning("While creating %s the OpenStackCloudException %s occurred.", worker, exc)
            server_start_data["openstack_wait_exceptions"].append(server.name)
            return
        logging.info("%s is active. Checking ssh", server.name)
        try:
            check_ssh_active(server.private_v4)
            logging.info(f"Server {server.name} is {server.status}.")
            server_start_data["available_servers"].append(server.name)
        except ConnectionError as exc:
            logging.warning(f"{exc}: Couldn't connect to {server.name}.")
            server_start_data["connection_exceptions"].append(server.name)
    except OpenStackCloudException as exc:
        logging.warning("While creating %s the OpenStackCloudException %s occurred. Worker ignored.",
                        worker, exc)
        server_start_data["other_openstack_exception"].append(worker)


def check_ssh_active(private_ip, private_key="/opt/slurm/.ssh/id_ecdsa", username="ubuntu", timeout=5):
    """
    Waits until SSH connects successful. This guarantees that the node can be reached via Ansible.
    @param private_ip: ip of node
    @param private_key: private ssh key
    @param username: username of node
    @param timeout: how long to try
    @return:
    """
    # Wait for SSH Connection available
    paramiko_key = paramiko.ECDSAKey.from_private_key_file(private_key)
    with paramiko.SSHClient() as client:
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        attempts = 0
        establishing_connection = True
        while establishing_connection:
            try:
                client.connect(hostname=private_ip, username=username, pkey=paramiko_key)
                establishing_connection = False
            except paramiko.ssh_exception.NoValidConnectionsError as exc:
                logging.info("Attempting to connect to %s... This might take a while", private_ip)
                if attempts < timeout:
                    time.sleep(2 ** attempts)
                    attempts += 1
                else:
                    logging.warning("Attempt to connect to %s failed.", private_ip)
                    raise ConnectionError from exc


def run_playbook(run_instances):
    """
    Runs the BiBiGrid playbook for run_instances
    @param run_instances: instances to run the playbook for
    @return:
    """
    logging.info("run_playbook with \ninstances: %s", run_instances)

    # cmdline_args = ["/opt/playbook/site.yml", '-i', '/opt/playbook/ansible_hosts', '-vvvv', '-l', instances]
    cmdline_args = ["/opt/playbook/site.yml", '-i', '/opt/playbook/ansible_hosts', '-l', ",".join(start_instances)]
    executable_cmd = '/usr/local/bin/ansible-playbook'
    logging.info(f"run_command...\nexecutable_cmd: {executable_cmd}\ncmdline_args: {cmdline_args}")

    runner = ansible_runner.interface.init_command_config(
        executable_cmd=executable_cmd,
        cmdline_args=cmdline_args)

    runner.run()
    runner_response = runner.stdout.read()
    runner_error = runner.stderr.read()
    return runner, runner_response, runner_error, runner.rc

server_start_data = {"started_servers": [], "other_openstack_exceptions": [], "connection_exceptions": [], "available_servers": [], "openstack_wait_exceptions": []}

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
                start_server(worker, connections[cloud_name], server_start_data)

# If no suitable server can be started: abort
if len(server_start_data["available_servers"]) == 0:
    logging.warning("No suitable server found! Abort!")
    exit(1)

# run ansible on started nodes
logging.info("Call Ansible to configure instances.")
runnable_instances = ",".join(server_start_data["available_servers"])
r, response, error, rc = run_playbook(runnable_instances)
logging.info("Ansible executed!")

# the rest of this code is only concerned with logging errors
unreachable_list = list(r.stats["dark"].keys())
failed_list = list(r.stats["failures"].keys())
ansible_execution_data = {"unreachable_list":unreachable_list, "failed_list": failed_list}
if failed_list or unreachable_list:
    logging.warning(ansible_execution_data)
    exit(1)
else:
    logging.info(ansible_execution_data)
server_start_data = {"started_servers": [], "other_openstack_exceptions": [], "connection_exceptions": [], "available_servers": [], "openstack_wait_exceptions": []}
if [key for key in server_start_data if "exception" in key]:
    logging.warning(server_start_data)
    exit(1)
else:
    logging.info(server_start_data)

logging.info("Successful create_server.py execution!")
time_in_s = time.time() - start_time
logging.info(f"--- %s minutes and %s seconds ---", math.floor(time_in_s / 60), time_in_s % 60)
logging.info("Exit Code 0")
exit(0)

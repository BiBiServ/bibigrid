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
logging.info("Instances: %s", start_instances)


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


# read instances configuration
with open("/opt/playbook/vars/instances.yml", mode="r") as f:
    instances = yaml.safe_load(f)["instances"]

# read common configuration
with open("/opt/playbook/vars/common_configuration.yml", mode="r") as f:
    common_config = yaml.safe_load(f)

# read clouds.yaml
with open("/etc/openstack/clouds.yaml", mode="r") as f:
    clouds = yaml.safe_load(f)["clouds"]

connections = {}
for cloud in clouds:
    connections[cloud] = os_client_config.make_sdk(cloud=cloud)

# Iterate over all names and search for a fitting ...
server_list = []
openstack_exception_list = []
no_ssh_list = []
return_list = []
openstack_wait_exception_list = []
# ... worker_type
for cloud_name,cloud in [(key,value) for key, value in instances.items() if key != 'master']:
    for worker_type in cloud["workers"]:
        for worker in start_instances:
            # check if worker is described in instances.yml
            result = subprocess.run(["scontrol", "show", "hostname", worker_type["name"]], stdout=subprocess.PIPE)
            possible_workers = result.stdout.decode("utf-8").strip().split("\n")
            if worker in possible_workers:
                try:
                    logging.info("Create server %s.", worker)
                    # create server and ...
                    server = connections[cloud_name].create_server(
                        name=worker,
                        flavor=worker_type["flavor"]["name"],
                        image=worker_type["image"],
                        network=worker_type["network"],
                        key_name=f"tempKey_bibi-{common_config['cluster_id']}",
                        wait=False)
                    # ... add it to server
                    server_list.append(server)
                    try:
                        connections[cloud_name].wait_for_server(server, auto_ip=False, timeout=600)
                        server = connections[cloud_name].get_server(server["id"])
                    except OpenStackCloudException as exc:
                        logging.warning("While creating %s the OpenStackCloudException %s occurred.", worker, exc)
                        openstack_wait_exception_list.append(server.name)
                        continue
                    logging.info("%s is active. Checking ssh", server.name)
                    try:
                        check_ssh_active(server.private_v4)
                        logging.info(f"Server {server.name} is {server.status}.")
                        return_list.append(server.name)
                    except ConnectionError as exc:
                        logging.warning(f"{exc}: Couldn't connect to {server.name}.")
                        no_ssh_list.append(server.name)
                except OpenStackCloudException as exc:
                    logging.warning("While creating %s the OpenStackCloudException %s occurred. Worker ignored.",
                                    worker, exc)
                    openstack_exception_list.append(worker)

# If no suitable server can be started: abort
if len(return_list) == 0:
    logging.warning("No suitable server found! Abort!")
    exit(1)

logging.info("Call Ansible to configure instances.")
# run ansible
# ToDo: use https://ansible-runner.readthedocs.io/en/latest/ instead of subprocess
runnable_instances = ",".join(return_list)

r, response, error, rc = run_playbook(runnable_instances)
logging.info("Ansible executed!")
unreachable_list = list(r.stats["dark"].keys())
failed_list = list(r.stats["failures"].keys())
overall_failed_list = unreachable_list + failed_list + no_ssh_list + openstack_wait_exception_list
if overall_failed_list or openstack_exception_list:
    logging.warning(f"Openstack exception list: {openstack_exception_list}")
    logging.warning(f"Unable to connect via ssh list: {no_ssh_list}")
    logging.warning(f"Unreachable list: {unreachable_list}")
    logging.warning(f"Failed list: {failed_list}")
    logging.warning(f"Return code: {rc}")
    for server_name in overall_failed_list:
        logging.warning(f"Deleting server {server_name}") # {sdk.delete_server(server_name)}
    logging.warning("Exit Code 1")
    exit(1)
logging.info("Successful create_server.py execution!")
time_in_s = time.time() - start_time
logging.info(f"--- %s minutes and %s seconds ---", math.floor(time_in_s / 60), time_in_s % 60)
logging.info("Exit Code 0")
exit(0)

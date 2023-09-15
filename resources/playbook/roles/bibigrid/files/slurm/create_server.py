#!/usr/bin/env python3
"""
Creates one or more instances from comma separated name list.
Is called automatically by create.sh (called by slurm user automatically) which sources a virtual environment.
"""
import difflib
import logging
import math
import os
import re
import subprocess
import sys
import threading
import time

import ansible_runner
import os_client_config
import paramiko
import yaml
from openstack.exceptions import OpenStackCloudException


class ImageNotFoundException(Exception):
    """ Image not found exception"""


LOGGER_FORMAT = "%(asctime)s [%(levelname)s] %(message)s"
logging.basicConfig(format=LOGGER_FORMAT, filename="/var/log/slurm/create_server.log", level=logging.INFO)
HOSTS_FILE_PATH = "/opt/playbook/vars/hosts.yml"

logging.info("create_server.py started")
start_time = time.time()

if len(sys.argv) < 2:
    logging.warning("usage:  $0 instance1_name[,instance2_name,...]")
    logging.info("Your input %s with length %s", sys.argv, len(sys.argv))
    sys.exit(1)
start_workers = sys.argv[1].split("\n")
logging.info("Starting instances %s", start_workers)


def select_image(start_worker_group, connection):
    image = start_worker_group["image"]
    # check if image is active
    active_images = [img["name"] for img in connection.image.images() if img["status"].lower() == "active"]
    if image not in active_images:
        old_image = image
        logging.info(f"Image '{old_image}' has no direct match. Maybe it's a regex? Trying regex match.")
        image = next((elem for elem in active_images if re.match(image, elem)), None)
        if not image:
            logging.warning(f"Couldn't find image '{old_image}'.")
            if isinstance(start_worker_group.get("fallbackOnOtherImage"), str):
                image = next(
                    elem for elem in active_images if re.match(start_worker_group["fallbackOnOtherImage"], elem))
                logging.info(f"Taking first regex ('{start_worker_group['fallbackOnOtherImage']}') match '{image}'.")
            elif start_worker_group.get("fallbackOnOtherImage", False):
                image = difflib.get_close_matches(old_image, active_images)[0]
                logging.info(f"Taking closest active image (in name) '{image}' instead.")
            else:
                raise ImageNotFoundException(f"Image {old_image} no longer active or doesn't exist.")
            logging.info(f"Using alternative '{image}' instead of '{old_image}'. You should change the configuration.")
        else:
            logging.info(f"Taking first regex match: '{image}'")
    return image


def start_server(worker, start_worker_group, start_data):
    try:
        logging.info("Create server %s.", worker)
        connection = connections[start_worker_group["cloud_identifier"]]
        # check for userdata
        userdata = ""
        userdata_file_path = f"/opt/slurm/userdata_{start_worker_group['cloud_identifier']}.txt"
        if os.path.isfile(userdata_file_path):
            with open(userdata_file_path, mode="r", encoding="utf-8") as userdata_file:
                userdata = userdata_file.read()
        # create server and ...
        image = select_image(start_worker_group, connection)
        server = connection.create_server(name=worker, flavor=start_worker_group["flavor"]["name"], image=image,
                                          network=start_worker_group["network"],
                                          key_name=f"tempKey_bibi-{common_config['cluster_id']}",
                                          security_groups=[f"default-{common_config['cluster_id']}"], userdata=userdata,
                                          wait=False)
        # ... add it to server
        start_data["started_servers"].append(server)
        try:
            connection.wait_for_server(server, auto_ip=False, timeout=600)
            server = connection.get_server(server["id"])
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
        logging.info("Update hosts.yml")
        update_hosts(server.name, server.private_v4)

    except OpenStackCloudException as exc:
        logging.warning("While creating %s the OpenStackCloudException %s occurred. Worker ignored.", worker, exc)
        server_start_data["other_openstack_exception"].append(worker)


def check_ssh_active(private_ip, private_key="/opt/slurm/.ssh/id_ecdsa", username="ubuntu", timeout=7):
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


def update_hosts(name, ip):  # pylint: disable=invalid-name
    """
    Update hosts.yml
    @param name: hostname
    @param ip: ibibigrid-worker0-3k1eeysgetmg4vb-3p address
    @return:
    """
    hosts = {"host_entries": {}}
    if os.path.isfile(HOSTS_FILE_PATH):
        with open(HOSTS_FILE_PATH, mode="r", encoding="utf-8") as hosts_file:
            hosts = yaml.safe_load(hosts_file)
            hosts_file.close()
        if hosts is None or "host_entries" not in hosts.keys():
            hosts = {"host_entries": {}}

    hosts["host_entries"][name] = ip

    with open(HOSTS_FILE_PATH, mode="w", encoding="utf-8") as hosts_file:
        yaml.dump(hosts, hosts_file)
        hosts_file.close()


def configure_dns():
    """
    Reconfigure DNS (dnsmasq)
    @return:
    """
    logging.info("configure_dns")
    cmdline_args = ["/opt/playbook/site.yml", '-i', '/opt/playbook/ansible_hosts', '-l', "master", "-t", "dns"]
    return _run_playbook(cmdline_args)


def configure_worker(instances):
    """
    Cnfigure worker nodes.
    @param instances: worker node to be configured
    @return:
    """
    logging.info("configure_worker \nworker: %s", instances)
    cmdline_args = ["/opt/playbook/site.yml", '-i', '/opt/playbook/ansible_hosts', '-l', instances]
    return _run_playbook(cmdline_args)


def _run_playbook(cmdline_args):
    """
    Running BiBiGrid playbook with given cmdline arguments.
    @param cmdline_args:
    @return
    """
    executable_cmd = '/usr/local/bin/ansible-playbook'
    logging.info(f"run_command...\nexecutable_cmd: {executable_cmd}\ncmdline_args: {cmdline_args}")
    runner = ansible_runner.interface.init_command_config(executable_cmd=executable_cmd, cmdline_args=cmdline_args)

    runner.run()
    runner_response = runner.stdout.read()
    runner_error = runner.stderr.read()
    return runner, runner_response, runner_error, runner.rc


server_start_data = {"started_servers": [], "other_openstack_exceptions": [], "connection_exceptions": [],
                     "available_servers": [], "openstack_wait_exceptions": []}

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

start_server_threads = []
for worker_group in worker_groups:
    for start_worker in start_workers:
        # start all servers that are part of the current worker group
        result = subprocess.run(["scontrol", "show", "hostname", worker_group["name"]], stdout=subprocess.PIPE,
                                check=True)  # get all workers in worker_type
        possible_workers = result.stdout.decode("utf-8").strip().split("\n")
        if start_worker in possible_workers:
            start_worker_thread = threading.Thread(target=start_server,
                                                   kwargs={"worker": start_worker, "start_worker_group": worker_group,
                                                           "start_data": server_start_data})
            start_worker_thread.start()
            start_server_threads.append(start_worker_thread)

for start_server_thread in start_server_threads:
    start_server_thread.join()

# If no suitable server can be started: abort
if len(server_start_data["available_servers"]) == 0:
    logging.warning("Couldn't make server available! Abort!")
    sys.exit(1)

# run ansible on master node to configure dns
logging.info("Call Ansible to configure dns.")
r, response, error, rc = configure_dns()
logging.info("DNS was configure by Ansible!")

# run ansible on started worker nodes
logging.info("Call Ansible to configure worker.")
RUNNABLE_INSTANCES = ",".join(server_start_data["available_servers"])
r, response, error, rc = configure_worker(RUNNABLE_INSTANCES)
logging.info("Worker were configured by Ansible!")

# the rest of this code is only concerned with logging errors
unreachable_list = list(r.stats["dark"].keys())
failed_list = list(r.stats["failures"].keys())
ansible_execution_data = {"unreachable_list": unreachable_list, "failed_list": failed_list}
if failed_list or unreachable_list:
    logging.warning(ansible_execution_data)
    sys.exit(1)
else:
    logging.info(ansible_execution_data)
server_start_data = {"started_servers": [], "other_openstack_exceptions": [], "connection_exceptions": [],
                     "available_servers": [], "openstack_wait_exceptions": []}
if [key for key in server_start_data if "exception" in key]:
    logging.warning(server_start_data)
    sys.exit(1)
else:
    logging.info(server_start_data)

logging.info("Successful create_server.py execution!")
time_in_s = time.time() - start_time
logging.info("--- %s minutes and %s seconds ---", math.floor(time_in_s / 60), time_in_s % 60)
logging.info("Exit Code 0")
sys.exit(0)

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
from filelock import FileLock

import ansible_runner
import os_client_config
import paramiko
import yaml
from openstack.exceptions import OpenStackCloudException


class ImageNotFoundException(Exception):
    """ Image not found exception"""


class ConfigurationException(Exception):
    """ Configuration exception """


LOGGER_FORMAT = "%(asctime)s [%(levelname)s] %(message)s"
logging.basicConfig(format=LOGGER_FORMAT, filename="/var/log/slurm/create_server.log", level=logging.INFO)
# For Debugging
# console_handler = logging.StreamHandler(sys.stdout)
# console_handler.setFormatter(logging.Formatter(LOGGER_FORMAT))
# logging.basicConfig(level=logging.INFO, handlers=[console_handler])

HOSTS_FILE_PATH = "/opt/playbook/vars/hosts.yaml"

logging.info("create_server.py started")
start_time = time.time()

if len(sys.argv) < 2:
    logging.warning("Not enough arguments!")
    logging.info("Your input %s with length %s", sys.argv, len(sys.argv))
    sys.exit(1)
start_workers = sys.argv[1].split("\n")
logging.info("Starting instances %s", start_workers)

server_start_data = {"started_servers": [], "other_openstack_exceptions": [], "connection_exceptions": [],
                     "available_servers": [], "openstack_wait_exceptions": []}

GROUP_VARS_PATH = "/opt/playbook/group_vars"
worker_groups = []
for filename in os.listdir(GROUP_VARS_PATH):
    if filename != "master.yaml":
        worker_group_yaml_file = os.path.join(GROUP_VARS_PATH, filename)
        # checking if it is a file
        if os.path.isfile(worker_group_yaml_file):
            with open(worker_group_yaml_file, mode="r", encoding="UTF-8") as worker_group_yaml:
                worker_groups.append(yaml.safe_load(worker_group_yaml))

# read common configuration
with open("/opt/playbook/vars/common_configuration.yaml", mode="r", encoding="UTF-8") as common_configuration_file:
    common_config = yaml.safe_load(common_configuration_file)
logging.info(f"Maximum 'is active' attempts: {common_config['cloud_scheduling']['sshTimeout']}")
# read clouds.yaml
with open("/etc/openstack/clouds.yaml", mode="r", encoding="UTF-8") as clouds_file:
    clouds = yaml.safe_load(clouds_file)["clouds"]

connections = {}  # connections to cloud providers
for cloud in clouds:
    connections[cloud] = os_client_config.make_sdk(cloud=cloud, volume_api_version="3")


# pylint: disable=duplicate-code
def create_volume_from_snapshot(connection, snapshot_name_or_id, volume_name_or_id=None):
    """
    Uses the cinder API to create a volume from snapshot:
    https://github.com/openstack/python-cinderclient/blob/master/cinderclient/v3/volumes.py
    @param connection:
    @param snapshot_name_or_id: name or id of snapshot
    @param volume_name_or_id:
    @return: id of created volume
    """
    logging.debug("Trying to create volume from snapshot")
    snapshot = connection.get_volume_snapshot(snapshot_name_or_id)
    if snapshot:
        logging.debug(f"Snapshot {snapshot_name_or_id} found.")
        if snapshot["status"] == "available":
            logging.debug("Snapshot %s is available.", {snapshot_name_or_id})
            size = snapshot["size"]
            name = volume_name_or_id or f"bibigrid-{snapshot['name']}"
            description = f"Created from snapshot {snapshot_name_or_id} by BiBiGrid"
            volume = connection.create_volume(name=name, size=size, description=description)
            return volume.toDict()
        logging.warning("Snapshot %s is %s; must be available.", snapshot_name_or_id, snapshot['status'])
    else:
        logging.warning("Snapshot %s not found.", snapshot_name_or_id)
    return None


def get_server_vars(name):
    # loading server_vars
    host_vars_path = f"/opt/playbook/host_vars/{name}.yaml"
    server_vars = {"volumes": []}
    if os.path.isfile(host_vars_path):
        logging.info(f"Found host_vars file {host_vars_path}.")
        with open(host_vars_path, mode="r", encoding="UTF-8") as host_vars_file:
            server_vars = yaml.safe_load(host_vars_file)
            logging.info(f"Loaded Vars: {server_vars}")
    else:
        logging.info(f"No host vars exist (group vars still apply). Using {server_vars}.")
    return server_vars


# pylint: disable=duplicate-code
def create_server_volumes(connection, host_vars, name):
    logging.info("Creating volumes ...")
    volumes = host_vars.get('volumes', [])
    return_volumes = []

    logging.info(f"Instance Volumes {volumes}")
    for volume in volumes:
        logging.debug(f"Trying to find volume {volume['name']}")
        return_volume = connection.get_volume(volume['name'])
        if not return_volume:
            logging.debug(f"Volume {volume['name']} not found.")

            if volume.get('snapshot'):
                logging.debug("Creating volume from snapshot...")
                return_volume = create_volume_from_snapshot(connection, volume['snapshot'], volume['name'])
                if not return_volume:
                    raise ConfigurationException(f"Snapshot {volume['snapshot']} not found!")
            else:
                logging.debug("Creating volume...")
                return_volume = connection.create_volume(name=volume['name'], size=volume.get("size", 50),
                                                         volume_type=volume.get("type"),
                                                         description=f"Created for {name}")
        return_volumes.append(return_volume)
    return return_volumes


def volumes_host_vars_update(connection, server, host_vars):
    logging.info("Updating host vars volume info")
    host_vars_path = f"/opt/playbook/host_vars/{server['name']}.yaml"

    with FileLock(f"{host_vars_path}.lock"):
        logging.info(f"{host_vars_path}.lock acquired")
        # get name and device info
        server_attachment = []
        for server_volume in server["volumes"]:
            volume = connection.get_volume(server_volume["id"])
            for attachment in volume["attachments"]:
                if attachment["server_id"] == server["id"]:
                    server_attachment.append({"name": volume["name"], "device": attachment["device"]})
                    break
        # add device info
        volumes = host_vars.get("volumes", [])
        if volumes:
            for volume in volumes:
                logging.info(f"Finding device for {volume['name']}.")
                server_volume = next((server_volume for server_volume in server_attachment if
                                      server_volume["name"] == volume["name"]), None)
                if not server_volume:
                    raise RuntimeError(
                        f"Created server {server['name']} doesn't have attached volume {volume['name']}.")
                volume["device"] = server_volume.get("device")

                logging.debug(f"Added Configuration: Instance {server['name']} has volume {volume['name']} "
                              f"as device {volume['device']} that is going to be mounted to "
                              f"{volume.get('mountPoint')}")
        with open(host_vars_path, mode="w+", encoding="UTF-8") as host_vars_file:
            yaml.dump(host_vars, host_vars_file)
    logging.info(f"{host_vars_path}.lock released")


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
            if isinstance(start_worker_group.get("fallback_on_other_image"), str):
                image = next(
                    elem for elem in active_images if re.match(start_worker_group["fallback_on_other_image"], elem))
                logging.info(f"Taking first regex ('{start_worker_group['fallback_on_other_image']}') match '{image}'.")
            elif start_worker_group.get("fallback_on_other_image", False):
                image = difflib.get_close_matches(old_image, active_images)[0]
                logging.info(f"Taking closest active image (in name) '{image}' instead.")
            else:
                raise ImageNotFoundException(f"Image {old_image} no longer active or doesn't exist.")
            logging.info(f"Using alternative '{image}' instead of '{old_image}'. You should change the configuration.")
        else:
            logging.info(f"Taking first regex match: '{image}'")
    return image


def start_server(name, start_worker_group, start_data):
    try:
        logging.info("Create server %s.", name)
        connection = connections[start_worker_group["cloud_identifier"]]
        # check if running
        already_running_server = connection.get_server(name)
        if already_running_server:
            logging.warning(
                f"Already running server {name} on {start_worker_group['cloud_identifier']} (will be terminated): "
                f"{already_running_server['name']}")
            server_deleted = connection.delete_server(name)
            logging.info(
                f"Server {name} on {start_worker_group['cloud_identifier']} has been terminated ({server_deleted}). "
                f"Continuing startup.")
        # check for userdata
        userdata = ""
        userdata_file_path = f"/opt/slurm/userdata_{start_worker_group['cloud_identifier']}.txt"
        if os.path.isfile(userdata_file_path):
            with open(userdata_file_path, mode="r", encoding="UTF-8") as userdata_file:
                userdata = userdata_file.read()
        # create server and ...
        image = select_image(start_worker_group, connection)
        host_vars = get_server_vars(name)
        volumes = create_server_volumes(connection, host_vars, name)
        boot_volume = start_worker_group.get("bootVolume", {})
        server = connection.create_server(name=name, flavor=start_worker_group["flavor"]["name"], image=image,
                                          network=start_worker_group["network"],
                                          key_name=f"tempKey_bibi-{common_config['cluster_id']}",
                                          security_groups=[f"default-{common_config['cluster_id']}"], userdata=userdata,
                                          volumes=volumes, wait=False,
                                          boot_from_volume=boot_volume.get("name", False),
                                          boot_volume=bool(boot_volume),
                                          terminate_volume=boot_volume.get("terminate", True),
                                          volume_size=boot_volume.get("size", 50)
                                          )
        # ... add it to server
        start_data["started_servers"].append(server)
        try:
            connection.wait_for_server(server, auto_ip=False, timeout=600)
            server = connection.get_server(server["id"])
        except OpenStackCloudException as exc:
            logging.warning("While creating %s the OpenStackCloudException %s occurred.", name, exc)
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
        logging.info("Update hosts.yaml")
        volumes_host_vars_update(connection, server, host_vars)
        update_hosts(server.name, server.private_v4)

    except OpenStackCloudException as exc:
        logging.warning("While creating %s the OpenStackCloudException %s occurred. Worker ignored.", name, exc)
        server_start_data["other_openstack_exceptions"].append(name)


def check_ssh_active(private_ip, private_key="/opt/slurm/.ssh/id_ecdsa", username="ubuntu"):
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
                if attempts < common_config["cloud_scheduling"]["sshTimeout"]:
                    time.sleep(2 ** (2 + attempts))
                    attempts += 1
                else:
                    logging.warning("Attempt to connect to %s failed.", private_ip)
                    raise ConnectionError from exc


def update_hosts(name, ip):  # pylint: disable=invalid-name
    """
    Update hosts.yaml
    @param name: hostname
    @param ip: ibibigrid-worker0-3k1eeysgetmg4vb-3p address
    @return:
    """
    logging.info("Updating hosts.yaml")
    with FileLock("hosts.yaml.lock"):
        logging.info("Lock acquired")
        with open(HOSTS_FILE_PATH, mode="r", encoding="UTF-8") as hosts_file:
            hosts = yaml.safe_load(hosts_file)
        logging.info(f"Existing hosts {hosts}")
        if not hosts or "host_entries" not in hosts:
            logging.info(f"Resetting host entries because {'first run' if hosts else 'broken'}.")
            hosts = {"host_entries": {}}
        hosts["host_entries"][name] = ip
        logging.info(f"Added host {name} with ip {hosts['host_entries'][name]}")
        with open(HOSTS_FILE_PATH, mode="w", encoding="UTF-8") as hosts_file:
            yaml.dump(hosts, hosts_file)
    logging.info("Wrote hosts file. Released hosts.yaml.lock.")


def configure_dns():
    """
    Reconfigure DNS (dnsmasq)
    @return:
    """
    logging.info("configure_dns")
    cmdline_args = ["/opt/playbook/site.yaml", '-i', '/opt/playbook/ansible_hosts', '-l', "master", "-t", "dns"]
    return _run_playbook(cmdline_args)


def configure_worker(instances):
    """
    Cnfigure worker nodes.
    @param instances: worker node to be configured
    @return:
    """
    logging.info("configure_worker \nworker: %s", instances)
    cmdline_args = ["/opt/playbook/site.yaml", '-i', '/opt/playbook/ansible_hosts', '-l', instances]
    return _run_playbook(cmdline_args)


def _run_playbook(cmdline_args):
    """
    Running BiBiGrid playbook with given cmdline arguments.
    @param cmdline_args:
    @return
    """
    executable_cmd = '/opt/bibigrid-venv/bin/ansible-playbook'
    logging.info(f"run_command...\nexecutable_cmd: {executable_cmd}\ncmdline_args: {cmdline_args}")
    runner = ansible_runner.interface.init_command_config(executable_cmd=executable_cmd, cmdline_args=cmdline_args)

    runner.run()
    runner_response = runner.stdout.read()
    runner_error = runner.stderr.read()
    return runner, runner_response, runner_error, runner.rc


start_server_threads = []
for worker_group in worker_groups:
    for worker_name in start_workers:
        # start all servers that are part of the current worker group
        result = subprocess.run(["scontrol", "show", "hostname", worker_group["name"]], stdout=subprocess.PIPE,
                                check=True)  # get all workers in worker_type
        possible_workers = result.stdout.decode("utf-8").strip().split("\n")
        if worker_name in possible_workers:
            start_worker_thread = threading.Thread(target=start_server,
                                                   kwargs={"name": worker_name, "start_worker_group": worker_group,
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
logging.info(f"This is error {error}")
logging.info(f"This is response {response}")
if error:
    logging.error(response)
else:
    logging.info("DNS was configure by Ansible!")

# run ansible on started worker nodes
logging.info("Call Ansible to configure worker.")
RUNNABLE_INSTANCES = ",".join(server_start_data["available_servers"])
r, response, error, rc = configure_worker(RUNNABLE_INSTANCES)
if error:
    logging.error(response)
else:
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

logging.info("Successful create_server.py execution!")
time_in_s = time.time() - start_time
logging.info("--- %s minutes and %s seconds ---", math.floor(time_in_s / 60), time_in_s % 60)
logging.info("Exit Code 0")
sys.exit(0)

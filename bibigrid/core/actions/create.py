"""
The cluster creation (master's creation, key creation, ansible setup and execution, ...) is done here
"""

import os
import shutil
import subprocess
import threading
import traceback
from functools import partial

import paramiko
import sympy
import yaml

from bibigrid.core.actions import terminate
from bibigrid.core.utility import ansible_configurator
from bibigrid.core.utility import id_generation
from bibigrid.core.utility import image_selection
from bibigrid.core.utility.handler import ssh_handler
from bibigrid.core.utility.paths import ansible_resources_path as a_rp
from bibigrid.core.utility.paths import bin_path
from bibigrid.models import exceptions
from bibigrid.models import return_threading
from bibigrid.models.exceptions import ExecutionException, ConfigurationException

PREFIX = "bibigrid"
SEPARATOR = "-"
PREFIX_WITH_SEP = PREFIX + SEPARATOR
FILEPATHS = [(a_rp.PLAYBOOK_PATH, a_rp.PLAYBOOK_PATH_REMOTE), (bin_path.BIN_PATH, bin_path.BIN_PATH_REMOTE)]


def get_identifier(identifier, cluster_id, additional=""):
    """
    This method does more advanced string formatting to generate master, vpngtw and worker names
    @param identifier: master|vpngtw|worker
    @param cluster_id: id of cluster
    @param additional: an additional string to be added at the end
    @return: the generated string
    """
    general = PREFIX_WITH_SEP + identifier + SEPARATOR + cluster_id
    if additional or additional == 0:
        return general + SEPARATOR + str(additional)
    return general


MASTER_IDENTIFIER = partial(get_identifier, identifier="master", additional="")
WORKER_IDENTIFIER = partial(get_identifier, identifier="worker")
VPN_WORKER_IDENTIFIER = partial(get_identifier, identifier="vpngtw")

KEY_PREFIX = "tempKey_bibi"
CONFIG_FOLDER = os.path.expanduser("~/.config/bibigrid/")
KEY_FOLDER = os.path.join(CONFIG_FOLDER, "keys/")
AC_NAME = "ac" + SEPARATOR + "{cluster_id}"
KEY_NAME = KEY_PREFIX + SEPARATOR + "{cluster_id}"
CLUSTER_MEMORY_FOLDER = KEY_FOLDER
CLUSTER_MEMORY_FILE = ".bibigrid.mem"
CLUSTER_MEMORY_PATH = os.path.join(CONFIG_FOLDER, CLUSTER_MEMORY_FILE)
DEFAULT_SECURITY_GROUP_NAME = "default" + SEPARATOR + "{cluster_id}"
WIREGUARD_SECURITY_GROUP_NAME = "wireguard" + SEPARATOR + "{cluster_id}"


class Create:  # pylint: disable=too-many-instance-attributes,too-many-arguments
    """
    The class Create holds necessary methods to execute the Create-Action
    """

    def __init__(self, *, providers, configurations, config_path, log, debug=False,
                 cluster_id=None):
        """
        Additionally sets (unique) cluster_id, public_key_commands (to copy public keys to master) and key_name.
        Call create() to actually start server.
        @param providers: List of providers (provider)
        @param configurations: List of configurations (dict)
        @param config_path: string that is the path to config-file
        @param debug: Bool. If True Cluster offer shut-down after create and
        will ask before shutting down on errors
        """
        self.log = log
        self.providers = providers
        self.configurations = configurations
        self.debug = debug
        self.cluster_id = cluster_id or id_generation.generate_safe_cluster_id(providers)
        self.ssh_user = configurations[0].get("sshUser") or "ubuntu"
        self.ssh_add_public_key_commands = ssh_handler.get_add_ssh_public_key_commands(
            configurations[0].get("sshPublicKeyFiles"))
        self.ssh_timeout = configurations[0].get("sshTimeout", 5)
        self.config_path = config_path
        self.master_ip = None
        self.log.debug("Cluster-ID: %s", self.cluster_id)
        self.name = AC_NAME.format(cluster_id=self.cluster_id)
        self.key_name = KEY_NAME.format(cluster_id=self.cluster_id)
        self.default_security_group_name = DEFAULT_SECURITY_GROUP_NAME.format(cluster_id=self.cluster_id)
        self.wireguard_security_group_name = WIREGUARD_SECURITY_GROUP_NAME.format(cluster_id=self.cluster_id)

        self.worker_counter = 0
        # permanents holds groups or single nodes that ansible playbook should be run for during startup
        self.permanents = ["vpn"]
        self.vpn_counter = 0
        self.vpn_counter_thread_lock = threading.Lock()
        self.worker_thread_lock = threading.Lock()
        self.use_master_with_public_ip = not configurations[0].get("gateway") and configurations[0].get(
            "useMasterWithPublicIp", True)
        self.log.debug("Keyname: %s", self.key_name)

    def create_defaults(self):
        self.log.debug("Creating default files")
        if not self.configurations[0].get("customAnsibleCfg", False) or not os.path.isfile(a_rp.ANSIBLE_CFG_PATH):
            self.log.debug("Copying ansible.cfg")
            shutil.copy(a_rp.ANSIBLE_CFG_DEFAULT_PATH, a_rp.ANSIBLE_CFG_PATH)
        if not self.configurations[0].get("customSlurmConf", False) or not os.path.isfile(
                a_rp.SLURM_CONF_TEMPLATE_PATH):
            self.log.debug("Copying slurm.conf")
            shutil.copy(a_rp.SLURM_CONF_TEMPLATE_DEFAULT_PATH, a_rp.SLURM_CONF_TEMPLATE_PATH)

    def generate_keypair(self):
        """
        Generates ECDSA Keypair using system-function ssh-keygen and uploads the generated public key to providers.
        generate_keypair makes use of the fact that files in tmp are automatically deleted
        ToDo find a more pythonic way to create an ECDSA keypair
        See here for why using python module ECDSA wasn't successful
        https://stackoverflow.com/questions/71194770/why-does-creating-ecdsa-keypairs-via-python-differ-from-ssh
        -keygen-t-ecdsa-and
        @return:
        """
        self.log.info("Generating keypair")
        # create KEY_FOLDER if it doesn't exist
        if not os.path.isdir(KEY_FOLDER):
            self.log.debug("%s not found. Creating folder.", KEY_FOLDER)
            os.mkdir(KEY_FOLDER)
        # generate keyfile
        res = subprocess.check_output(f'ssh-keygen -t ecdsa -f {KEY_FOLDER}{self.key_name} -P ""', shell=True).decode()
        self.log.debug(res)
        # read private keyfile
        with open(f"{os.path.join(KEY_FOLDER, self.key_name)}.pub", mode="r", encoding="UTF-8") as key_file:
            public_key = key_file.read()
        # upload keyfiles
        for provider in self.providers:
            provider.create_keypair(name=self.key_name, public_key=public_key)

        # write cluster_id to automatically read it on following calls if no cid is given
        with open(CLUSTER_MEMORY_PATH, mode="w+", encoding="UTF-8") as cluster_memory_file:
            yaml.safe_dump(data={"cluster_id": self.cluster_id, "ssh_user": self.ssh_user}, stream=cluster_memory_file)

    def delete_old_vars(self):
        """
        Deletes host_vars and group_vars
        @return:
        """
        for folder in [a_rp.GROUP_VARS_FOLDER, a_rp.HOST_VARS_FOLDER]:
            for file_name in os.listdir(folder):
                # construct full file path
                file = os.path.join(folder, file_name)
                if os.path.isfile(file):
                    self.log.debug('Deleting file: %s', file)
                    os.remove(file)

    def generate_security_groups(self):
        """
        Generate a security groups:
         - default with basic rules for the cluster
         - wireguard when more than one provider is used (= multi-cloud)
        """
        self.log.info("Generating Security Groups")
        for provider, configuration in zip(self.providers, self.configurations):
            # create a default security group
            default_security_group_id = provider.create_security_group(name=self.default_security_group_name)["id"]

            rules = [{"direction": "ingress", "ethertype": "IPv4", "protocol": None, "port_range_min": None,
                      "port_range_max": None, "remote_ip_prefix": None, "remote_group_id": default_security_group_id},
                     {"direction": "ingress", "ethertype": "IPv4", "protocol": "tcp", "port_range_min": 22,
                      "port_range_max": 22, "remote_ip_prefix": "0.0.0.0/0", "remote_group_id": None}]
            # when running a multi-cloud setup additional default rules are necessary
            if len(self.providers) > 1:
                # allow incoming traffic from wireguard network
                rules.append({"direction": "ingress", "ethertype": "IPv4", "protocol": "tcp", "port_range_min": None,
                              "port_range_max": None, "remote_ip_prefix": "10.0.0.0/24", "remote_group_id": None})
                # allow incoming traffic from all other local provider networks
                for tmp_configuration in self.configurations:
                    if tmp_configuration != configuration:
                        for cidr in tmp_configuration['subnet_cidrs']:
                            rules.append(
                                {"direction": "ingress", "ethertype": "IPv4", "protocol": "tcp", "port_range_min": None,
                                 "port_range_max": None, "remote_ip_prefix": cidr, "remote_group_id": None})
            provider.append_rules_to_security_group(default_security_group_id, rules)
            configuration["security_groups"] = [self.default_security_group_name]  # store in configuration
            # when running a multi-cloud setup create an additional wireguard group
            if len(self.providers) > 1:
                _ = provider.create_security_group(name=self.wireguard_security_group_name)["id"]
                configuration["security_groups"].append(self.wireguard_security_group_name)  # store in configuration

    def start_vpn_or_master(self, configuration, provider):  # pylint: disable=too-many-locals
        """
        Start master/vpn-worker of a provider
        @param configuration: dict configuration of said provider.
        @param provider: provider
        @return:
        """
        identifier, instance = self.prepare_vpn_or_master_args(configuration)
        external_network = provider.get_external_network(configuration["network"])
        if identifier == MASTER_IDENTIFIER:  # pylint: disable=comparison-with-callable
            name = identifier(cluster_id=self.cluster_id)
        else:
            name = identifier(cluster_id=self.cluster_id,  # pylint: disable=redundant-keyword-arg
                              additional=self.vpn_counter)  # pylint: disable=redundant-keyword-arg
            with self.vpn_counter_thread_lock:
                self.vpn_counter += 1
        self.log.info(f"Starting server {name} on {provider.cloud_specification['identifier']}")
        flavor = instance["type"]
        network = configuration["network"]
        image = image_selection.select_image(provider, instance["image"], self.log,
                                             configuration.get("fallbackOnOtherImage"))

        volumes = self.create_server_volumes(provider=provider, instance=instance, name=name)

        # create a server and block until it is up and running
        self.log.debug("Creating server...")
        boot_volume = instance.get("bootVolume", configuration.get("bootVolume", {}))
        server = provider.create_server(name=name, flavor=flavor, key_name=self.key_name, image=image, network=network,
                                        volumes=volumes, security_groups=configuration["security_groups"], wait=True,
                                        boot_from_volume=boot_volume.get("name", False),
                                        boot_volume=bool(boot_volume),
                                        terminate_boot_volume=boot_volume.get("terminate", True),
                                        volume_size=boot_volume.get("size", 50))
        # description=instance.get("description", configuration.get("description")))
        self.add_volume_device_info_to_instance(provider, server, instance)

        configuration["private_v4"] = server["private_v4"]
        self.log.debug(f"Created Server {name}: {server['private_v4']}.")
        # get mac address for given private address
        # Attention: The following source code works with Openstack and IPV4 only
        configuration["mac_addr"] = None
        for elem in server['addresses']:
            for network in server['addresses'][elem]:
                if network['addr'] == configuration["private_v4"]:
                    configuration["mac_addr"] = network['OS-EXT-IPS-MAC:mac_addr']
        if configuration["mac_addr"] is None:
            raise ConfigurationException(f"MAC address for ip {configuration['private_v4']} not found.")

        # pylint: disable=comparison-with-callable
        if identifier == VPN_WORKER_IDENTIFIER or (identifier == MASTER_IDENTIFIER and self.use_master_with_public_ip):
            configuration["floating_ip"] = \
                provider.attach_available_floating_ip(network=external_network, server=server)["floating_ip_address"]
            if identifier == MASTER_IDENTIFIER:
                with open(CLUSTER_MEMORY_PATH, mode="w+", encoding="UTF-8") as cluster_memory_file:
                    yaml.safe_dump(
                        data={"cluster_id": self.cluster_id, "floating_ip": configuration["floating_ip"]},
                        stream=cluster_memory_file)
            self.log.debug(f"Added floating ip {configuration['floating_ip']} to {name}.")
        elif identifier == MASTER_IDENTIFIER:
            configuration["floating_ip"] = server["private_v4"]  # pylint: enable=comparison-with-callable

    def start_worker(self, worker, worker_count, configuration, provider):  # pylint: disable=too-many-locals
        """
        Starts a single worker (with onDemand: False) and adds all relevant information to the configuration dictionary.
        Additionally, a hosts.yaml entry is created for the DNS resolution.
        @param worker:
        @param worker_count:
        @param configuration:
        @param provider:
        @return:
        """
        name = WORKER_IDENTIFIER(cluster_id=self.cluster_id, additional=worker_count)
        self.log.info(f"Starting server {name} on {provider.cloud_specification['identifier']}.")
        flavor = worker["type"]
        network = configuration["network"]
        image = image_selection.select_image(provider, worker["image"], self.log,
                                             configuration.get("fallbackOnOtherImage"))

        volumes = self.create_server_volumes(provider=provider, instance=worker, name=name)

        # create a server and attaches volumes if given; blocks until it is up and running
        boot_volume = worker.get("bootVolume", configuration.get("bootVolume", {}))
        server = provider.create_server(name=name, flavor=flavor, key_name=self.key_name, image=image, network=network,
                                        volumes=volumes, security_groups=configuration["security_groups"], wait=True,
                                        boot_from_volume=boot_volume.get("name", False),
                                        boot_volume=bool(boot_volume),
                                        terminate_boot_volume=boot_volume.get("terminateBoot", True),
                                        volume_size=boot_volume.get("size", 50),
                                        description=worker.get("description", configuration.get("description")))
        self.add_volume_device_info_to_instance(provider, server, worker)

        self.log.info(f"Worker {name} started on {provider.cloud_specification['identifier']}.")

        # for DNS resolution an entry in the hosts file is created
        with self.worker_thread_lock:
            self.permanents.append(name)
            with open(a_rp.HOSTS_FILE, mode="r", encoding="utf-8") as hosts_file:
                hosts = yaml.safe_load(hosts_file)
            if not hosts or "host_entries" not in hosts:
                self.log.warning("Hosts file is broken.")
                hosts = {"host_entries": {}}
            hosts["host_entries"][name] = server["private_v4"]
            ansible_configurator.write_yaml(a_rp.HOSTS_FILE, hosts, self.log)
            self.log.debug(f"Added worker {name} to hosts file {a_rp.HOSTS_FILE}.")

    # pylint: disable=duplicate-code
    def create_server_volumes(self, provider, instance, name):
        """
        Creates all volumes of a single instance
        @param provider:
        @param instance: flavor, image, ... description
        @param name: sever name
        @return:
        """
        self.log.info("Creating volumes ...")
        return_volumes = []

        group_instance = {"volumes": []}
        instance["group_instances"] = {name: group_instance}
        for i, volume in enumerate(instance.get("volumes", [])):
            if not volume.get("exists"):
                if volume.get("permanent"):
                    infix = "perm"
                elif volume.get("semiPermanent"):
                    infix = "semiperm"
                else:
                    infix = "tmp"
                postfix = f"-{volume.get('name')}" if volume.get('name') else ''
                volume_name = f"{name}-{infix}-{i}{postfix}"
            else:
                volume_name = volume["name"]
            group_instance["volumes"].append({**volume, "name": volume_name})

            self.log.debug(f"Trying to find volume {volume_name}")
            return_volume = provider.get_volume_by_id_or_name(volume_name)
            if not return_volume:
                self.log.debug(f"Volume {volume_name} not found.")
                if volume.get('snapshot'):
                    self.log.debug("Creating volume from snapshot...")
                    return_volume = provider.create_volume_from_snapshot(volume['snapshot'], volume_name)
                    if not return_volume:
                        raise ConfigurationException(f"Snapshot {volume['snapshot']} not found!")
                else:
                    self.log.debug("Creating volume...")
                    return_volume = provider.create_volume(name=volume_name, size=volume.get("size", 50),
                                                           volume_type=volume.get("type"),
                                                           description=f"Created for {name}")
            return_volumes.append(return_volume)
        return return_volumes

    def add_volume_device_info_to_instance(self, provider, server, instance):
        """
        Only after attaching the volume to the server it is decided where the device is attached.
        This method reads that value and stores it in the instance configuration.
        This method assumes that devices are attached the same on instances with identical images.
        @param provider:
        @param server:
        @param instance:
        @return:
        """
        self.log.info("Adding device info")
        server_volumes = provider.get_mount_info_from_server(server)  # list of volumes attachments
        group_instance_volumes = instance["group_instances"][server["name"]].get("volumes")
        final_volumes = []
        if group_instance_volumes:
            for volume in group_instance_volumes:
                server_volume = next((server_volume for server_volume in server_volumes if
                                      server_volume["name"] == volume["name"]), None)
                if not server_volume:
                    raise RuntimeError(
                        f"Created server {server['name']} doesn't have attached volume {volume['name']}.")
                device = server_volume.get("device")
                final_volumes.append({**volume, "device": device})

                self.log.debug(f"Added Configuration: Instance {server['name']} has volume {volume['name']} "
                               f"as device {device} that is going to be mounted to "
                               f"{volume.get('mountPoint')}")

            ansible_configurator.write_yaml(os.path.join(a_rp.HOST_VARS_FOLDER, f"{server['name']}.yaml"),
                                            {"volumes": final_volumes},
                                            self.log)

    def prepare_vpn_or_master_args(self, configuration):
        """
        Prepares start_instance arguments for master/vpn
        @param configuration: configuration (dict) of said master/vpn
        @return: arguments needed by start_instance
        """
        if configuration.get("masterInstance"):
            instance_type = configuration["masterInstance"]
            identifier = MASTER_IDENTIFIER
        elif configuration.get("vpnInstance"):
            instance_type = configuration["vpnInstance"]
            identifier = VPN_WORKER_IDENTIFIER
        else:
            self.log.warning(
                f"Configuration {configuration['cloud_identifier']} "
                f"has no vpngtw or master and is therefore unreachable.")
            raise ConfigurationException(
                f"Configuration {configuration['cloud_identifier']} has neither vpngtw nor masterInstance")
        return identifier, instance_type

    def initialize_instances(self):
        """
        Setup all servers
        """
        for configuration in self.configurations:
            ssh_data = {"floating_ip": configuration["floating_ip"], "private_key": KEY_FOLDER + self.key_name,
                        "username": self.ssh_user, "commands": None, "filepaths": None,
                        "gateway": configuration.get("gateway", {}), "timeout": self.ssh_timeout}
            if configuration.get("masterInstance"):
                self.master_ip = configuration["floating_ip"]
                wait_for_service_command, wait_for_service_message = ssh_handler.a_c.WAIT_FOR_SERVICES
                wait_for_services_commands = [
                    (wait_for_service_command.format(service=service), wait_for_service_message.format(service=service))
                    for service in configuration.get("waitForServices", [])]
                ssh_data["commands"] = (
                        wait_for_services_commands + self.ssh_add_public_key_commands + ssh_handler.ANSIBLE_SETUP)
                ssh_data["filepaths"] = [(ssh_data["private_key"], ssh_handler.PRIVATE_KEY_FILE)]
                ssh_handler.execute_ssh(ssh_data, self.log)
            elif configuration.get("vpnInstance"):
                ssh_data["commands"] = ssh_handler.VPN_SETUP
                ssh_handler.execute_ssh(ssh_data, self.log)

    def prepare_configurations(self):
        """
        Makes sure that subnet and network key are set for each configuration.
        If none is set a keyError will be raised and caught in create.
        @return:
        """
        for configuration, provider in zip(self.configurations, self.providers):
            if not configuration.get("network"):
                self.log.debug("No network found. Getting network by subnet.")
                configuration["network"] = provider.get_network_id_by_subnet(configuration["subnet"])
                if not configuration.get("network"):
                    self.log.warning("Unable to set network. "
                                     f"Subnet doesn't exist in cloud {configuration['cloud_identifier']}")
                    raise ConfigurationException(f"Subnet doesn't exist in cloud {configuration['cloud_identifier']}")
            self.log.debug("Getting subnets by network.")
            configuration["subnet"] = provider.get_subnet_ids_by_network(configuration["network"])
            if not configuration["subnet"]:
                self.log.warning("Unable to set subnet. Network doesn't exist or has no subnets.")
                raise ConfigurationException("Network doesn't exist.")
            configuration["subnet_cidrs"] = [provider.get_subnet_by_id_or_name(subnet)["cidr"] for subnet in
                                             configuration["subnet"]]
            configuration["sshUser"] = self.ssh_user  # is used in ansibleConfigurator

    def upload_data(self, private_key, clean_playbook=False):
        """
        Configures ansible and then uploads the modified files and all necessary data to the master
        @return:
        """
        self.log.debug("Running upload_data")
        if not os.path.isfile(a_rp.HOSTS_FILE):
            with open(a_rp.HOSTS_FILE, 'a', encoding='utf-8') as hosts_file:
                hosts_file.write("# placeholder file for worker DNS entries (see 003-dns)")

        ansible_configurator.configure_ansible_yaml(providers=self.providers, configurations=self.configurations,
                                                    cluster_id=self.cluster_id, log=self.log)
        ansible_start = ssh_handler.ANSIBLE_START
        ansible_start[-1] = (ansible_start[-1][0].format(",".join(self.permanents)), ansible_start[-1][1])
        self.log.debug(f"Starting playbook with {ansible_start}.")
        if self.configurations[0].get("dontUploadCredentials"):
            commands = ansible_start
        else:
            commands = [ssh_handler.get_ac_command(self.providers, AC_NAME.format(
                cluster_id=self.cluster_id))] + ssh_handler.ANSIBLE_START
        if clean_playbook:
            self.log.info("Cleaning Playbook")
            ssh_data = {"floating_ip": self.master_ip, "private_key": private_key, "username": self.ssh_user,
                        "commands": [("rm -rf ~/playbook/*", "Remove Playbook")], "filepaths": [],
                        "gateway": self.configurations[0].get("gateway", {}), "timeout": self.ssh_timeout}
            ssh_handler.execute_ssh(ssh_data=ssh_data, log=self.log)
        self.log.info("Uploading Data")
        ssh_data = {"floating_ip": self.master_ip, "private_key": private_key, "username": self.ssh_user,
                    "commands": commands, "filepaths": FILEPATHS, "gateway": self.configurations[0].get("gateway", {}),
                    "timeout": self.ssh_timeout}
        ssh_handler.execute_ssh(ssh_data=ssh_data, log=self.log)

    def start_start_server_threads(self):
        """
        Starts for each provider a start_instances thread and joins them.
        @return:
        """
        self.log.debug("Running start_start_server_threads")
        start_server_threads = []
        worker_count = 0
        ansible_configurator.write_yaml(a_rp.HOSTS_FILE, {"host_entries": {}}, self.log)
        for configuration, provider in zip(self.configurations, self.providers):
            start_server_thread = return_threading.ReturnThread(target=self.start_vpn_or_master,
                                                                args=[configuration, provider])
            start_server_thread.start()
            start_server_threads.append(start_server_thread)
            for worker in configuration.get("workerInstances", []):
                if not worker.get("onDemand", True):
                    for _ in range(int(worker.get("count", 1))):
                        start_server_thread = return_threading.ReturnThread(target=self.start_worker,
                                                                            args=[worker, worker_count, configuration,
                                                                                  provider])
                        start_server_thread.start()
                        start_server_threads.append(start_server_thread)
                        worker_count += 1
                else:
                    worker_count += worker.get("count", 1)
        for start_server_thread in start_server_threads:
            start_server_thread.join()

    def extended_network_configuration(self):
        """
            Configure master/vpn-worker network for a multi/hybrid cloud
        @return:
        """
        self.log.debug("Running extended_network_configuration")
        if len(self.providers) == 1:
            return

        for provider_a, configuration_a in zip(self.providers, self.configurations):
            # configure wireguard network as allowed network
            allowed_addresses = [{'ip_address': '10.0.0.0/24', 'mac_address': configuration_a["mac_addr"]}]
            # iterate over all configurations ...
            for configuration_b in self.configurations:
                # ... and pick all other configuration
                if configuration_a != configuration_b:
                    self.log.info(
                        f"{configuration_a['private_v4']} --> allowed_address_pair({configuration_a['mac_addr']},"
                        f"{configuration_b['subnet_cidrs']})")
                    # add provider_b network as allowed network
                    for cidr in configuration_b["subnet_cidrs"]:
                        allowed_addresses.append({'ip_address': cidr, 'mac_address': configuration_a["mac_addr"]})
                    # configure security group rules
                    provider_a.append_rules_to_security_group(self.wireguard_security_group_name, [
                        {"direction": "ingress", "ethertype": "IPv4", "protocol": "udp", "port_range_min": 51820,
                         "port_range_max": 51820, "remote_ip_prefix": configuration_b["floating_ip"],
                         "remote_group_id": None}])
            # configure allowed addresses for provider_a/configuration_a
            provider_a.set_allowed_addresses(configuration_a['private_v4'], allowed_addresses)

    def create(self):  # pylint: disable=too-many-branches,too-many-statements
        """
        Creates cluster and logs helpful cluster-info afterwards.
        If debug is set True it offers termination after starting the cluster.
        @return: exit_state
        """
        try:
            for folder in [a_rp.VARS_FOLDER, a_rp.GROUP_VARS_FOLDER, a_rp.HOST_VARS_FOLDER]:
                if not os.path.isdir(folder):
                    self.log.info("%s not found. Creating folder.", folder)
                    os.mkdir(folder)
            self.generate_keypair()
            self.delete_old_vars()
            self.prepare_configurations()
            self.create_defaults()
            self.generate_security_groups()
            self.start_start_server_threads()
            self.extended_network_configuration()
            self.initialize_instances()
            self.upload_data(os.path.join(KEY_FOLDER, self.key_name))
            self.log_cluster_start_info()
            if self.configurations[0].get("deleteTmpKeypairAfter"):
                for provider in self.providers:
                    terminate.delete_keypairs(provider=provider, tmp_keyname=self.key_name, log=self.log)
                terminate.delete_local_keypairs(tmp_keyname=self.key_name, log=self.log)
            if self.debug:
                self.log.info("DEBUG MODE: Entering termination...")
                terminate.terminate(cluster_id=self.cluster_id, providers=self.providers, debug=self.debug,
                                    log=self.log)
        except exceptions.ConnectionException:
            if self.debug:
                self.log.error(traceback.format_exc())
            self.log.error("Connection couldn't be established. Check Provider connection.")
        except paramiko.ssh_exception.NoValidConnectionsError:
            if self.debug:
                self.log.error(traceback.format_exc())
            self.log.error("SSH connection couldn't be established. Check keypair.")
        except KeyError as exc:
            if self.debug:
                self.log.error(traceback.format_exc())
            self.log.error(
                f"Tried to access dictionary key {str(exc)}, but couldn't. Please check your configurations.")
        except FileNotFoundError as exc:
            if self.debug:
                self.log.error(traceback.format_exc())
            self.log.error(f"Tried to access resource files but couldn't. No such file or directory: {str(exc)}")
        except TimeoutError as exc:
            if self.debug:
                self.log.error(traceback.format_exc())
            self.log.error(f"Timeout while connecting to master. Maybe you are trying to create a master without "
                           f"public ip "
                           f"while not being in the same network: {str(exc)}")
        except ExecutionException as exc:
            if self.debug:
                self.log.error(traceback.format_exc())
            self.log.error(f"Execution of cmd on remote host fails: {str(exc)}")
        except ConfigurationException as exc:
            if self.debug:
                self.log.error(traceback.format_exc())
            self.log.error(f"Configuration invalid: {str(exc)}")
        except Exception as exc:  # pylint: disable=broad-except
            self.log.error(traceback.format_exc())
            self.log.error(f"Unexpected error: '{str(exc)}' ({type(exc)}) Contact a developer!)")
        else:
            return 0  # will be called if no exception occurred
        terminate.terminate(cluster_id=self.cluster_id, providers=self.providers, log=self.log, debug=self.debug)
        return 1

    def log_cluster_start_info(self):
        """
        Logs helpful cluster-info:
        SSH: How to connect to master via SSH
        Terminate: What bibigrid command is needed to terminate the created cluster
        Detailed cluster info: How to log detailed info about the created cluster
        @return:
        """
        gateway = self.configurations[0].get("gateway")
        ssh_ip = self.master_ip
        port = None
        if gateway:
            octets = {f'oct{enum + 1}': int(elem) for enum, elem in enumerate(self.master_ip.split("."))}
            port = int(sympy.sympify(gateway["portFunction"]).subs(dict(octets)))
            ssh_ip = gateway["ip"]
        self.log.log(42, f"Cluster {self.cluster_id} with master {self.master_ip} up and running!")
        self.log.log(42, f"SSH: ssh -i '{KEY_FOLDER}{self.key_name}' {self.ssh_user}@{ssh_ip}"
                         f"{f' -p {port}' if gateway else ''}")
        self.log.log(42, f"Terminate cluster: ./bibigrid.sh -i '{self.config_path}' -t -cid {self.cluster_id}")
        self.log.log(42, f"Detailed cluster info: ./bibigrid.sh -i '{self.config_path}' -l -cid {self.cluster_id}")
        if self.configurations[0].get("ide"):
            self.log.log(42, f"IDE Port Forwarding: ./bibigrid.sh -i '{self.config_path}' -ide -cid {self.cluster_id}")

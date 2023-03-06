"""
The cluster creation (master's creation, key creation, ansible setup and execution, ...) is done here
"""

import logging
import os
import subprocess
import threading
import traceback
from functools import partial

import paramiko
import yaml

from bibigrid.core.actions import terminate_cluster
from bibigrid.core.utility import ansible_configurator
from bibigrid.core.utility import id_generation
from bibigrid.core.utility.handler import ssh_handler
from bibigrid.core.utility.paths import ansible_resources_path as aRP
from bibigrid.core.utility.paths import bin_path as biRP
from bibigrid.models import exceptions
from bibigrid.models import return_threading
from bibigrid.models.exceptions import ExecutionException, ConfigurationException

PREFIX = "bibigrid"
SEPARATOR = "-"
PREFIX_WITH_SEP = PREFIX + SEPARATOR
LOG = logging.getLogger("bibigrid")


def get_identifier(identifier, cluster_id, worker_group="", additional=""):
    """
    This method does more advanced string formatting to generate master, vpnwkr and worker names
    @param identifier: master|vpnwkr|worker
    @param cluster_id: id of cluster
    @param worker_group: group of worker (every member of a group has same flavor/type and image)
    @param additional: an additional string to be added at the end
    @return: the generated string
    """
    general = PREFIX_WITH_SEP + identifier + str(worker_group) + SEPARATOR + cluster_id
    if additional or additional == 0:
        return general + SEPARATOR + str(additional)
    return general


MASTER_IDENTIFIER = partial(get_identifier, identifier="master", additional="")
WORKER_IDENTIFIER = partial(get_identifier, identifier="worker")
VPN_WORKER_IDENTIFIER = partial(get_identifier, identifier="vpnwkr")

KEY_PREFIX = "tempKey_bibi"
KEY_FOLDER = os.path.expanduser("~/.config/bibigrid/keys/")
AC_NAME = "ac" + SEPARATOR + "{cluster_id}"
KEY_NAME = KEY_PREFIX + SEPARATOR + "{cluster_id}"
CLUSTER_MEMORY_FOLDER = KEY_FOLDER
CLUSTER_MEMORY_FILE = ".bibigrid.mem"
CLUSTER_MEMORY_PATH = os.path.join(CLUSTER_MEMORY_FOLDER, CLUSTER_MEMORY_FILE)
DEFAULT_SECURITY_GROUP_NAME = "default" + SEPARATOR + "{cluster_id}"
WIREGUARD_SECURITY_GROUP_NAME = "wireguard" + SEPARATOR + "{cluster_id}"


class Create:  # pylint: disable=too-many-instance-attributes,too-many-arguments
    """
    The class Create holds necessary methods to execute the Create-Action
    """

    def __init__(self, providers, configurations, config_path, debug=False):
        """
        Additionally sets (unique) cluster_id, public_key_commands (to copy public keys to master) and key_name.
        Call create() to actually start server.
        :param providers: List of providers (provider)
        :param configurations: List of configurations (dict)
        :param config_path: string that is the path to config-file
        :param debug: Bool. If True Cluster offer shut-down after create and
        will ask before shutting down on errors
        """
        self.providers = providers
        self.configurations = configurations
        self.debug = debug
        self.cluster_id = id_generation.generate_safe_cluster_id(providers)
        self.ssh_user = configurations[0].get("sshUser") or "ubuntu"
        self.ssh_add_public_key_commands = ssh_handler.get_add_ssh_public_key_commands(
            configurations[0].get("sshPublicKeyFiles"))
        self.config_path = config_path
        self.master_ip = None
        LOG.debug("Cluster-ID: %s", self.cluster_id)
        self.name = AC_NAME.format(cluster_id=self.cluster_id)
        self.key_name = KEY_NAME.format(cluster_id=self.cluster_id)
        self.default_security_group_name = DEFAULT_SECURITY_GROUP_NAME.format(cluster_id=self.cluster_id)
        self.wireguard_security_group_name = WIREGUARD_SECURITY_GROUP_NAME.format(cluster_id=self.cluster_id)

        self.worker_counter = 0
        self.vpn_counter = 0
        self.thread_lock = threading.Lock()
        self.use_master_with_public_ip = configurations[0].get("useMasterWithPublicIp", True)
        LOG.debug("Keyname: %s", self.key_name)

    def generate_keypair(self):
        """
        Generates ECDSA Keypair using system-function ssh-keygen and uploads the generated public key to providers.
        generate_keypair makes use of the fact that files in tmp are automatically deleted
        ToDo find a more pythonic way to create an ECDSA keypair
        See here for why using python module ECDSA wasn't successful
        https://stackoverflow.com/questions/71194770/why-does-creating-ecdsa-keypairs-via-python-differ-from-ssh-keygen-t-ecdsa-and
        :return:
        """
        # create KEY_FOLDER if it doesn't exist
        if not os.path.isdir(KEY_FOLDER):
            LOG.info("%s not found. Creating folder.", KEY_FOLDER)
            os.mkdir(KEY_FOLDER)
        # generate keyfile
        res = subprocess.check_output(f'ssh-keygen -t ecdsa -f {KEY_FOLDER}{self.key_name} -P ""', shell=True).decode()
        LOG.debug(res)
        # read private keyfile
        with open(f"{os.path.join(KEY_FOLDER, self.key_name)}.pub", mode="r", encoding="UTF-8") as key_file:
            public_key = key_file.read()
        # upload keyfiles
        for provider in self.providers:
            provider.create_keypair(name=self.key_name, public_key=public_key)

        # write cluster_id to automatically read it on following calls if no cid is given
        with open(CLUSTER_MEMORY_PATH, mode="w+", encoding="UTF-8") as cluster_memory_file:
            yaml.safe_dump(data={"cluster_id": self.cluster_id}, stream=cluster_memory_file)

    def generate_security_groups(self):
        """
        Generate a security groups:
         - default with basic rules for the cluster
         - wireguard when more than one provider is used (= multicloud)
        """
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
                        rules.append(
                            {"direction": "ingress", "ethertype": "IPv4", "protocol": "tcp", "port_range_min": None,
                             "port_range_max": None, "remote_ip_prefix": tmp_configuration['subnet_cidrs'],
                             "remote_group_id": None})
            provider.append_rules_to_security_group(default_security_group_id, rules)
            configuration["security_groups"] = [self.default_security_group_name]  # store in configuration
            # when running a multi-cloud setup create an additional wireguard group
            if len(self.providers) > 1:
                _ = provider.create_security_group(name=self.wireguard_security_group_name)["id"]
                configuration["security_groups"].append(self.wireguard_security_group_name)  # store in configuration

    def start_vpn_or_master_instance(self, configuration, provider):
        """
        Start master/vpn-worker of a provider
        :param configuration: dict configuration of said provider
        :param provider: provider
        :return:
        """
        identifier, instance_type, volumes = self.prepare_vpn_or_master_args(configuration, provider)
        external_network = provider.get_external_network(configuration["network"])
        with self.thread_lock:
            if identifier == MASTER_IDENTIFIER:  # pylint: disable=comparison-with-callable
                name = identifier(cluster_id=self.cluster_id)
            else:
                name = identifier(cluster_id=self.cluster_id,   # pylint: disable=redundant-keyword-arg
                                  additional=self.vpn_counter)  # pylint: disable=redundant-keyword-arg
                self.vpn_counter += 1
        LOG.info(f"Starting instance/server {name} on {provider.NAME}")
        flavor = instance_type["type"]
        image = instance_type["image"]
        network = configuration["network"]

        # create a server and block until it is up and running
        server = provider.create_server(name=name, flavor=flavor, key_name=self.key_name, image=image, network=network,
                                        volumes=volumes, security_groups=configuration["security_groups"], wait=True)
        configuration["private_v4"] = server["private_v4"]

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
        elif identifier == MASTER_IDENTIFIER:
            configuration["floating_ip"] = server["private_v4"]  # pylint: enable=comparison-with-callable

    def prepare_vpn_or_master_args(self, configuration, provider):
        """
        Prepares start_instance arguments for master/vpn
        :param configuration: configuration (dict) of said master/vpn
        :param provider: provider
        :return: arguments needed by start_instance
        """
        if configuration.get("masterInstance"):
            instance_type = configuration["masterInstance"]
            identifier = MASTER_IDENTIFIER
            master_mounts = configuration.get("masterMounts", [])
            volumes = self.prepare_volumes(provider, master_mounts)
        elif configuration.get("vpnInstance"):
            instance_type = configuration["vpnInstance"]
            identifier = VPN_WORKER_IDENTIFIER
            volumes = []  # only master has volumes
        else:
            LOG.warning("Configuration %s has no vpnwkr or master and is therefore unreachable.", configuration)
            raise KeyError
        return identifier, instance_type, volumes

    def initialize_instances(self):
        """
        Setup all servers
        """
        for configuration in self.configurations:
            if configuration.get("masterInstance"):
                self.master_ip = configuration["floating_ip"]
                ssh_handler.ansible_preparation(floating_ip=configuration["floating_ip"],
                                                private_key=KEY_FOLDER + self.key_name, username=self.ssh_user,
                                                commands=self.ssh_add_public_key_commands)
            elif configuration.get("vpnInstance"):
                ssh_handler.execute_ssh(floating_ip=configuration["floating_ip"],
                                        private_key=KEY_FOLDER + self.key_name, username=self.ssh_user,
                                        commands=ssh_handler.VPN_SETUP)

    def prepare_volumes(self, provider, mounts):
        """
        Creates volumes from snapshots and returns all volumes (pre-existing and newly created)
        :param provider: provider on which the volumes and snapshots exist
        :param mounts: volumes or snapshots
        :return: list of pre-existing and newly created volumes
        """
        LOG.info("Preparing volumes")
        volumes = []
        for mount in mounts:
            volume_id = provider.get_volume_by_id_or_name(mount)["id"]
            if volume_id:
                volumes.append(volume_id)
            else:
                LOG.debug("Volume %s does not exist. Checking for snapshot.", mount)
                volume_id = provider.create_volume_from_snapshot(mount)
                if volume_id:
                    volumes.append(volume_id)
                else:
                    LOG.warning("Mount %s is neither a snapshot nor a volume.", mount)
        ret_volumes = set(volumes)
        if len(ret_volumes) < len(volumes):
            LOG.warning("Identical mounts found in masterMounts list. "
                        "Trying to set() to save the run. Check configurations!")
        return ret_volumes

    def prepare_configurations(self):
        """
        Makes sure that subnet and network key are set for each configuration.
        If none is set a keyError will be raised and caught in create.
        :return:
        """
        for configuration, provider in zip(self.configurations, self.providers):
            if not configuration.get("network"):
                configuration["network"] = provider.get_network_id_by_subnet(configuration["subnet"])
            elif not configuration.get("subnet"):
                configuration["subnet"] = provider.get_subnet_ids_by_network(configuration["network"])
            configuration["subnet_cidrs"] = provider.get_subnet_by_id_or_name(configuration["subnet"])["cidr"]
            configuration["cloud_specification"] = provider.cloud_specification["identifier"]
            configuration["sshUser"] = self.ssh_user  # is used in ansibleConfigurator

    def upload_data(self):
        """
        Configures ansible and then uploads the modified files and all necessary data to the master
        :return:
        """
        if not os.path.isdir(aRP.VARS_FOLDER):
            LOG.info("%s not found. Creating folder.", aRP.VARS_FOLDER)
            os.mkdir(aRP.VARS_FOLDER)
        ansible_configurator.configure_ansible_yaml(providers=self.providers, configurations=self.configurations,
                                                    cluster_id=self.cluster_id)
        ssh_handler.execute_ssh(floating_ip=self.master_ip, private_key=KEY_FOLDER + self.key_name,
                                username=self.ssh_user,
                                filepaths=[(aRP.PLAYBOOK_PATH, aRP.PLAYBOOK_PATH_REMOTE),
                                           (biRP.BIN_PATH, biRP.BIN_PATH_REMOTE)],
                                commands=[
                                             ssh_handler.get_ac_command(
                                                 self.providers,
                                                 AC_NAME.format(
                                                     cluster_id=self.cluster_id))] + ssh_handler.ANSIBLE_START)

    def start_start_instances_threads(self):
        """
        Starts for each provider a start_instances thread and joins them.
        :return:
        """
        start_instances_threads = []
        for configuration, provider in zip(self.configurations, self.providers):
            start_instances_thread = return_threading.ReturnThread(target=self.start_vpn_or_master_instance,
                                                                   args=[configuration, provider])
            start_instances_thread.start()
            start_instances_threads.append(start_instances_thread)
        for start_instance_thread in start_instances_threads:
            start_instance_thread.join()

    def extended_network_configuration(self):
        """
            Configure master/vpn-worker network for a multi/hybrid cloud
        :return:
        """
        if len(self.providers) == 1:
            return

        for provider_a, configuration_a in zip(self.providers, self.configurations):
            # configure wireguard network as allowed network
            allowed_addresses = [{'ip_address': '10.0.0.0/24', 'mac_address': configuration_a["mac_addr"]}]
            # iterate over all configurations ...
            for configuration_b in self.configurations:
                # ... and pick all other configuration
                if configuration_a != configuration_b:
                    LOG.info(f"{configuration_a['private_v4']} --> allowed_address_pair({configuration_a['mac_addr']},"
                             f"{configuration_b['subnet_cidrs']})")
                    # add provider_b network as allowed network
                    allowed_addresses.append(
                        {'ip_address': configuration_b["subnet_cidrs"], 'mac_address': configuration_a["mac_addr"]})
                    # configure security group rules
                    provider_a.append_rules_to_security_group(self.wireguard_security_group_name, [
                        {"direction": "ingress", "ethertype": "IPv4", "protocol": "udp", "port_range_min": 51820,
                         "port_range_max": 51820, "remote_ip_prefix": configuration_b["floating_ip"],
                         "remote_group_id": None}])
            # configure allowed addresses for provider_a/configuration_a
            provider_a.set_allowed_addresses(configuration_a['private_v4'], allowed_addresses)

    def create(self):  # pylint: disable=too-many-branches
        """
        Creates cluster and prints helpful cluster-info afterwards.
        If debug is set True it offers termination after starting the cluster.
        :return: exit_state
        """
        try:
            self.generate_keypair()
            self.prepare_configurations()
            self.generate_security_groups()
            self.start_start_instances_threads()
            self.extended_network_configuration()
            self.initialize_instances()
            self.upload_data()
            self.print_cluster_start_info()
            if self.debug:
                LOG.info("DEBUG MODE: Entering termination...")
                terminate_cluster.terminate_cluster(cluster_id=self.cluster_id, providers=self.providers,
                                                    debug=self.debug)
        except exceptions.ConnectionException:
            if self.debug:
                LOG.error(traceback.format_exc())
            LOG.error("Connection couldn't be established. Check Provider connection.")
        except paramiko.ssh_exception.NoValidConnectionsError:
            if self.debug:
                LOG.error(traceback.format_exc())
            LOG.error("SSH connection couldn't be established. Check keypair.")
        except KeyError as exc:
            if self.debug:
                LOG.error(traceback.format_exc())
            LOG.error(f"Tried to access dictionary key {str(exc)}, but couldn't. Please check your configurations.")
        except FileNotFoundError as exc:
            if self.debug:
                LOG.error(traceback.format_exc())
            LOG.error(f"Tried to access resource files but couldn't. No such file or directory: {str(exc)}")
        except TimeoutError as exc:
            if self.debug:
                LOG.error(traceback.format_exc())
            LOG.error(f"Timeout while connecting to master. Maybe you are trying to create a master without "
                      f"public ip "
                      f"while not being in the same network: {str(exc)}")
        except ExecutionException as exc:
            if self.debug:
                LOG.error(traceback.format_exc())
            LOG.error(f"Execution of cmd on remote host fails: {str(exc)}")
        except Exception as exc:  # pylint: disable=broad-except
            if self.debug:
                LOG.error(traceback.format_exc())
            LOG.error(f"Unexpected error: '{str(exc)}' ({type(exc)}) Contact a developer!)")
        else:
            return 0  # will be called if no exception occurred
        terminate_cluster.terminate_cluster(cluster_id=self.cluster_id, providers=self.providers, debug=self.debug)
        return 1

    def print_cluster_start_info(self):
        """
        Prints helpful cluster-info:
        SSH: How to connect to master via SSH
        Terminate: What bibigrid command is needed to terminate the created cluster
        Detailed cluster info: How to print detailed info about the created cluster
        :return:
        """
        print(f"Cluster {self.cluster_id} with master {self.master_ip} up and running!")
        print(f"SSH: ssh -i '{KEY_FOLDER}{self.key_name}' {self.ssh_user}@{self.master_ip}")
        print(f"Terminate cluster: ./bibigrid.sh -i '{self.config_path}' -t -cid {self.cluster_id}")
        print(f"Detailed cluster info: ./bibigrid.sh -i '{self.config_path}' -l -cid {self.cluster_id}")
        if self.configurations[0].get("ide"):
            print(f"IDE Port Forwarding: ./bibigrid.sh -i '{self.config_path}' -ide -cid {self.cluster_id}")

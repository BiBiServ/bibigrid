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
from bibigrid.models.exceptions import ExecutionException

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
        :param debug: Bool. If True Cluster will offer shut-down after create and
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
        self.worker_counter = 0
        self.vpn_counter = 0
        self.thread_lock = threading.Lock()
        self.use_master_with_public_ip = configurations[0].get("useMasterWithPublicIp", True)
        LOG.debug("Keyname: %s", self.key_name)

    def generate_keypair(self):
        """
        Generates ECDSA Keypair using system-function ssh-keygen and uploads the generated public key to providers.
        generate_keypair makes use of the fact that files in tmp are automatically deleted
        ToDo find a more pythonic way to create an ECDSA keypiar
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

    def start_instance(self, provider, identifier, instance_type, network, volumes=None,
                       external_network=None):
        """
        Starts any (master,worker,vpn) single server/instance in given network on given provider
        with floating-ip if master or vpn and with volume if master.
        :param provider: provider server will be started on
        :param identifier: string MASTER/WORKER/VPN_IDENTIFIER
        :param instance_type: dict from configuration containing server type, image and count (but count is not needed)
        :param network: string network where server will be started in.
        All server of a provider are started in the same network
        :param volumes: list of volumes that are to be attached to the server. Currently only relevant for master
        :param external_network: string only needed if worker=False to create floating_ip
        :return:
        """
        # potentially weird counting due to master
        with self.thread_lock:
            if identifier == MASTER_IDENTIFIER:  # pylint: disable=comparison-with-callable
                name = identifier(cluster_id=self.cluster_id)
            #elif identifier == WORKER_IDENTIFIER:  # pylint: disable=comparison-with-callable
            #    name = identifier(number=self.worker_counter, cluster_id=self.cluster_id)
            #    self.worker_counter += 1
            else:
                name = identifier(cluster_id=self.cluster_id, additional=self.vpn_counter)
                self.vpn_counter += 1
        LOG.info("Starting instance/server %s", name)
        flavor = instance_type["type"]
        image = instance_type["image"]
        server = provider.create_server(name=name, flavor=flavor, key_name=self.key_name,
                                        image=image, network=network, volumes=volumes)
        floating_ip = None
        # pylint: disable=comparison-with-callable
        if identifier == VPN_WORKER_IDENTIFIER or (
                identifier == MASTER_IDENTIFIER and self.use_master_with_public_ip):
            # wait seems to be included. Not in documentation
            floating_ip = provider.attach_available_floating_ip(network=external_network,
                                                                server=server)["floating_ip_address"]
        elif identifier == MASTER_IDENTIFIER:
            floating_ip = provider.conn.get_server(server["id"])["private_v4"]
        # pylint: enable=comparison-with-callable
        return floating_ip

    def start_instances(self, configuration, provider):
        """
        Starts all instances of a provider using multithreading
        :param configuration: dict configuration of said provider
        :param provider: provider
        :return:
        """
        LOG.info("Starting instances on %s", provider.NAME)
        # threads = []
        identifier, instance_type, volumes = self.prepare_vpn_or_master_args(configuration, provider)
        external_network = provider.get_external_network(configuration["network"])

        # Starts master/vpn. Uses return threading to get floating_ip of master/vpn
        vpn_or_master_thread = return_threading.ReturnThread(target=self.start_instance,
                                                             args=[provider,
                                                                   identifier,
                                                                   instance_type,
                                                                   configuration["network"],
                                                                   volumes,
                                                                   external_network])
        vpn_or_master_thread.start()

        # Starts all workers
        # for worker_instance_type in configuration.get("workerInstances") or []:
        #     for worker in range(worker_instance_type["count"]):
        #         worker_thread = threading.Thread(target=self.start_instance,
        #                                          args=[provider,
        #                                                WORKER_IDENTIFIER,
        #                                                worker_instance_type,
        #                                                configuration["network"],
        #                                                True])
        #         worker_thread.start()
        #         threads.append(worker_thread)
        LOG.info("Waiting for servers to start-up on cloud %s", provider.cloud_specification['identifier'])
        vpn_or_m_floating_ip_address = vpn_or_master_thread.join()
        configuration["floating_ip"] = vpn_or_m_floating_ip_address
        self.setup_reachable_servers(configuration, vpn_or_m_floating_ip_address)
        # for thread in threads:
        #     thread.join()

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

    def setup_reachable_servers(self, configuration, vpn_or_m_floating_ip_address):
        """
        Executes necessary commands on master or vpnwkr
        :param configuration: said configuration
        :param vpn_or_m_floating_ip_address: floating_ip to master or vpnwkr
        """
        if configuration.get("masterInstance"):
            self.master_ip = vpn_or_m_floating_ip_address
            ssh_handler.ansible_preparation(floating_ip=vpn_or_m_floating_ip_address,
                                            private_key=KEY_FOLDER + self.key_name,
                                            username=self.ssh_user,
                                            commands=self.ssh_add_public_key_commands)
        elif configuration.get("vpnInstance"):
            ssh_handler.execute_ssh(floating_ip=vpn_or_m_floating_ip_address,
                                    private_key=KEY_FOLDER + self.key_name,
                                    username=self.ssh_user,
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
            print("TEST", provider.get_subnet_ids_by_network(configuration["network"]))
            configuration["sshUser"] = self.ssh_user  # is used in ansibleConfigurator

    def upload_data(self):
        """
        Configures ansible and then uploads the modified files and all necessary data to the master
        :return:
        """
        if not os.path.isdir(aRP.VARS_FOLDER):
            LOG.info("%s not found. Creating folder.", aRP.VARS_FOLDER)
            os.mkdir(aRP.VARS_FOLDER)
        ansible_configurator.configure_ansible_yaml(providers=self.providers,
                                                    configurations=self.configurations,
                                                    cluster_id=self.cluster_id)
        ssh_handler.execute_ssh(floating_ip=self.master_ip, private_key=KEY_FOLDER + self.key_name,
                                username=self.ssh_user,
                                filepaths=[(aRP.PLAYBOOK_PATH, aRP.PLAYBOOK_PATH_REMOTE),
                                           (biRP.BIN_PATH, biRP.BIN_PATH_REMOTE)],
                                commands=ssh_handler.ANSIBLE_START +
                                         [ssh_handler.get_ac_command(self.providers[0], AC_NAME.format(
                                             cluster_id=self.cluster_id))])

    def start_start_instances_threads(self):
        """
        Starts for each provider a start_instances thread and joins them.
        :return:
        """
        start_instances_threads = []
        for configuration, provider in zip(self.configurations, self.providers):
            start_instances_thread = return_threading.ReturnThread(target=self.start_instances,
                                                                   args=[configuration, provider])
            start_instances_thread.start()
            start_instances_threads.append(start_instances_thread)
        for start_instance_thread in start_instances_threads:
            start_instance_thread.join()

    def create(self):
        """
        Creates cluster and prints helpful cluster-info afterwards.
        If debug is set True it offers termination after starting the cluster.
        :return: exit_state
        """
        self.generate_keypair()
        try:
            self.prepare_configurations()
            self.start_start_instances_threads()
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

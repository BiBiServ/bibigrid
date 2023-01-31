"""
Prepares ansible files (vars, common_configuration, ...)
"""

import logging

import mergedeep
import yaml

from bibigrid.core.actions import create
from bibigrid.core.actions import ide
from bibigrid.core.utility import id_generation
from bibigrid.core.utility import yaml_dumper
from bibigrid.core.utility.handler import configuration_handler
from bibigrid.core.utility.paths import ansible_resources_path as aRP
from bibigrid.core.utility.wireguard import wireguard_keys

DEFAULT_NFS_SHARES = ["/vol/spool"]
ADDITIONAL_PATH = "additional/"
PYTHON_INTERPRETER = "/usr/bin/python3"
MASTER_ROLES = [{"role": "bibigrid", "tags": ["bibigrid", "bibigrid-master"]}]
WORKER_ROLES = [{"role": "bibigrid", "tags": ["bibigrid", "bibigrid-worker"]}]
VARS_FILES = [aRP.INSTANCES_YML, aRP.CONFIG_YML]
IDE_CONF = {"ide": False, "workspace": ide.DEFAULT_IDE_WORKSPACE, "port_start": ide.REMOTE_BIND_ADDRESS,
            "port_end": ide.DEFAULT_IDE_PORT_END, "build": False}
ZABBIX_CONF = {"db": "zabbix", "db_user": "zabbix", "db_password": "zabbix", "timezone": "Europe/Berlin",
               "server_name": "bibigrid", "admin_password": "bibigrid"}
SLURM_CONF = {"db": "slurm", "db_user": "slurm", "db_password": "changeme",
              "munge_key": id_generation.generate_munge_key(),
              "elastic_scheduling": {"SuspendTime": 3600, "ResumeTimeout": 900, "TreeWidth": 128}}
LOG = logging.getLogger("bibigrid")


def generate_site_file_yaml(custom_roles):
    """
    Generates site_yaml (dict).
    Deepcopy is used in case roles might differ between servers in the future.
    :param custom_roles: ansibleRoles given by the config
    :return: site_yaml (dict)
    """
    site_yaml = [{'hosts': 'master', "become": "yes",
                  "vars_files": VARS_FILES, "roles": MASTER_ROLES},
                 {"hosts": "workers", "become": "yes", "vars_files": VARS_FILES,
                  "roles": WORKER_ROLES}]  # ,
    # {"hosts": "vpnwkr", "become": "yes", "vars_files": copy.deepcopy(VARS_FILES),
    # "roles": ["common", "vpnwkr"]}]
    # add custom roles and vars
    for custom_role in custom_roles:
        VARS_FILES.append(custom_role["vars_file"])
        MASTER_ROLES.append(ADDITIONAL_PATH + custom_role["name"])
        WORKER_ROLES.append(ADDITIONAL_PATH + custom_role["name"])
    return site_yaml


def generate_instances_yaml(configurations, providers, cluster_id):  # pylint: disable=too-many-locals
    """
    ToDo filter what information really is necessary. Determined by further development
    Filters unnecessary information
    :param configurations: configurations
    :param providers: providers
    :param cluster_id: To get proper naming
    :return: filtered information (dict)
    """
    LOG.info("Generating instances file...")
    instances = {}
    flavor_keys = ["name", "ram", "vcpus", "disk", "ephemeral"]
    worker_count = 0
    vpn_count = 0
    for configuration, provider in zip(configurations, providers):
        if not instances.get(configuration["cloud_specification"]):
            instances[configuration["cloud_specification"]] = {}
            instances[configuration["cloud_specification"]]["workers"] = []
        for index, worker in enumerate(configuration.get("workerInstances", [])):
            name = create.WORKER_IDENTIFIER(worker_group=index, cluster_id=cluster_id,
                                            additional=f"[{worker_count}-{worker_count + worker.get('count', 1) - 1}]")
            worker_count += worker.get('count', 1)
            flavor = provider.get_flavor(worker["type"])
            flavor_dict = {key: flavor[key] for key in flavor_keys}
            image = worker["image"]
            network = configuration["network"]
            regexp = create.WORKER_IDENTIFIER(worker_group=index, cluster_id=cluster_id, additional=r"\d+")
            instances[configuration["cloud_specification"]]["workers"].append(
                {"name": name, "regexp": regexp, "image": image,
                 "network": network, "flavor": flavor_dict})
        vpnwkr = configuration.get("vpnInstance")
        if vpnwkr:
            name = create.VPN_WORKER_IDENTIFIER(cluster_id=cluster_id,
                                                additional=f"{vpn_count}")
            vpn_count += 1
            flavor = provider.get_flavor(vpnwkr["type"])
            flavor_dict = {key: flavor[key] for key in flavor_keys}
            image = vpnwkr["image"]
            network = configuration["network"]
            regexp = create.WORKER_IDENTIFIER(cluster_id=cluster_id, additional=r"\d+")
            instances[configuration["cloud_specification"]]["vpnwkr"] = {"name": name, "regexp": regexp,
                                                                               "image": image, "network": network,
                                                                               "floating_ip": configuration[
                                                                                   "floating_ip"],
                                                                               "private_v4": configuration[
                                                                                   "private_v4"],
                                                                               "flavor": flavor_dict}
        else:
            master = configuration["masterInstance"]
            name = create.MASTER_IDENTIFIER(cluster_id=cluster_id)
            flavor = provider.get_flavor(master["type"])
            flavor_dict = {key: flavor[key] for key in flavor_keys}
            image = master["image"]
            network = configuration["network"]
            instances["master"] = {"name": name,
                                   "image": image,
                                   "network": network,
                                   "floating_ip": configuration["floating_ip"],
                                   "flavor": flavor_dict,
                                   "private_v4": configuration["private_v4"],
                                   "cloud_specification": configuration["cloud_specification"]}
    instances_yml = {"instances": instances}
    return instances_yml


def pass_through(dict_from, dict_to, key_from, key_to=None):
    """
    If key is defined in dict_from, set key of dict_to to value of corresponding value of dict_from. Happens in place.
    @param key_from:
    @param key_to:
    @param dict_from:
    @param dict_to:
    @return:
    """
    if not key_to:
        key_to = key_from
    if dict_from.get(key_from):
        dict_to[key_to] = dict_from[key_from]


def generate_common_configuration_yaml(cidrs, configurations, cluster_id, ssh_user, default_user):
    """
    Generates common_configuration yaml (dict)
    :param cidrs: str subnet cidrs (provider generated)
    :param configurations: master configuration (first in file)
    :param cluster_id: Id of cluster
    :param ssh_user: user for ssh connections
    :param default_user: Given default user
    :return: common_configuration_yaml (dict)
    """
    master_configuration = configurations[0]
    LOG.info("Generating common configuration file...")
    # print(configuration.get("slurmConf", {}))
    common_configuration_yaml = {"cluster_id": cluster_id, "cluster_cidrs": cidrs,
                                 "default_user": default_user,
                                 "local_fs": master_configuration.get("localFS", False),
                                 "local_dns_lookup": master_configuration.get("localDNSlookup", False),
                                 "use_master_as_compute": master_configuration.get("useMasterAsCompute", True),
                                 "dns_server_list": master_configuration.get("dns_server_list",["8.8.8.8"]),
                                 "enable_slurm": master_configuration.get("slurm", False),
                                 "enable_zabbix": master_configuration.get("zabbix", False),
                                 "enable_nfs": master_configuration.get("nfs", False),
                                 "enable_ide": master_configuration.get("ide", False),
                                 "slurm": master_configuration.get("slurm", True),
                                 "ssh_user": ssh_user,
                                 "slurm_conf": mergedeep.merge({}, SLURM_CONF,
                                                               master_configuration.get("slurmConf", {}),
                                                               strategy=mergedeep.Strategy.TYPESAFE_REPLACE)
                                 }
    if master_configuration.get("nfs"):
        nfs_shares = master_configuration.get("nfsShares", [])
        nfs_shares = nfs_shares + DEFAULT_NFS_SHARES
        common_configuration_yaml["nfs_mounts"] = [{"src": "/" + nfs_share, "dst": "/" + nfs_share}
                                                   for nfs_share in nfs_shares]
        common_configuration_yaml["ext_nfs_mounts"] = [{"src": ext_nfs_share, "dst": ext_nfs_share} for
                                                       ext_nfs_share in (master_configuration.get("extNfsShares", []))]

    if master_configuration.get("ide"):
        common_configuration_yaml["ide_conf"] = mergedeep.merge({}, IDE_CONF, master_configuration.get("ideConf", {}),
                                                                strategy=mergedeep.Strategy.TYPESAFE_REPLACE)
    if master_configuration.get("zabbix"):
        common_configuration_yaml["zabbix_conf"] = mergedeep.merge({}, ZABBIX_CONF,
                                                                   master_configuration.get("zabbixConf", {}),
                                                                   strategy=mergedeep.Strategy.TYPESAFE_REPLACE)

    for from_key, to_key in [("waitForServices", "wait_for_services"), ("ansibleRoles", "ansible_roles"),
                             ("ansibleGalaxyRoles", "ansible_galaxy_roles")]:
        pass_through(master_configuration, common_configuration_yaml, from_key, to_key)

    if len(configurations) > 1:
        peers = []
        for configuration in configurations:
            private_key, public_key = wireguard_keys.generate()
            peers.append({"name": configuration["cloud_specification"],
                          "private_key": private_key,
                          "public_key": public_key,
                          "ip": configuration["floating_ip"],
                          "subnet": configuration["subnet_cidrs"]})
            # subnet
        common_configuration_yaml["wireguard"] = {"mask_bits": 24, "listen_port": 51820, "peers": peers}

    return common_configuration_yaml


def generate_ansible_hosts_yaml(ssh_user, configurations, cluster_id):
    """
    Generates ansible_hosts_yaml (inventory file).
    :param ssh_user: str global SSH-username
    :param configurations: dict
    :param cluster_id: id of cluster
    :return: ansible_hosts yaml (dict)
    """
    LOG.info("Generating ansible hosts file...")
    ansible_hosts_yaml = {"master": {"hosts": {"localhost": to_instance_host_dict(ssh_user)}},
                          "workers": {"hosts": {}, "children": {"ephemeral": {"hosts": {}}, "vpnwkrs": {"hosts": {}}}}
                          }
    # vpnwkr are handled like workers on this level
    workers = ansible_hosts_yaml["workers"]

    worker_count = 0
    vpn_count = 0
    for configuration in configurations:
        for index, worker in enumerate(configuration.get("workerInstances", [])):
            name = create.WORKER_IDENTIFIER(worker_group=index, cluster_id=cluster_id,
                                            additional=f"[{worker_count}:{worker_count + worker.get('count', 1) - 1}]")
            worker_count += worker.get('count', 1)
            worker_dict = to_instance_host_dict(ssh_user, ip="", local=False)
            if "ephemeral" in worker["type"]:
                workers["children"]["ephemeral"]["hosts"][name] = worker_dict
            else:
                workers["hosts"][name] = worker_dict

            if configuration.get("vpnInstance"):
                name = create.VPN_WORKER_IDENTIFIER(cluster_id=cluster_id,
                                                    additional=vpn_count)
                vpn_dict = to_instance_host_dict(ssh_user, ip="", local=False)
                vpn_dict["ansible_host"] = configuration["floating_ip"]
                workers["children"]["vpnwkrs"]["hosts"][name] = vpn_dict
    return ansible_hosts_yaml


def to_instance_host_dict(ssh_user, ip="localhost", local=True):  # pylint: disable=invalid-name
    """
    Generates host entry
    :param ssh_user: str global SSH-username
    :param ip: str ip
    :param local: bool
    :return: host entry (dict)
    """
    host_yaml = {"ansible_connection": "local" if local else "ssh",
                 "ansible_python_interpreter": PYTHON_INTERPRETER,
                 "ansible_user": ssh_user}
    if ip:
        host_yaml["ip"] = ip
    return host_yaml


def get_cidrs(configurations):
    """
    Gets cidrs of all subnets in all providers
    :param configurations: list of configurations (dict)
    :param providers: list of providers
    :return:
    """
    all_cidrs = []
    for configuration in configurations:
        subnet = configuration["subnet_cidrs"]
        provider_cidrs = {"provider": configuration["cloud_specification"], "provider_cidrs": subnet}
        all_cidrs.append(provider_cidrs)
    return all_cidrs


def get_ansible_roles(ansible_roles):
    """
    Checks if ansible_roles have all necessary values and returns True if so.
    :param ansible_roles: ansible_roles from master configuration (first configuration)
    :return: list of valid ansible_roles
    """
    ansible_roles_yaml = []
    for ansible_role in (ansible_roles or []):
        if ansible_role.get("file") and ansible_role.get("hosts"):
            ansible_role_dict = {"file": ansible_role["file"], "hosts": ansible_role["hosts"]}
            for key in ["name", "vars", "vars_file"]:
                if ansible_role.get(key):
                    ansible_role_dict[key] = ansible_role[key]
            ansible_roles_yaml.append(ansible_role_dict)
        else:
            LOG.warning("Ansible role %s had neither galaxy,git nor url. Not added.", ansible_role)
    return ansible_roles_yaml


def get_ansible_galaxy_roles(ansible_galaxy_roles):
    """
    Checks if ansible_galaxy_role have all necessary values and adds it to the return list if so.
    :param ansible_galaxy_roles:
    :return: list of valid ansible_galaxy_roles
    """
    ansible_galaxy_roles_yaml = []
    for ansible_galaxy_role in (ansible_galaxy_roles or []):
        if ansible_galaxy_role.get("galaxy") or ansible_galaxy_role.get("git") or ansible_galaxy_role.get("url"):
            ansible_galaxy_role_dict = {"hosts": ansible_galaxy_role["hosts"]}
            for key in ["name", "galaxy", "git", "url", "vars", "vars_file"]:
                if ansible_galaxy_role.get(key):
                    ansible_galaxy_role_dict[key] = ansible_galaxy_role[key]
            ansible_galaxy_roles_yaml.append(ansible_galaxy_role_dict)
        else:
            LOG.warning("Galaxy role %s had neither galaxy,git nor url. Not added.", ansible_galaxy_role)
    return ansible_galaxy_roles_yaml


def generate_worker_specification_file_yaml(configurations):
    """
    Generates worker_specification_file_yaml
    :param configurations: list of configurations (dict)
    :return: worker_specification_yaml
    """
    LOG.info("Generating worker specification file...")
    worker_groups_list = configuration_handler.get_list_by_key(configurations, "workerInstances", False)
    # create.prepare_configuration guarantees that key is set
    network_list = configuration_handler.get_list_by_key(configurations, "network", False)
    worker_specification_yaml = []
    for worker_groups_provider_list, network in zip(worker_groups_list, network_list):
        for worker_group in worker_groups_provider_list:
            worker_specification_yaml.append({"TYPE": worker_group["type"],
                                              "IMAGE": worker_group["image"],
                                              "NETWORK": network})
    return worker_specification_yaml


def write_yaml(path, generated_yaml, alias=False):
    """
    Writes generated_yaml to file path with or without alias
    @param path:
    @param generated_yaml:
    @param alias:
    @return:
    """
    LOG.debug("Writing yaml %s", path)
    with open(path, mode="w+", encoding="UTF-8") as file:
        if alias:
            yaml.safe_dump(data=generated_yaml, stream=file)
        else:
            yaml.dump(data=generated_yaml, stream=file, Dumper=yaml_dumper.NoAliasSafeDumper)


def configure_ansible_yaml(providers, configurations, cluster_id):
    """
    Generates and writes all ansible-configuration-yaml files.
    :param providers: list of providers
    :param configurations: list of configurations (dict)
    :param cluster_id: id of cluster to create
    :return:
    """
    LOG.info("Writing ansible files...")
    alias = configurations[0].get("aliasDumper", False)
    ansible_roles = get_ansible_roles(configurations[0].get("ansibleRoles"))
    default_user = providers[0].cloud_specification["auth"].get("username", configurations[0].get("sshUser", "Ubuntu"))
    for path, generated_yaml in [
        (aRP.WORKER_SPECIFICATION_FILE, generate_worker_specification_file_yaml(configurations)),
        (aRP.COMMONS_CONFIG_FILE, generate_common_configuration_yaml(cidrs=get_cidrs(configurations),
                                                                     configurations=configurations,
                                                                     cluster_id=cluster_id,
                                                                     ssh_user=configurations[0]["sshUser"],
                                                                     default_user=default_user)),
        (aRP.COMMONS_INSTANCES_FILE, generate_instances_yaml(configurations, providers, cluster_id)),
        (aRP.HOSTS_CONFIG_FILE, generate_ansible_hosts_yaml(configurations[0]["sshUser"], configurations,
                                                            cluster_id)),
        (aRP.SITE_CONFIG_FILE, generate_site_file_yaml(ansible_roles))]:
        write_yaml(path, generated_yaml, alias)

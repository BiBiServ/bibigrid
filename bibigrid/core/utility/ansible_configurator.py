"""
Prepares ansible files (vars, common_configuration, ...)
"""

import logging
import os

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
vpngtw_ROLES = [{"role": "bibigrid", "tags": ["bibigrid", "bibigrid-vpngtw"]}]
MASTER_ROLES = [{"role": "bibigrid", "tags": ["bibigrid", "bibigrid-master"]}]
WORKER_ROLES = [{"role": "bibigrid", "tags": ["bibigrid", "bibigrid-worker"]}]
VARS_FILES = [aRP.CONFIG_YML, aRP.HOSTS_YML]
IDE_CONF = {"ide": False, "workspace": ide.DEFAULT_IDE_WORKSPACE, "port_start": ide.REMOTE_BIND_ADDRESS,
            "port_end": ide.DEFAULT_IDE_PORT_END, "build": False}
ZABBIX_CONF = {"db": "zabbix", "db_user": "zabbix", "db_password": "zabbix", "timezone": "Europe/Berlin",
               "server_name": "bibigrid", "admin_password": "bibigrid"}
SLURM_CONF = {"db": "slurm", "db_user": "slurm", "db_password": "changeme",
              "munge_key": id_generation.generate_munge_key(),
              "elastic_scheduling": {"SuspendTime": 3600, "ResumeTimeout": 900, "TreeWidth": 128}}
LOG = logging.getLogger("bibigrid")


def delete_old_vars():
    """
    Deletes host_vars and group_vars
    @return:
    """
    for folder in [aRP.GROUP_VARS_FOLDER, aRP.HOST_VARS_FOLDER]:
        for file_name in os.listdir(folder):
            # construct full file path
            file = os.path.join(folder, file_name)
            if os.path.isfile(file):
                logging.debug('Deleting file: %s', file)
                os.remove(file)


def generate_site_file_yaml(custom_roles):
    """
    Generates site_yaml (dict).
    Deepcopy is used in case roles might differ between servers in the future.
    :param custom_roles: ansibleRoles given by the config
    :return: site_yaml (dict)
    """
    site_yaml = [{'hosts': 'master', "become": "yes", "vars_files": VARS_FILES, "roles": MASTER_ROLES},
                 {'hosts': 'vpngtw', "become": "yes", "vars_files": VARS_FILES, "roles": vpngtw_ROLES},
                 {"hosts": "workers", "become": "yes", "vars_files": VARS_FILES, "roles": WORKER_ROLES}]  # ,
    # {"hosts": "vpngtw", "become": "yes", "vars_files": copy.deepcopy(VARS_FILES),
    # "roles": ["common", "vpngtw"]}]
    # add custom roles and vars
    for custom_role in custom_roles:
        VARS_FILES.append(custom_role["vars_file"])
        for role_group in [MASTER_ROLES, vpngtw_ROLES, WORKER_ROLES]:
            role_group.append(ADDITIONAL_PATH + custom_role["name"])
    return site_yaml


def write_host_and_group_vars(configurations, providers, cluster_id):  # pylint: disable=too-many-locals
    """
    ToDo filter what information really is necessary. Determined by further development
    Filters unnecessary information
    :param configurations: configurations
    :param providers: providers
    :param cluster_id: To get proper naming
    :return: filtered information (dict)
    """
    LOG.info("Generating instances file...")
    flavor_keys = ["name", "ram", "vcpus", "disk", "ephemeral"]
    worker_count = 0
    vpn_count = 0
    for configuration, provider in zip(configurations, providers):
        configuration_features = configuration.get("features", [])
        if isinstance(configuration_features, str):
            configuration_features = [configuration_features]
        for worker in configuration.get("workerInstances", []):
            flavor = provider.get_flavor(worker["type"])
            flavor_dict = {key: flavor[key] for key in flavor_keys}
            name = create.WORKER_IDENTIFIER(cluster_id=cluster_id,
                                            additional=f"[{worker_count}-{worker_count + worker.get('count', 1) - 1}]")
            group_name = name.replace("[", "").replace("]", "").replace(":", "_").replace("-", "_")
            worker_count += worker.get('count', 1)
            regexp = create.WORKER_IDENTIFIER(cluster_id=cluster_id, additional=r"\d+")
            worker_dict = {"name": name, "regexp": regexp, "image": worker["image"],
                           "network": configuration["network"], "flavor": flavor_dict,
                           "gateway_ip": configuration["private_v4"],
                           "cloud_identifier": configuration["cloud_identifier"]}

            worker_features = worker.get("features", [])
            if isinstance(worker_features, str):
                worker_features = [worker_features]
            features = set(configuration_features+worker_features)
            if features:
                worker_dict["features"] = features
            write_yaml(os.path.join(aRP.GROUP_VARS_FOLDER, group_name), worker_dict)
        vpngtw = configuration.get("vpnInstance")
        if vpngtw:
            name = create.VPN_WORKER_IDENTIFIER(cluster_id=cluster_id, additional=f"{vpn_count}")
            wireguard_ip = f"10.0.0.{vpn_count + 2}"  # skipping 0 and 1 (master)
            vpn_count += 1
            flavor = provider.get_flavor(vpngtw["type"])
            flavor_dict = {key: flavor[key] for key in flavor_keys}
            regexp = create.WORKER_IDENTIFIER(cluster_id=cluster_id, additional=r"\d+")
            vpngtw_dict = {"name": name, "regexp": regexp, "image": vpngtw["image"],
                           "network": configuration["network"],
                           "network_cidr": configuration["subnet_cidrs"],
                           "floating_ip": configuration["floating_ip"],
                           "private_v4": configuration["private_v4"],
                           "flavor": flavor_dict,
                           "wireguard_ip": wireguard_ip,
                           "cloud_identifier": configuration[
                               "cloud_identifier"]}
            if configuration.get("wireguard_peer"):
                vpngtw_dict["wireguard"] = {"ip": wireguard_ip,
                                            "peer": configuration.get(
                                                "wireguard_peer")}
            write_yaml(os.path.join(aRP.HOST_VARS_FOLDER, name), vpngtw_dict)
        else:
            master = configuration["masterInstance"]
            name = create.MASTER_IDENTIFIER(cluster_id=cluster_id)
            flavor = provider.get_flavor(master["type"])
            flavor_dict = {key: flavor[key] for key in flavor_keys}
            master_dict = {"name": name, "image": master["image"], "network": configuration["network"],
                           "network_cidr": configuration["subnet_cidrs"],
                           "floating_ip": configuration["floating_ip"], "flavor": flavor_dict,
                           "private_v4": configuration["private_v4"],
                           "cloud_identifier": configuration["cloud_identifier"],
                           "volumes": configuration["volumes"]}
            if configuration.get("wireguard_peer"):
                master_dict["wireguard"] = {"ip": "10.0.0.1", "peer": configuration.get("wireguard_peer")}
            write_yaml(os.path.join(aRP.GROUP_VARS_FOLDER, "master.yml"), master_dict)


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
    :param cluster_id: id of cluster
    :param ssh_user: user for ssh connections
    :param default_user: Given default user
    :return: common_configuration_yaml (dict)
    """
    master_configuration = configurations[0]
    LOG.info("Generating common configuration file...")
    # print(configuration.get("slurmConf", {}))
    common_configuration_yaml = {"cluster_id": cluster_id, "cluster_cidrs": cidrs, "default_user": default_user,
                                 "local_fs": master_configuration.get("localFS", False),
                                 "local_dns_lookup": master_configuration.get("localDNSlookup", False),
                                 "use_master_as_compute": master_configuration.get("useMasterAsCompute", True),
                                 "dns_server_list": master_configuration.get("dns_server_list", ["8.8.8.8"]),
                                 "enable_slurm": master_configuration.get("slurm", False),
                                 "enable_zabbix": master_configuration.get("zabbix", False),
                                 "enable_nfs": master_configuration.get("nfs", False),
                                 "enable_ide": master_configuration.get("ide", False),
                                 "slurm": master_configuration.get("slurm", True), "ssh_user": ssh_user,
                                 "slurm_conf": mergedeep.merge({}, SLURM_CONF,
                                                               master_configuration.get("slurmConf", {}),
                                                               strategy=mergedeep.Strategy.TYPESAFE_REPLACE)}
    if master_configuration.get("nfs"):
        nfs_shares = master_configuration.get("nfsShares", [])
        nfs_shares = nfs_shares + DEFAULT_NFS_SHARES
        common_configuration_yaml["nfs_mounts"] = [{"src": "/" + nfs_share, "dst": "/" + nfs_share} for nfs_share in
                                                   nfs_shares]
        common_configuration_yaml["ext_nfs_mounts"] = [{"src": ext_nfs_share, "dst": ext_nfs_share} for ext_nfs_share in
                                                       (master_configuration.get("extNfsShares", []))]

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
        peers = configuration_handler.get_list_by_key(configurations, "wireguard_peer")
        common_configuration_yaml["wireguard_common"] = {"mask_bits": 24, "listen_port": 51820, "peers": peers}

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
    ansible_hosts_yaml = {"vpn": {"hosts": {},
                                  "children": {"master": {"hosts": {"localhost": to_instance_host_dict(ssh_user)}},
                                               "vpngtw": {"hosts": {}}}}, "workers": {"hosts": {}, "children": {}}}
    # vpngtw are handled like workers on this level
    workers = ansible_hosts_yaml["workers"]
    vpngtws = ansible_hosts_yaml["vpn"]["children"]["vpngtw"]["hosts"]
    worker_count = 0
    vpngtw_count = 0
    for configuration in configurations:
        for worker in configuration.get("workerInstances", []):
            name = create.WORKER_IDENTIFIER(cluster_id=cluster_id,
                                            additional=f"[{worker_count}:{worker_count + worker.get('count', 1) - 1}]")
            worker_dict = to_instance_host_dict(ssh_user, ip="")
            group_name = name.replace("[", "").replace("]", "").replace(":", "_").replace("-", "_")
            # if not workers["children"].get(group_name): # in the current setup this is not needed
            workers["children"][group_name] = {"hosts": {}}
            workers["children"][group_name]["hosts"][name] = worker_dict
            worker_count += worker.get('count', 1)

        if configuration.get("vpnInstance"):
            name = create.VPN_WORKER_IDENTIFIER(cluster_id=cluster_id, additional=vpngtw_count)
            vpngtw_dict = to_instance_host_dict(ssh_user, ip="")
            vpngtw_dict["ansible_host"] = configuration["floating_ip"]
            vpngtws[name] = vpngtw_dict
            vpngtw_count += 1
    return ansible_hosts_yaml


def to_instance_host_dict(ssh_user, ip="localhost"):  # pylint: disable=invalid-name
    """
    Generates host entry
    :param ssh_user: str global SSH-username
    :param ip: str ip
    :return: host entry (dict)
    """
    host_yaml = {"ansible_connection": "ssh", "ansible_python_interpreter": PYTHON_INTERPRETER,
                 "ansible_user": ssh_user}
    if ip:
        host_yaml["ip"] = ip
    return host_yaml


def get_cidrs(configurations):
    """
    Gets cidrs of all subnets in all providers
    :param configurations: list of configurations (dict)
    :return:
    """
    all_cidrs = []
    for configuration in configurations:
        subnet = configuration["subnet_cidrs"]
        provider_cidrs = {"cloud_identifier": configuration["cloud_identifier"], "provider_cidrs": subnet}
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
            worker_specification_yaml.append(
                {"TYPE": worker_group["type"], "IMAGE": worker_group["image"], "NETWORK": network})
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


def add_wireguard_peers(configurations):
    """
    Adds wireguard_peer information to configuration
    @param configurations:
    @return:
    """
    if len(configurations) > 1:
        for configuration in configurations:
            private_key, public_key = wireguard_keys.generate()
            configuration["wireguard_peer"] = {"name": configuration["cloud_identifier"], "private_key": private_key,
                                               "public_key": public_key, "ip": configuration["floating_ip"],
                                               "subnet": configuration["subnet_cidrs"]}


def configure_ansible_yaml(providers, configurations, cluster_id):
    """
    Generates and writes all ansible-configuration-yaml files.
    :param providers: list of providers
    :param configurations: list of configurations (dict)
    :param cluster_id: id of cluster to create
    :return:
    """
    delete_old_vars()
    LOG.info("Writing ansible files...")
    alias = configurations[0].get("aliasDumper", False)
    ansible_roles = get_ansible_roles(configurations[0].get("ansibleRoles"))
    default_user = providers[0].cloud_specification["auth"].get("username", configurations[0].get("sshUser", "Ubuntu"))
    add_wireguard_peers(configurations)
    for path, generated_yaml in [
        (aRP.WORKER_SPECIFICATION_FILE, generate_worker_specification_file_yaml(configurations)), (
                aRP.COMMONS_CONFIG_FILE,
                generate_common_configuration_yaml(cidrs=get_cidrs(configurations), configurations=configurations,
                                                   cluster_id=cluster_id, ssh_user=configurations[0]["sshUser"],
                                                   default_user=default_user)),
        (aRP.HOSTS_CONFIG_FILE, generate_ansible_hosts_yaml(configurations[0]["sshUser"], configurations, cluster_id)),
        (aRP.SITE_CONFIG_FILE, generate_site_file_yaml(ansible_roles))]:
        write_yaml(path, generated_yaml, alias)
    write_host_and_group_vars(configurations, providers, cluster_id)  # writing included in method

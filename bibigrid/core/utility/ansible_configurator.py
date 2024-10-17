"""
Prepares ansible files (vars, common_configuration, ...)
"""

import os

import mergedeep
import yaml

from bibigrid.core.actions import create
from bibigrid.core.actions import ide
from bibigrid.core.actions.version import __version__
from bibigrid.core.utility import id_generation
from bibigrid.core.utility import yaml_dumper
from bibigrid.core.utility.handler import configuration_handler
from bibigrid.core.utility.paths import ansible_resources_path as aRP
from bibigrid.core.utility.wireguard import wireguard_keys

DEFAULT_NFS_SHARES = ["/vol/spool"]
PYTHON_INTERPRETER = "/usr/bin/python3"
VPNGTW_ROLES = [{"role": "bibigrid", "tags": ["bibigrid", "bibigrid-vpngtw"]}]
MASTER_ROLES = [{"role": "bibigrid", "tags": ["bibigrid", "bibigrid-master"]}]
WORKER_ROLES = [{"role": "bibigrid", "tags": ["bibigrid", "bibigrid-worker"]}]
VARS_FILES = [aRP.CONFIG_YAML, aRP.HOSTS_YAML]
IDE_CONF = {"ide": False, "workspace": ide.DEFAULT_IDE_WORKSPACE, "port_start": ide.REMOTE_BIND_ADDRESS,
            "port_end": ide.DEFAULT_IDE_PORT_END, "build": False}
ZABBIX_CONF = {"db": "zabbix", "db_user": "zabbix", "db_password": "zabbix", "timezone": "Europe/Berlin",
               "server_name": "bibigrid", "admin_password": "bibigrid"}
SLURM_CONF = {"db": "slurm", "db_user": "slurm", "db_password": "changeme",
              "munge_key": id_generation.generate_munge_key(),
              "elastic_scheduling": {"SuspendTime": 3600, "ResumeTimeout": 1200, "SuspendTimeout": 60,
                                     "TreeWidth": 128}}
CLOUD_SCHEDULING = {"sshTimeout": 5}


def delete_old_vars(log):
    """
    Deletes host_vars and group_vars
    @param log:
    @return:
    """
    for folder in [aRP.GROUP_VARS_FOLDER, aRP.HOST_VARS_FOLDER]:
        for file_name in os.listdir(folder):
            # construct full file path
            file = os.path.join(folder, file_name)
            if os.path.isfile(file):
                log.debug('Deleting file: %s', file)
                os.remove(file)


def generate_site_file_yaml(user_roles):
    """
    Generates site_yaml (dict).
    Deepcopy is used in case roles might differ between servers in the future.
    @param user_roles: userRoles given by the config
    @return: site_yaml (dict)
    """
    site_yaml = [{'hosts': 'master', "become": "yes", "vars_files": VARS_FILES, "roles": MASTER_ROLES},
                 {'hosts': 'vpngtw', "become": "yes", "vars_files": VARS_FILES, "roles": VPNGTW_ROLES},
                 {"hosts": "workers", "become": "yes", "vars_files": VARS_FILES, "roles": WORKER_ROLES}]
    for user_role in user_roles:
        for host_dict in site_yaml:
            if host_dict["hosts"] in user_role["hosts"]:
                host_dict["vars_files"] = host_dict["vars_files"] + user_role.get("varsFiles", [])
                host_dict["roles"] = host_dict["roles"] + [{"role": role["name"], "tags": role.get("tags", [])} for role
                                                           in user_role["roles"]]

    return site_yaml


def write_host_and_group_vars(configurations, providers, cluster_id, log):  # pylint: disable=too-many-locals
    """
    Filters unnecessary information
    @param log:
    @param configurations: configurations
    @param providers: providers
    @param cluster_id: To get proper naming
    @return: filtered information (dict)
    """
    log.info("Generating instances file...")
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
                           "cloud_identifier": configuration["cloud_identifier"],
                           "on_demand": worker.get("onDemand", True), "state": "CLOUD",
                           "partitions": worker.get("partitions", []) + ["all", configuration["cloud_identifier"]],
                           }

            worker_features = worker.get("features", [])
            if isinstance(worker_features, str):
                worker_features = [worker_features]
            features = set(configuration_features + worker_features)
            if features:
                worker_dict["features"] = features

            pass_through(configuration, worker_dict, "waitForServices", "wait_for_services")
            write_yaml(os.path.join(aRP.GROUP_VARS_FOLDER, f"{group_name}.yaml"), worker_dict, log)
            for worker_number in range(worker.get('count', 1)):
                name = create.WORKER_IDENTIFIER(cluster_id=cluster_id, additional=worker_number)
                write_yaml(os.path.join(aRP.HOST_VARS_FOLDER, f"{name}.yaml"), {"volumes": worker.get("attachVolumes", [])}, log)
        vpngtw = configuration.get("vpnInstance")
        if vpngtw:
            name = create.VPN_WORKER_IDENTIFIER(cluster_id=cluster_id, additional=f"{vpn_count}")
            wireguard_ip = f"10.0.0.{vpn_count + 2}"  # skipping 0 and 1 (master)
            vpn_count += 1
            flavor = provider.get_flavor(vpngtw["type"])
            flavor_dict = {key: flavor[key] for key in flavor_keys}
            regexp = create.WORKER_IDENTIFIER(cluster_id=cluster_id, additional=r"\d+")
            vpngtw_dict = {"name": name, "regexp": regexp, "image": vpngtw["image"],
                           "network": configuration["network"], "network_cidrs": configuration["subnet_cidrs"],
                           "floating_ip": configuration["floating_ip"], "private_v4": configuration["private_v4"],
                           "flavor": flavor_dict, "wireguard_ip": wireguard_ip,
                           "cloud_identifier": configuration["cloud_identifier"],
                           "fallback_on_other_image": configuration.get("fallbackOnOtherImage", False),
                           "on_demand": False}
            if configuration.get("wireguard_peer"):
                vpngtw_dict["wireguard"] = {"ip": wireguard_ip, "peer": configuration.get("wireguard_peer")}
            pass_through(configuration, vpngtw_dict, "waitForServices", "wait_for_services")
            write_yaml(os.path.join(aRP.HOST_VARS_FOLDER, f"{name}.yaml"), vpngtw_dict, log)
        else:
            master = configuration["masterInstance"]
            name = create.MASTER_IDENTIFIER(cluster_id=cluster_id)
            flavor = provider.get_flavor(master["type"])
            flavor_dict = {key: flavor[key] for key in flavor_keys}
            master_dict = {"name": name, "image": master["image"], "network": configuration["network"],
                           "network_cidrs": configuration["subnet_cidrs"], "floating_ip": configuration["floating_ip"],
                           "flavor": flavor_dict, "private_v4": configuration["private_v4"],
                           "cloud_identifier": configuration["cloud_identifier"],
                           "volumes": configuration["masterInstance"]["attachVolumes"],
                           "fallback_on_other_image": configuration.get("fallbackOnOtherImage", False),
                           "state": "UNKNOWN" if configuration.get("useMasterAsCompute", True) else "DRAINED",
                           "on_demand": False,
                           "partitions": master.get("partitions", []) + ["all", configuration["cloud_identifier"]]}
            if configuration.get("wireguard_peer"):
                master_dict["wireguard"] = {"ip": "10.0.0.1", "peer": configuration.get("wireguard_peer")}
            pass_through(configuration, master_dict, "waitForServices", "wait_for_services")
            write_yaml(os.path.join(aRP.GROUP_VARS_FOLDER, "master.yaml"), master_dict, log)


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


def generate_common_configuration_yaml(cidrs, configurations, cluster_id, ssh_user, default_user, log):
    """
    Generates common_configuration yaml (dict)
    @param cidrs: str subnet cidrs (provider generated)
    @param configurations: master configuration (first in file)
    @param cluster_id: id of cluster
    @param ssh_user: user for ssh connections
    @param default_user: Given default user
    @param log:
    @return: common_configuration_yaml (dict)
    """
    master_configuration = configurations[0]
    log.info("Generating common configuration file...")
    common_configuration_yaml = {"bibigrid_version": __version__, "cluster_id": cluster_id, "cluster_cidrs": cidrs,
                                 "default_user": default_user, "local_fs": master_configuration.get("localFS", False),
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
                                                               strategy=mergedeep.Strategy.TYPESAFE_REPLACE),
                                 "cloud_scheduling": mergedeep.merge({}, CLOUD_SCHEDULING,
                                                                     master_configuration.get("cloudScheduling", {}),
                                                                     strategy=mergedeep.Strategy.TYPESAFE_REPLACE)}
    if master_configuration.get("nfs"):
        nfs_shares = master_configuration.get("nfsShares", [])
        nfs_shares = nfs_shares + DEFAULT_NFS_SHARES
        common_configuration_yaml["nfs_mounts"] = [{"src": nfs_share, "dst": nfs_share} for nfs_share in nfs_shares]
        common_configuration_yaml["ext_nfs_mounts"] = [{"src": ext_nfs_share, "dst": ext_nfs_share} for ext_nfs_share in
                                                       (master_configuration.get("extNfsShares", []))]

    if master_configuration.get("ide"):
        common_configuration_yaml["ide_conf"] = mergedeep.merge({}, IDE_CONF, master_configuration.get("ideConf", {}),
                                                                strategy=mergedeep.Strategy.TYPESAFE_REPLACE)
    if master_configuration.get("zabbix"):
        common_configuration_yaml["zabbix_conf"] = mergedeep.merge({}, ZABBIX_CONF,
                                                                   master_configuration.get("zabbixConf", {}),
                                                                   strategy=mergedeep.Strategy.TYPESAFE_REPLACE)

    if len(configurations) > 1:
        peers = configuration_handler.get_list_by_key(configurations, "wireguard_peer")
        common_configuration_yaml["wireguard_common"] = {"mask_bits": 24, "listen_port": 51820, "peers": peers}

    return common_configuration_yaml


def generate_ansible_hosts_yaml(ssh_user, configurations, cluster_id, log):  # pylint: disable-msg=too-many-locals
    """
    Generates ansible_hosts_yaml (inventory file).
    @param ssh_user: str global SSH-username
    @param configurations: dict
    @param cluster_id: id of cluster
    @param log:
    @return: ansible_hosts yaml (dict)
    """
    log.info("Generating ansible hosts file...")
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
    @param ssh_user: str global SSH-username
    @param ip: str ip
    @return: host entry (dict)
    """
    host_yaml = {"ansible_connection": "ssh", "ansible_python_interpreter": PYTHON_INTERPRETER,
                 "ansible_user": ssh_user}
    if ip:
        host_yaml["ip"] = ip
    return host_yaml


def get_cidrs(configurations):
    """
    Gets cidrs of all subnets in all providers
    @param configurations: list of configurations (dict)
    @return:
    """
    all_cidrs = []
    for configuration in configurations:
        provider_cidrs = {"cloud_identifier": configuration["cloud_identifier"],
                          "provider_cidrs": configuration["subnet_cidrs"]}
        all_cidrs.append(provider_cidrs)
    return all_cidrs


def get_ansible_galaxy_roles(ansible_galaxy_roles, log):
    """
    Checks if ansible_galaxy_role have all necessary values and adds it to the return list if so.
    @param ansible_galaxy_roles:
    @param log:
    @return: list of valid ansible_galaxy_roles
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
            log.warning("Galaxy role %s had neither galaxy,git nor url. Not added.", ansible_galaxy_role)
    return ansible_galaxy_roles_yaml


def generate_worker_specification_file_yaml(configurations, log):
    """
    Generates worker_specification_file_yaml
    @param configurations: list of configurations (dict)
    @param log:
    @return: worker_specification_yaml
    """
    log.info("Generating worker specification file...")
    worker_groups_list = configuration_handler.get_list_by_key(configurations, "workerInstances", False)
    # create.prepare_configuration guarantees that key is set
    network_list = configuration_handler.get_list_by_key(configurations, "network", False)
    worker_specification_yaml = []
    for worker_groups_provider_list, network in zip(worker_groups_list, network_list):
        for worker_group in worker_groups_provider_list:
            worker_specification_yaml.append(
                {"TYPE": worker_group["type"], "IMAGE": worker_group["image"], "NETWORK": network})
    return worker_specification_yaml


def write_yaml(path, generated_yaml, log, alias=False):
    """
    Writes generated_yaml to file path with or without alias
    @param path:
    @param generated_yaml:
    @param log:
    @param alias:
    @return:
    """
    log.debug("Writing yaml %s", path)
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
                                               "subnets": configuration["subnet_cidrs"]}


def configure_ansible_yaml(providers, configurations, cluster_id, log):
    """
    Generates and writes all ansible-configuration-yaml files.
    @param providers: list of providers
    @param configurations: list of configurations (dict)
    @param cluster_id: id of cluster to create
    @param log:
    @return:
    """
    delete_old_vars(log)
    log.info("Writing ansible files...")
    alias = configurations[0].get("aliasDumper", False)
    user_roles = configurations[0].get("userRoles", [])
    default_user = providers[0].cloud_specification["auth"].get("username", configurations[0].get("sshUser", "Ubuntu"))
    add_wireguard_peers(configurations)
    for path, generated_yaml in [
        (aRP.WORKER_SPECIFICATION_FILE, generate_worker_specification_file_yaml(configurations, log)), (
                aRP.COMMONS_CONFIG_FILE,
                generate_common_configuration_yaml(cidrs=get_cidrs(configurations), configurations=configurations,
                                                   cluster_id=cluster_id, ssh_user=configurations[0]["sshUser"],
                                                   default_user=default_user, log=log)), (aRP.HOSTS_CONFIG_FILE,
                                                                                          generate_ansible_hosts_yaml(
                                                                                              configurations[0][
                                                                                                  "sshUser"],
                                                                                              configurations,
                                                                                              cluster_id, log)),
        (aRP.SITE_CONFIG_FILE, generate_site_file_yaml(user_roles))]:
        write_yaml(path, generated_yaml, log, alias)
    write_host_and_group_vars(configurations, providers, cluster_id, log)  # writing included in method

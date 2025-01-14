"""
This module contains methods to list all clusters or a specific cluster in a formatted, readable output.
This includes a method to create a dictionary containing all running clusters and their servers.
"""

import pprint
import re

from bibigrid.core.utility.statics.create_statics import MASTER_IDENTIFIER

SERVER_REGEX = re.compile(r"^bibigrid-((master)-([a-zA-Z0-9]+)|(worker|vpngtw)-([a-zA-Z0-9]+)-\d+)$")


def dict_clusters(providers, log):
    """
    Creates a dictionary containing all servers by type and provider information
    @param providers: list of all providers
    @param log:
    @return: list of all clusters in yaml format
    """
    log.info("Creating cluster dictionary...")
    cluster_dict = {}
    for provider in providers:
        servers = provider.list_servers()
        for server in servers:
            result = SERVER_REGEX.match(server["name"])
            if result:
                identifier = result.group(4) or result.group(2)
                cluster_id = result.group(5) or result.group(3)
                setup(cluster_dict, cluster_id, server, provider)
                if identifier == "master":
                    cluster_dict[cluster_id][identifier] = server
                else:
                    cluster_dict[cluster_id][identifier + "s"].append(server)
    return cluster_dict  # recursively converts munches in cluster_dict to dict


def setup(cluster_dict, cluster_id, server, provider):
    """
    Determines cluster_id.
    Generates empty entry for cluster_id in cluster_dict.
    @param server: found server (dict)
    @param cluster_id: id of said cluster
    @param cluster_dict: dict containing all found servers by their cluster_id
    @param provider: server's provider
    @return: cluster_id
    """
    if not cluster_dict.get(cluster_id):
        cluster_dict[cluster_id] = {}
        cluster_dict[cluster_id]["workers"] = []
        cluster_dict[cluster_id]["vpngtws"] = []
    server["provider"] = provider.NAME
    server["cloud_specification"] = provider.cloud_specification["identifier"]


def log_list(cluster_id, providers, log):
    """
    Calls dict_clusters and gives a visual representation of the found cluster.
    Detail depends on whether a cluster_id is given or not.
    @param cluster_id:
    @param providers:
    @param log:
    @return:
    """
    cluster_dict = dict_clusters(providers=providers, log=log)
    if cluster_id:  # pylint: disable=too-many-nested-blocks
        if cluster_dict.get(cluster_id):
            log.info("Printing specific cluster_dictionary")
            master_count, worker_count, vpn_count = get_size_overview(cluster_dict[cluster_id], log)
            log.log(42, f"\tCluster has {master_count} master, {vpn_count} vpngtw and {worker_count} regular workers. "
                        f"The cluster is spread over {vpn_count + master_count} reachable provider(s).")
            log.log(42, pprint.pformat(cluster_dict[cluster_id]))
        else:
            log.info("Cluster with cluster-id {cluster_id} not found.")
            log.log(42, f"Cluster with cluster-id {cluster_id} not found.")
    else:
        log.info("Printing overview of cluster all clusters")
        if cluster_dict:
            for cluster_key_id, cluster_node_dict in cluster_dict.items():
                log.log(42, f"Cluster-ID: {cluster_key_id}")
                master = cluster_node_dict.get('master')
                if master:
                    for key in ["name", "user_id", "launched_at", "key_name", "public_v4", "public_v6", "provider"]:
                        value = cluster_node_dict['master'].get(key)
                        if value:
                            log.log(42, f"\t{key}: {value}")
                    security_groups = get_security_groups(cluster_node_dict)
                    log.log(42, f"\tsecurity_groups: {security_groups}")
                    networks = get_networks(cluster_node_dict)
                    log.log(42, f"\tnetwork: {pprint.pformat(networks)}")
                else:
                    log.warning("No master for cluster: %s.", cluster_key_id)
                master_count, worker_count, vpn_count = get_size_overview(cluster_node_dict, log)
                log.log(42,
                        f"\tCluster has {master_count} master, {vpn_count} vpngtw and {worker_count} regular workers. "
                        f"The cluster is spread over {vpn_count + master_count} reachable provider(s).")
        else:
            log.log(42, "No cluster found.")
    return 0


def get_size_overview(cluster_dict, log):
    """
    @param cluster_dict: dictionary of cluster to size_overview
    @param log:
    @return: number of masters, number of workers, number of vpns
    """
    log.info("Printing size overview")
    master_count = int(bool(cluster_dict.get("master")))
    worker_count = len(cluster_dict.get("workers") or "")
    vpn_count = len(cluster_dict.get("vpngtws") or "")
    return master_count, worker_count, vpn_count


def get_networks(cluster_dict):
    """
    Gets all addresses of servers
    @param cluster_dict: dictionary of clusters to find addresses
    @return: dict containing addresses
    """
    master = cluster_dict["master"]
    addresses = [{master["provider"]: list(master["addresses"].keys())}]
    for server in (cluster_dict.get("vpngtws") or []):
        addresses.append({server["provider"]: list(server["addresses"].keys())})
    return addresses


def get_security_groups(cluster_dict):
    """
    Gets all security group of servers
    @param cluster_dict: dictionary of clusters to find security_groups
    @return: dict containing security_groups
    """
    master = cluster_dict["master"]
    security_groups = [{master["provider"]: master["security_groups"]}]
    for server in (cluster_dict.get("vpngtws") or []):
        security_groups.append({server["provider"]: server["security_groups"]})
    return security_groups


def get_master_access_ip(cluster_id, master_provider, log):
    """
    Returns master's ip of cluster cluster_id
    @param master_provider: master's provider
    @param cluster_id: id of cluster
    @param log:
    @return: public ip of master
    """
    # TODO: maybe move the method from list_clusters as it is now independent of list_clusters
    log.info("Finding master ip for cluster %s...", cluster_id)
    master = MASTER_IDENTIFIER(cluster_id=cluster_id)
    server = master_provider.get_server(master)
    if server:
        return server.get("public_v4") or server.get("public_v6") or server.get("private_v4")
    log.warning("Cluster %s not found on master_provider %s.", cluster_id,
                master_provider.cloud_specification["identifier"])
    return None

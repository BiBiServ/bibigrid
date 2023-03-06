"""
This module contains methods to terminate a cluster. i.e. to delete all servers, keypairs (local and remote)
and application credentials used by it.
"""

import logging
import os
import re
import time

from bibigrid.core.actions import create

LOG = logging.getLogger("bibigrid")


def terminate_cluster(cluster_id, providers, debug=False):
    """
    Goes through all providers and gets info of all servers which name contains cluster ID.
    It then checks if any resources are reserved, but not used and frees them that were hold by the cluster.
    :param debug if set user gets asked before termination is executed
    :param providers providers
    :param cluster_id: ID of cluster to terminate
    :return: VOID
    """
    if debug:
        if not input(f"DEBUG MODE: Any non-empty input to shutdown cluster {cluster_id}. "
                     "Empty input to exit with cluster still alive:"):
            return 0
    cluster_server_state = []
    cluster_keypair_state = []
    cluster_security_group_state = []
    tmp_keyname = create.KEY_NAME.format(cluster_id=cluster_id)
    local_keypairs_deleted = delete_local_keypairs(tmp_keyname)
    if local_keypairs_deleted or input(f"WARNING: No local temporary keyfiles found for cluster {cluster_id}. "
                                       f"This might not be your cluster. Are you sure you want to terminate it?\n"
                                       f"Any non-empty input to shutdown cluster {cluster_id}. "
                                       f"Empty input to exit with cluster still alive:"):
        for provider in providers:
            LOG.info("Terminating cluster %s on cloud %s", cluster_id, provider.cloud_specification['identifier'])
            server_list = provider.list_servers()
            cluster_server_state += terminate_servers(server_list, cluster_id, provider)
            cluster_keypair_state.append(delete_keypairs(provider, tmp_keyname))
            cluster_keypair_state.append(delete_security_groups(provider, cluster_id))
        ac_state = delete_application_credentials(providers[0], cluster_id)
        terminate_output(cluster_server_state, cluster_keypair_state, cluster_security_group_state, ac_state,
                         cluster_id)
    return 0


def terminate_servers(server_list, cluster_id, provider):
    """
    Terminates all servers in server_list that match the bibigrid regex.
    @param server_list: list of server dicts. All servers are from provider
    @param cluster_id: id of cluster to terminate
    @param provider: provider that holds all servers in server_list
    @return: a list of the servers' (that were to be terminated) termination states
    """
    LOG.info("Deleting servers on provider %s...", provider.cloud_specification['identifier'])
    cluster_server_state = []
    # ^(master-{cluster_id}|worker-{cluster_id}|worker-[0-9]+-[0-9]+-{cluster_id})$
    server_regex = re.compile(fr"^bibigrid-(master-{cluster_id}+|(worker\d+|vpnwkr)-{cluster_id}+-\d+)$")
    for server in server_list:
        if server_regex.match(server["name"]):
            LOG.info("Trying to terminate Server %s on cloud %s.", server['name'],
                     provider.cloud_specification['identifier'])
            cluster_server_state.append(terminate_server(provider, server))
    return cluster_server_state


def terminate_server(provider, server):
    """
    Terminates a single server and stores the termination state
    @param provider: the provider that holds the server
    @param server: the server that is to be terminated
    @return: true if the server has been terminated, false else
    """
    terminated = provider.delete_server(server["id"])
    if not terminated:
        LOG.warning("Unable to terminate server %s on provider %s.", server['name'],
                    provider.cloud_specification['identifier'])
    else:
        LOG.info("Server %s terminated on provider %s.", server['name'], provider.cloud_specification['identifier'])
    return terminated


def delete_keypairs(provider, tmp_keyname):
    """
    Deletes keypairs from all provider
    @param provider: provider to delete keypair from
    @param tmp_keyname: BiBiGrid keyname
    @return: True if keypair was deleted
    """
    LOG.info("Deleting Keypair on provider %s...", provider.cloud_specification['identifier'])
    deleted = provider.delete_keypair(tmp_keyname)
    if deleted:
        LOG.info("Keypair %s deleted on provider %s.", tmp_keyname, provider.cloud_specification['identifier'])
    else:
        LOG.warning("Unable to delete %s on provider %s.", tmp_keyname, provider.cloud_specification['identifier'])
    return deleted


def delete_local_keypairs(tmp_keyname):
    """
    Deletes local keypairs of a cluster
    @param tmp_keyname: BiBiGrid keyname
    @return: Returns true if at least one local keyfile (pub or private) was found
    """
    success = False
    LOG.info("Deleting Keypair locally...")
    tmp_keypath = os.path.join(create.KEY_FOLDER, tmp_keyname)
    pub_tmp_keypath = tmp_keypath + ".pub"
    if os.path.isfile(tmp_keypath):
        os.remove(tmp_keypath)
        success = True
    else:
        LOG.warning(f"Unable to find private keyfile '{tmp_keypath}' locally. No local private keyfile deleted.")
    if os.path.isfile(pub_tmp_keypath):
        os.remove(pub_tmp_keypath)
        success = True
    else:
        LOG.warning(f"Unable to find public keyfile '{pub_tmp_keypath}' locally. No local public keyfile deleted.")
    return success


def delete_security_groups(provider, cluster_id):
    """
    Delete configured security groups from provider.

    :param provider: current cloud provider
    :param cluster_id:  cluster id
    :return: True if all configured security groups can be deleted, false otherwise
    """
    LOG.info("Deleting security groups on provider %s...", provider.cloud_specification['identifier'])
    success = True
    time.sleep(15)  # avoid in use bug
    for security_group_format in [create.DEFAULT_SECURITY_GROUP_NAME, create.WIREGUARD_SECURITY_GROUP_NAME]:
        security_group_name = security_group_format.format(cluster_id=cluster_id)
        tmp_success = provider.delete_security_group(security_group_name)
        LOG.info(f"Delete security_group {security_group_name} -> {tmp_success}")
        success = success and tmp_success
    return success


def delete_application_credentials(master_provider, cluster_id):
    """
    Deletes application credentials from the master_provider
    @param master_provider: provider that holds the master
    @param cluster_id:
    @return: True if no cluster credential remains on the provider. Else False.
    """
    # implement deletion
    auth = master_provider.cloud_specification["auth"]
    if not auth.get("application_credential_id") or not auth.get("application_credential_secret"):
        return master_provider.delete_application_credential_by_id_or_name(create.AC_NAME.format(cluster_id=cluster_id))
    LOG.info("Because you used application credentials to authenticate, "
             "no created application credentials need deletion.")
    return True


def terminate_output(cluster_server_state, cluster_keypair_state, cluster_security_group_state, ac_state, cluster_id):
    """
    Logs the termination result in detail
    @param cluster_server_state: list of bools. Each bool stands for a server termination
    @param cluster_keypair_state: list of bools. Each bool stands for a keypair deletion
    @param cluster_security_group_state: list of bools. Each bool stands for a security group deletion
    @param ac_state: bool that stands for the deletion of the credentials on the master
    @param cluster_id:
    @return:
    """
    cluster_existed = bool(cluster_server_state)
    cluster_server_terminated = all(cluster_server_state)
    cluster_keypair_deleted = all(cluster_keypair_state)
    cluster_security_group_deleted = all(cluster_security_group_state)
    if cluster_existed:
        if cluster_server_terminated:
            LOG.info("Terminated all servers of cluster %s.", cluster_id)
        else:
            LOG.warning("Unable to terminate all servers of cluster %s.", cluster_id)
        if cluster_keypair_deleted:
            LOG.info("Deleted all keypairs of cluster %s.", cluster_id)
        else:
            LOG.warning("Unable to delete all keypairs of cluster %s.", cluster_id)
        if cluster_keypair_deleted:
            LOG.info("Deleted all security groups of cluster %s.", cluster_id)
        else:
            LOG.warning("Unable to delete all security groups of cluster %s.", cluster_id)

        if cluster_server_terminated and cluster_keypair_deleted and cluster_security_group_deleted:
            out = f"Successfully terminated cluster {cluster_id}."
            LOG.info(out)
            print(out)
        else:
            LOG.warning("Unable to terminate cluster %s properly."
                        "\nAll servers terminated: %s"
                        "\nAll keys deleted: %s"
                        "\nAll security groups deleted: %s", cluster_id, cluster_server_terminated,
                        cluster_keypair_deleted, cluster_security_group_deleted)
        if ac_state:
            LOG.info("Successfully handled application credential of cluster %s.", cluster_id)
        else:
            LOG.warning("Unable to delete application credential of cluster %s", cluster_id)
    else:
        LOG.warning("Unable to find any servers for cluster-id %s. "
                    "Check cluster-id and configuration.\nAll keys deleted: %s", cluster_id, cluster_keypair_deleted)

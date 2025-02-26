"""
This module contains methods to terminate a cluster. i.e. to delete all servers, keypairs (local and remote)
and application credentials used by it.
"""

import os
import re
import time

import yaml

from bibigrid.core.utility.paths.basic_path import CLUSTER_INFO_FOLDER, CLUSTER_MEMORY_PATH, KEY_FOLDER
from bibigrid.core.utility.statics.create_statics import DEFAULT_SECURITY_GROUP_NAME, WIREGUARD_SECURITY_GROUP_NAME, \
    KEY_NAME, AC_NAME
from bibigrid.models.exceptions import ConflictException


def write_cluster_state(cluster_id, state):
    # last cluster
    with open(CLUSTER_MEMORY_PATH, mode="w+", encoding="UTF-8") as cluster_memory_file:
        yaml.safe_dump(data=state, stream=cluster_memory_file)
    # all clusters
    cluster_info_path = os.path.normpath(os.path.join(CLUSTER_INFO_FOLDER, f"{cluster_id}.yaml"))
    if not cluster_info_path.startswith(CLUSTER_INFO_FOLDER):
        raise ValueError("Invalid cluster_id resulting in path traversal")
    with open(cluster_info_path, mode="w+", encoding="UTF-8") as cluster_info_file:
        yaml.safe_dump(data=state, stream=cluster_info_file)


def terminate(cluster_id, providers, log, debug=False, assume_yes=False):
    """
    Goes through all providers and gets info of all servers which name contains cluster ID.
    It then checks if any resources are reserved, but not used and frees them that were hold by the cluster.
    @param debug if set user gets asked before termination is executed
    @param providers:
    @param log:
    @param cluster_id: ID of cluster to terminate
    @param assume_yes: if set, no input will be asked, but instead yes will be assumed
    @return VOID
    """
    if not assume_yes and debug:
        if not input(f"DEBUG MODE: Any non-empty input to shutdown cluster {cluster_id}. "
                     "Empty input to exit with cluster still alive:"):
            return 0
    security_groups = [DEFAULT_SECURITY_GROUP_NAME]
    if len(providers) > 1:
        security_groups.append(WIREGUARD_SECURITY_GROUP_NAME)
    cluster_server_state = []
    cluster_keypair_state = []
    cluster_security_group_state = []
    cluster_volume_state = []
    tmp_keyname = KEY_NAME.format(cluster_id=cluster_id)
    local_keypairs_deleted = delete_local_keypairs(tmp_keyname, log)
    if assume_yes or local_keypairs_deleted or input(
            f"WARNING: No local temporary keyfiles found for cluster {cluster_id}. "
            f"This might not be your cluster. Are you sure you want to terminate it?\n"
            f"Any non-empty input to shutdown cluster {cluster_id}. "
            f"Empty input to exit with cluster still alive:"):
        for provider in providers:
            log.info("Terminating cluster %s on cloud %s", cluster_id, provider.cloud_specification['identifier'])
            cluster_server_state += terminate_servers(cluster_id, provider, log)
            cluster_keypair_state.append(delete_keypairs(provider, tmp_keyname, log))
            cluster_security_group_state.append(delete_security_groups(provider, cluster_id, security_groups, log))
            cluster_volume_state.append(delete_non_permanent_volumes(provider, cluster_id, log))
        ac_state = delete_application_credentials(providers[0], cluster_id, log)
        terminate_output(cluster_server_state=cluster_server_state, cluster_keypair_state=cluster_keypair_state,
                         cluster_security_group_state=cluster_security_group_state,
                         cluster_volume_state=cluster_volume_state, ac_state=ac_state, cluster_id=cluster_id,
                         log=log)
    return 0


def terminate_servers(cluster_id, provider, log):
    """
    Terminates all servers that match the bibigrid regex.
    @param cluster_id: id of cluster to terminate
    @param provider: provider that holds all servers in server_list
    @param log:
    @return: a list of the servers' (that were to be terminated) termination states
    """
    log.info("Deleting servers on provider %s...", provider.cloud_specification['identifier'])
    server_list = provider.list_servers()
    cluster_server_state = []
    server_regex = re.compile(fr"^bibigrid-(master-{cluster_id}|(worker|vpngtw)-{cluster_id}-\d+)$")
    for server in server_list:
        if server_regex.match(server["name"]):
            log.info("Trying to terminate Server %s on cloud %s.", server['name'],
                     provider.cloud_specification['identifier'])
            cluster_server_state.append(terminate_server(provider, server, log))
    return cluster_server_state


def terminate_server(provider, server, log):
    """
    Terminates a single server and stores the termination state
    @param provider: the provider that holds the server.
    @param server: the server that is to be terminated
    @param log:
    @return: true if the server has been terminated, false else
    """
    terminated = provider.delete_server(server["id"])
    if not terminated:
        log.warning("Unable to terminate server %s on provider %s.", server['name'],
                    provider.cloud_specification['identifier'])
    else:
        log.info("Server %s terminated on provider %s.", server['name'], provider.cloud_specification['identifier'])
    return terminated


def delete_keypairs(provider, tmp_keyname, log):
    """
    Deletes keypairs from all provider
    @param provider: provider to delete keypair from
    @param tmp_keyname: BiBiGrid keyname
    @param log
    @return: True if keypair was deleted
    """
    log.info("Deleting Keypair on provider %s...", provider.cloud_specification['identifier'])
    deleted = provider.delete_keypair(tmp_keyname)
    if deleted:
        log.info("Keypair %s deleted on provider %s.", tmp_keyname, provider.cloud_specification['identifier'])
    else:
        log.warning("Unable to delete %s on provider %s.", tmp_keyname, provider.cloud_specification['identifier'])
    return deleted


def delete_local_keypairs(tmp_keyname, log):
    """
    Deletes local keypairs of a cluster
    @param tmp_keyname: BiBiGrid keyname
    @param log
    @return: Returns true if at least one local keyfile (pub or private) was found
    """
    success = False
    log.info("Deleting Keypair locally...")
    tmp_keypath = os.path.join(KEY_FOLDER, tmp_keyname)
    pub_tmp_keypath = tmp_keypath + ".pub"
    if os.path.isfile(tmp_keypath):
        os.remove(tmp_keypath)
        success = True
    else:
        log.warning(f"Unable to find private keyfile '{tmp_keypath}' locally. No local private keyfile deleted.")
    if os.path.isfile(pub_tmp_keypath):
        os.remove(pub_tmp_keypath)
        success = True
    else:
        log.warning(f"Unable to find public keyfile '{pub_tmp_keypath}' locally. No local public keyfile deleted.")
    return success


def delete_security_groups(provider, cluster_id, security_groups, log, timeout=5):
    """
    Delete configured security groups from provider.

    @param provider: current cloud provider
    @param cluster_id:  cluster id
    @param timeout: how often should delete be attempted
    @param security_groups: security groups that have been used
    @param log
    @return: True if all configured security groups can be deleted, false otherwise
    """
    log.info("Deleting security groups on provider %s...", provider.cloud_specification['identifier'])
    success = True
    for security_group_format in security_groups:
        security_group_name = security_group_format.format(cluster_id=cluster_id)
        attempts = 0
        tmp_success = not provider.get_security_group(security_group_name)
        while not tmp_success:
            try:
                tmp_success = provider.delete_security_group(security_group_name)
            except ConflictException:
                log.info(f"ConflictException on deletion attempt on {provider.cloud_specification['identifier']}.")
                tmp_success = False
            if tmp_success:
                break
            if attempts < timeout:
                attempts += 1
                time.sleep(1 + 2 ** attempts)
                log.info(f"Retrying to delete security group {security_group_name} on "
                         f"{provider.cloud_specification['identifier']}. Attempt {attempts}/{timeout}")
            else:
                log.error(f"Attempt to delete security group {security_group_name} on "
                          f"{provider.cloud_specification['identifier']} failed.")
                break
        log.info(f"Delete security_group {security_group_name} -> {tmp_success} on "
                 f"{provider.cloud_specification['identifier']}.")
        success = success and tmp_success
    return success


def delete_application_credentials(master_provider, cluster_id, log):
    """
    Deletes application credentials from the master_provider
    @param master_provider: provider that holds the master
    @param cluster_id:
    @param log:
    @return: True if no cluster credential remains on the provider. Else False.
    """
    # implement deletion
    auth = master_provider.cloud_specification["auth"]
    if not auth.get("application_credential_id") or not auth.get("application_credential_secret"):
        return master_provider.delete_application_credential_by_id_or_name(AC_NAME.format(cluster_id=cluster_id))
    log.info("Because you used application credentials to authenticate, "
             "no created application credentials need deletion.")
    return True


def delete_non_permanent_volumes(provider, cluster_id, log):
    """
    Terminates all temporary and semiperm volumes that match the regex.
    @param cluster_id: id of cluster to terminate
    @param provider: provider that holds all servers in server_list
    @param log:
    @return: a list of the servers' (that were to be terminated) termination states
    """
    log.info("Deleting non permanent volumes on provider %s...", provider.cloud_specification['identifier'])
    volume_list = provider.list_volumes()
    cluster_volume_state = []
    volume_regex = re.compile(
        fr"^bibigrid-(master-{cluster_id}|(worker|vpngtw)-{cluster_id}-(\d+))-(semiperm|tmp)-\d+(-.+)?$")
    for volume in volume_list:
        if volume_regex.match(volume["name"]):
            log.info("Trying to delete volume %s on cloud %s.", volume['name'], provider.cloud_specification[
                'identifier'])
            cluster_volume_state.append(provider.delete_volume(volume))
    return cluster_volume_state


# pylint: disable=too-many-branches
def terminate_output(*, cluster_server_state, cluster_keypair_state, cluster_security_group_state, cluster_volume_state,
                     ac_state, cluster_id, log):
    """
    Logs the termination result in detail
    @param cluster_server_state: list of bools. Each bool stands for a server termination
    @param cluster_keypair_state: list of bools. Each bool stands for a keypair deletion
    @param cluster_security_group_state: list of bools. Each bool stands for a security group deletion
    @param cluster_volume_state: list of bools. Each bool stands for a volume deletion
    @param ac_state: bool that stands for the deletion of the credentials on the master
    @param cluster_id:
    @param log:
    @return:
    """
    cluster_existed = bool(cluster_server_state)
    cluster_server_terminated = all(cluster_server_state)
    cluster_keypair_deleted = all(cluster_keypair_state)
    cluster_security_group_deleted = all(cluster_security_group_state)
    cluster_volume_deleted = all(all(instance_volume_states) for instance_volume_states in cluster_volume_state)
    success = (cluster_server_terminated and cluster_keypair_deleted and
               cluster_security_group_deleted and cluster_volume_deleted)
    # message = "Cluster terminated."
    state = "terminated"
    if cluster_existed:
        if cluster_server_terminated:
            log.info("Terminated all servers of cluster %s.", cluster_id)
        else:
            log.warning("Unable to terminate all servers of cluster %s.", cluster_id)
        if cluster_keypair_deleted:
            log.info("Deleted all keypairs of cluster %s.", cluster_id)
        else:
            log.warning("Unable to delete all keypairs of cluster %s.", cluster_id)
        if cluster_keypair_deleted:
            log.info("Deleted all security groups of cluster %s.", cluster_id)
        else:
            log.warning("Unable to delete all security groups of cluster %s.", cluster_id)
        if cluster_volume_deleted:
            log.info("Deleted all volumes of cluster %s", cluster_id)
        else:
            log.warning("Unable to delete all volumes of cluster %s.", cluster_id)
        if success:
            message = f"Successfully terminated cluster {cluster_id}."
            log.log(42, message)
        else:
            message = (f"Unable to terminate cluster {cluster_id} properly."
                       f"\nAll servers terminated: {cluster_server_terminated}"
                       f"\nAll keys deleted: {cluster_keypair_deleted}"
                       f"\nAll security groups deleted: {cluster_security_group_deleted}"
                       f"\nAll volumes deleted: {cluster_volume_deleted}")
            log.warning(message)
        if ac_state:
            log.info("Successfully handled application credential of cluster %s.", cluster_id)
        else:
            log.warning("Unable to delete application credential of cluster %s", cluster_id)

        write_cluster_state(cluster_id, {"cluster_id": cluster_id,
                                         "floating_ip": None,
                                         "ssh_user": None,
                                         "state": state,
                                         "message": message})
    else:
        log.warning(f"Unable to find any servers for cluster-id {cluster_id}. "
                    f"Check cluster-id and configuration.\nAll keys deleted: {cluster_keypair_deleted}")

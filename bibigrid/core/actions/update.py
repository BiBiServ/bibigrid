"""
Module that contains methods to update the master playbook
"""

from bibigrid.core.utility import ansible_commands as a_c
from bibigrid.core.utility.handler import ssh_handler
from bibigrid.core.utility.paths import ansible_resources_path as a_rp
from bibigrid.core.utility.paths import bin_path
from bibigrid.core.utility.handler import cluster_ssh_handler


def update(cluster_id, master_provider, master_configuration, log):
    log.info("Starting update...")
    master_ip, ssh_user, used_private_key = cluster_ssh_handler.get_ssh_connection_info(cluster_id, master_provider,
                                                                                        master_configuration, log)
    if master_ip and ssh_user and used_private_key:
        log.info("Trying to update %s@%s", master_ip, ssh_user)
        ssh_handler.execute_ssh(floating_ip=master_ip, private_key=used_private_key, username=ssh_user,
                                log=log,
                                gateway=master_configuration.get("gateway", {}),
                                commands=[a_c.EXECUTE],
                                filepaths=[(a_rp.PLAYBOOK_PATH, a_rp.PLAYBOOK_PATH_REMOTE),
                                           (bin_path.BIN_PATH, bin_path.BIN_PATH_REMOTE)])
        return 0
    return 1

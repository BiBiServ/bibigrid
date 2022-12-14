"""
Module that contains methods to update the master playbook
"""

import logging

from bibigrid2.core.utility import ansible_commands as aC
from bibigrid2.core.utility.handler import ssh_handler
from bibigrid2.core.utility.paths import ansible_resources_path as aRP
from bibigrid2.core.utility.paths import bin_path as biRP
from bibigrid2.core.utility.handler import cluster_ssh_handler

LOG = logging.getLogger("bibigrid")

def update(cluster_id, master_provider, master_configuration):
    LOG.info("Starting update...")
    master_ip, ssh_user, used_private_key = cluster_ssh_handler.get_ssh_connection_info(cluster_id, master_provider,
                                                                                        master_configuration)
    if master_ip and ssh_user and used_private_key:
        LOG.info("Trying to update %s@%s", master_ip, ssh_user)
        ssh_handler.execute_ssh(floating_ip=master_ip, private_key=used_private_key, username=ssh_user,
                                commands=[aC.EXECUTE],
                                filepaths=[(aRP.PLAYBOOK_PATH, aRP.PLAYBOOK_PATH_REMOTE),
                                           (biRP.BIN_PATH, biRP.BIN_PATH_REMOTE)])
        return 0

    return 1

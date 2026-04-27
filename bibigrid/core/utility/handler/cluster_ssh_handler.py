"""
This module gets information about ssh connection.
"""

import os
import sys

from bibigrid.core.actions import list_clusters
from bibigrid.core.utility.statics.create_statics import KEY_NAME
from bibigrid.core.utility.paths.basic_path import KEY_FOLDER


def get_ssh_connection_info(cluster_id, master_provider, master_configuration, log):
    """
    Gets master_ip, ssh_user and private key to enable other modules to create an ssh connection to a clusters master
    @param cluster_id: id of cluster to connect to
    @param master_provider: master's provider
    @param master_configuration: master's configuration
    @param log:
    @return: triple (master_ip, ssh_user, private_key)
    """
    # If cluster_id is an ip, cluster_id will be used for master_ip
    if "." in cluster_id:
        log.info("Interpreting %s as ip since it doesn't match cluster_id", cluster_id)
        master_ip = cluster_id
    else:
        master_ip = list_clusters.get_master_access_ip(cluster_id, master_provider, log)
    ssh_user = master_configuration.get("sshUser")
    public_keys = master_configuration.get("sshPublicKeyFiles") or []
    used_private_key = None

    # TODO needs solution for when key has a passphrase
    # first check configuration then if not found take the temporary key
    # for public_key in public_keys:
    #     if isinstance(public_key, str):
    #         private_key = public_key[:-4]
    #         if os.path.isfile(private_key):
    #             used_private_key = private_key
    #             break

    if not used_private_key:
        private_key = os.path.join(KEY_FOLDER, KEY_NAME.format(cluster_id=cluster_id))
        if os.path.isfile(private_key):
            used_private_key = private_key
        else:
            log.warning(f"Could not find private key for cluster {cluster_id}")
            sys.exit(1)
    return master_ip, ssh_user, used_private_key

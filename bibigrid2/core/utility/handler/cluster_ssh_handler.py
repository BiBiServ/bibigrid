"""
This module gets information about ssh connection.
"""

import logging
import os

from bibigrid2.core.actions import create, list_clusters

LOG = logging.getLogger("bibigrid")
def get_ssh_connection_info(cluster_id, master_provider, master_configuration):
    """
    Gets master_ip, ssh_user and private key to enable other modules to create an ssh connection to a clusters master
    @param cluster_id: id of cluster to connect to
    @param master_provider: master's provider
    @param master_configuration: master's configuration
    @return: triple (master_ip, ssh_user, private_key)
    """
    # If cluster_id is an ip, cluster_id will be used for master_ip
    if "." in cluster_id:
        LOG.info("Interpreting %s as ip since it doesn't match cluster_id", cluster_id)
        master_ip = cluster_id
    else:
        master_ip = list_clusters.get_master_access_ip(cluster_id, master_provider)
    ssh_user = master_configuration.get("sshUser")
    public_keys = master_configuration.get("sshPublicKeyFiles")
    used_private_key = None

    # first check configuration then if not found take the temporary key
    if public_keys:
        public_key = public_keys[0]
        if isinstance(public_key, str):
            private_key = public_key.strip(".pub")
            if os.path.isfile(private_key):
                used_private_key = private_key
    if not used_private_key:
        private_key = os.path.join(create.KEY_FOLDER, create.KEY_NAME.format(cluster_id=cluster_id))
        if os.path.isfile(private_key):
            used_private_key = private_key
    return master_ip, ssh_user, used_private_key

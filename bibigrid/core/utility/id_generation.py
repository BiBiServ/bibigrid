"""
Generates ids and munge keys
"""

import shortuuid

from bibigrid.core.utility.statics.create_statics import MASTER_IDENTIFIER, VPNGTW_IDENTIFIER, WORKER_IDENTIFIER

MAX_ID_LENGTH = 15
CLUSTER_UUID_ALPHABET = '0123456789abcdefghijklmnopqrstuvwxyz'


def generate_cluster_id():
    """
    Generates an encrypted shortUUID with length MAX_ID_LENGTH
    @return:
    """
    uuid = shortuuid.ShortUUID()
    uuid.set_alphabet(CLUSTER_UUID_ALPHABET)
    return uuid.random(MAX_ID_LENGTH)


def generate_safe_cluster_id(providers):
    """
    Generates a cluster_id and checks if cluster_id is not in use. When a unique id is found it is returned
    @param providers: providers to check whether they use said cluster_id
    @return: cluster_id
    """
    id_is_unique = False
    cluster_id = None
    while not id_is_unique:
        cluster_id = generate_cluster_id()
        id_is_unique = is_unique_cluster_id(cluster_id, providers)
    return cluster_id


def is_unique_cluster_id(cluster_id, providers):
    """
    Checks if cluster_id is not in use on any provider
    @param cluster_id: generated cluster_ird
    @param providers: providers to check
    @return: True if cluster_id is unique. False else.
    """
    for provider in providers:
        for server in provider.list_servers():
            master = MASTER_IDENTIFIER(cluster_id=cluster_id)
            vpngtw = VPNGTW_IDENTIFIER(cluster_id=cluster_id)
            worker = WORKER_IDENTIFIER(cluster_id=cluster_id)
            if server["name"] in [master, vpngtw, worker]:
                return False
    return True


def generate_munge_key():
    """
    Generates a munge key (UUID) for slurm
    @return:
    """
    return shortuuid.ShortUUID().random(32)

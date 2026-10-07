"""
Containg static variables for create.py to avoid cyclic imports
"""

from functools import partial

from bibigrid.core.utility.paths import ansible_resources_path as a_rp
from bibigrid.core.utility.paths import bin_path


def get_identifier(identifier, cluster_id, additional=""):
    """
    This method does more advanced string formatting to generate master, vpngtw and worker names
    @param identifier: master|vpngtw|worker
    @param cluster_id: id of cluster
    @param additional: an additional string to be added at the end
    @return: the generated string
    """
    general = PREFIX_WITH_SEP + identifier + SEPARATOR + cluster_id
    if additional or additional == 0:
        return general + SEPARATOR + str(additional)
    return general


PREFIX = "bibigrid"
SEPARATOR = "-"
PREFIX_WITH_SEP = PREFIX + SEPARATOR
UPLOAD_FILEPATHS = [(a_rp.PLAYBOOK_PATH, a_rp.PLAYBOOK_PATH_REMOTE), (bin_path.BIN_PATH, bin_path.BIN_PATH_REMOTE)]
MASTER_IDENTIFIER = partial(get_identifier, identifier="master", additional="")
WORKER_IDENTIFIER = partial(get_identifier, identifier="worker")
VPNGTW_IDENTIFIER = partial(get_identifier, identifier="vpngtw")

KEY_PREFIX = "tempKey_bibi"
AC_NAME = "ac" + SEPARATOR + "{cluster_id}"
KEY_NAME = KEY_PREFIX + SEPARATOR + "{cluster_id}"
DEFAULT_SECURITY_GROUP_NAME = "default" + SEPARATOR + "{cluster_id}"
WIREGUARD_SECURITY_GROUP_NAME = "wireguard" + SEPARATOR + "{cluster_id}"

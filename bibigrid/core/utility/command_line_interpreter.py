"""
Has necessary methods and variables to interpret the command line
"""

import argparse
import logging
import os

STANDARD_CONFIG_INPUT_PATH = os.path.expanduser("~/.config/bibigrid")
FOLDER_START = ("~/", "/")
LOG = logging.getLogger("bibigrid")


def check_cid(cid):
    if "-" in cid:
        new_cid = cid.split("-")[-1]
        LOG.info("-cid %s is not a cid, but probably the entire master name. Using '%s' as "
                    "cid instead.", cid, new_cid)
        return new_cid
    if "." in cid:
        LOG.info("-cid %s is not a cid, but probably the master's ip. "
                    "Using the master ip instead of cid only works if a cluster key is in your systems default ssh key "
                    "location (~/.ssh/). Otherwise bibigrid can't identify the cluster key.")
    return cid


def interpret_command_line():
    """
    Interprets commandline. Used in startup.py
    :return:
    """
    parser = argparse.ArgumentParser(description='Bibigrid sets up cluster easily inside a cloud environment')
    parser.add_argument("-v", "--verbose", action="count", default=0,
                        help="Increases logging verbosity. `-v` adds more info to the logfile, "
                             "`-vv` adds debug information to the logfile.")
    parser.add_argument("-d", "--debug", action='store_true', help="Keeps cluster active. Asks before shutdown. "
                                                                   "Offers termination after create")
    parser.add_argument("-i", "--config_input", metavar="<path>", help="Path to YAML configurations file. "
                                                                       "Relative paths can be used and start "
                                                                       "at ~/.config/bibigrid", required=True,
                        type=lambda s: s if s.startswith(FOLDER_START) else os.path.join(STANDARD_CONFIG_INPUT_PATH, s))
    parser.add_argument("-cid", "--cluster_id", metavar="<cluster-id>", type=check_cid, default="",
                        help="Cluster id is needed for ide and termination")

    actions = parser.add_mutually_exclusive_group(required=True)
    actions.add_argument("-V", "--version", action='store_true', help="Displays version")
    actions.add_argument("-t", "--terminate", action='store_true',
                         help="Terminates cluster. Needs cluster-id set.")
    actions.add_argument("-c", "--create", action='store_true', help="Creates cluster")
    actions.add_argument("-l", "--list", action='store_true',
                         help="Lists all running clusters. If cluster-id is set, will list this cluster in detail only")
    actions.add_argument("-ch", "--check", action='store_true', help="Validates cluster configuration")
    actions.add_argument("-ide", "--ide", action='store_true',
                         help="Establishes a secured connection to ide. Needs cluster-id set")
    actions.add_argument("-u", "--update", action='store_true', help="Updates master's playbook. "
                                                                     "Needs cluster-id set, no job running "
                                                                     "and no workers up")
    args = parser.parse_args()
    return args

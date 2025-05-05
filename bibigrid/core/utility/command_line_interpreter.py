"""
Has necessary methods and variables to interpret the command line
"""

import argparse
import logging
import os

from bibigrid.core.utility.paths.basic_path import CONFIG_FOLDER, ENFORCED_CONFIG_PATH, DEFAULT_CONFIG_PATH

FOLDER_START = ("~/", "/", "./")
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
    @return:
    """
    parser = argparse.ArgumentParser(description='BiBiGrid easily sets up clusters within a cloud environment')
    parser.add_argument("-v", "--verbose", action="count", default=0,
                        help="Increases logging verbosity. `-v` adds more info to the logfile, "
                             "`-vv` adds debug information to the logfile")
    parser.add_argument("-d", "--debug", action='store_true',
                        help="Keeps cluster active even when crashing. "
                             "Asks before shutdown. "
                             "Offers termination after successful create")
    parser.add_argument("-i", "--config_input", metavar="<path>", help="Path to YAML configurations file. "
                                                                       "Relative paths can be used and start "
                                                                       f"at '{CONFIG_FOLDER}'. "
                                                                       "Required for all actions but '--version'",
                        type=lambda s: os.path.expanduser(s) if s.startswith(FOLDER_START) else
                        os.path.join(CONFIG_FOLDER, s))
    parser.add_argument("-di", "--default_config_input", metavar="<path>",
                        help="Path to default YAML configurations file. "
                             "Relative paths can be used and start "
                             f"at '{CONFIG_FOLDER}'.",
                        type=lambda s: os.path.expanduser(s) if s.startswith(FOLDER_START) else
                        os.path.join(CONFIG_FOLDER, s), default=DEFAULT_CONFIG_PATH)
    parser.add_argument("-ei", "--enforced_config_input", metavar="<path>",
                        help="Path to default YAML configurations file. "
                             "Relative paths can be used and start "
                             f"at '{CONFIG_FOLDER}'.",
                        type=lambda s: os.path.expanduser(s) if s.startswith(FOLDER_START) else
                        os.path.join(CONFIG_FOLDER, s), default=ENFORCED_CONFIG_PATH)
    parser.add_argument("-cid", "--cluster_id", metavar="<cluster-id>", type=check_cid, default="",
                        help="Cluster id is needed for '--ide', '--terminate_cluster' and '--update'. "
                             "If not set, last created cluster's id is used")
    actions = parser.add_mutually_exclusive_group(required=True)
    actions.add_argument("-V", "--version", action='store_true', help="Displays version")
    actions.add_argument("-t", "--terminate", action='store_true',
                         help="Terminates cluster. Needs cluster-id set.")
    actions.add_argument("-c", "--create", action='store_true', help="Creates cluster")
    actions.add_argument("-l", "--list", action='store_true',
                         help="Lists all running clusters. If cluster-id is set, will list this cluster in detail only")
    actions.add_argument("-ch", "--check", action='store_true', help="Validates cluster configuration")
    actions.add_argument("-ide", "--ide", action='store_true',
                         help="Establishes a secure connection to ide. Needs cluster-id set")
    actions.add_argument("-u", "--update", action='store_true', help="Updates master's playbook. "
                                                                     "Needs cluster-id set, no jobs running "
                                                                     "and all workers down (experimental)")
    args = parser.parse_args()
    needs_config = args.terminate or args.create or args.list or args.check or args.ide
    if needs_config and not args.config_input:
        parser.error("requested action requires '-i' ('--config_input')")
    return args

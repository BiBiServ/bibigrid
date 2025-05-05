"""
Contains the static variable __version__ which holds the current version number.
https://www.akeeba.com/how-do-version-numbers-work.html
"""

import logging
import os
import yaml

import seedir

from bibigrid.core.utility.handler import configuration_handler
from bibigrid.core.utility.paths.basic_path import CLOUD_NODE_REQUIREMENTS_PATH

LOG = logging.getLogger("bibigrid")

__version__ = "0.5.0"
RELEASE_DATE = "2025"
GIT_HUB = "https://github.com/BiBiServ/bibigrid"


def version(log):
    log.info("Printing version information")
    log.log(42, f"BiBiGrid {__version__} ({RELEASE_DATE})\nBielefeld University\n{GIT_HUB}\n\n"
                "# Configuration Folders\n")
    for directory in configuration_handler.CLOUDS_YAML_PATHS:
        if os.path.isdir(os.path.expanduser(directory)):
            log.log(42, f"## '{directory}'\n")
            dir_print = seedir.seedir(directory, exclude_folders=["keys", "cluster_info"], printout=False)
            log.log(42, dir_print)
    log.log(42, "# cloud_node_requirements.yaml")

    with open(CLOUD_NODE_REQUIREMENTS_PATH, "r", encoding="UTF-8") as cloud_node_requirements_file:
        cloud_node_requirements = yaml.safe_load(cloud_node_requirements_file)
    log.log(42, cloud_node_requirements)

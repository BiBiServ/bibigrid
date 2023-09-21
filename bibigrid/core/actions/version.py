"""
Contains the static variable __version__ which holds the current version number.
https://www.akeeba.com/how-do-version-numbers-work.html
"""

import logging
import os

import seedir

from bibigrid.core.utility.handler import configuration_handler

LOG = logging.getLogger("bibigrid")

__version__ = "0.4.0"
RELEASE_DATE = "2023"
GIT_HUB = "https://github.com/BiBiServ/bibigrid"


def version(log):
    log.log(42, f"BiBiGrid {__version__} ({RELEASE_DATE})\nBielefeld University\n{GIT_HUB}\n\n"
                "# Configuration Folders\n")
    for directory in configuration_handler.CLOUDS_YAML_PATHS:
        if os.path.isdir(os.path.expanduser(directory)):
            log.log(42, f"## '{directory}'\n")
            dir_print = seedir.seedir(directory, exclude_folders=["keys"], printout=False)
            log.log(42, dir_print)

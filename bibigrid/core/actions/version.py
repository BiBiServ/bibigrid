"""
Contains the static variable __version__ which holds the current version number.
https://www.akeeba.com/how-do-version-numbers-work.html
"""

import logging
import os

import seedir
import yaml

from bibigrid.core.utility.handler import configuration_handler
from bibigrid.core.utility.paths.basic_path import CLOUD_NODE_REQUIREMENTS_PATH

LOG = logging.getLogger("bibigrid")

__version__ = "3.1.0"
RELEASE_DATE = "2025"
GIT_HUB = "https://github.com/BiBiServ/bibigrid"
PROG_NAME = "BiBiGrid"

MESSAGE = f"""{PROG_NAME} {__version__} ({RELEASE_DATE})
Bielefeld University
{GIT_HUB}
# Configuration Folders
{[directory for directory in configuration_handler.CLOUDS_YAML_PATHS]}"""

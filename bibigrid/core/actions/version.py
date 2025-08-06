"""
Contains the static variable __version__ which holds the current version number.
https://www.akeeba.com/how-do-version-numbers-work.html
"""

import logging

from bibigrid.core.utility.handler import configuration_handler

LOG = logging.getLogger("bibigrid")

__version__ = "3.1.0"
RELEASE_DATE = "2025"
GIT_HUB = "https://github.com/BiBiServ/bibigrid"
PROG_NAME = "BiBiGrid"

MESSAGE = f"""{PROG_NAME} {__version__} ({RELEASE_DATE})
Bielefeld University
{GIT_HUB}
# Configuration Folders
{configuration_handler.CLOUDS_YAML_PATHS}"""

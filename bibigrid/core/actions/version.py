"""
Contains the static variable __version__ which holds the current version number.
https://www.akeeba.com/how-do-version-numbers-work.html
"""

import logging
import os
import seedir

from bibigrid.core.utility.handler import configuration_handler

LOG = logging.getLogger("bibigrid")

__version__ = "0.3.0"
RELEASE_DATE = "2023"
GIT_HUB = "https://github.com/BiBiServ/bibigrid"


def version():
    print(f"BiBiGrid {__version__} ({RELEASE_DATE})\nUniversität Bielefeld\nWebsite: {GIT_HUB}\n\n"
          "# Configuration Folders\n")
    for directory in configuration_handler.CLOUDS_YAML_PATHS:
        if os.path.isdir(os.path.expanduser(directory)):
            print(f"## '{directory}'\n")
            seedir.seedir(directory)

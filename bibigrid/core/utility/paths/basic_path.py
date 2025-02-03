"""
Module containing the most basic paths. Must stay at the same place relative to root.
"""

import os
from pathlib import Path

RESOURCES = "resources"
# if the relative path from this file to resources is altered, the next line must be adapted or files will not be found.
ROOT_PATH = Path(__file__).absolute().parents[4]
RESOURCES_PATH = os.path.join(ROOT_PATH, RESOURCES)

STANDARD_CONFIG_PATH = "~/.config/bibigrid"
CLUSTER_INFO = "cluster_info"
CONFIG_FOLDER = os.path.expanduser(STANDARD_CONFIG_PATH)
CLUSTER_INFO_FOLDER = os.path.join(CONFIG_FOLDER, CLUSTER_INFO)

KEY_FOLDER = os.path.join(CONFIG_FOLDER, "keys/")

CLUSTER_MEMORY_FOLDER = KEY_FOLDER
CLUSTER_MEMORY_FILE = ".bibigrid.mem"
CLUSTER_MEMORY_PATH = os.path.join(CONFIG_FOLDER, CLUSTER_MEMORY_FILE)
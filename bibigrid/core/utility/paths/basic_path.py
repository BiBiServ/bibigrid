"""
Module containing the most basic paths. Must stay at the same place relative to root.
"""

import os
from pathlib import Path

RESOURCES = "resources"
# if the relative path from this file to resources is altered, the next line must be adapted or files will not be found.
ROOT_PATH = Path(__file__).absolute().parents[3]
RESOURCES_PATH = os.path.join(ROOT_PATH, RESOURCES)

CLOUD_NODE_REQUIREMENTS_FILE = "cloud_node_requirements.yaml"
CLOUD_NODE_REQUIREMENTS_PATH = os.path.join(RESOURCES_PATH, CLOUD_NODE_REQUIREMENTS_FILE)

CONFIG_PATH = os.path.expanduser("~/.config")
CONFIG_FOLDER = os.path.join(CONFIG_PATH, "bibigrid")
ENFORCED_CONFIG_PATH = os.path.join(CONFIG_FOLDER, "enforced_bibigrid.yaml")
DEFAULT_CONFIG_PATH = os.path.join(CONFIG_FOLDER, "default_bibigrid.yaml")
CLUSTER_INFO = "cluster_info"
CLUSTER_INFO_FOLDER = os.path.join(CONFIG_FOLDER, CLUSTER_INFO)

KEY_FOLDER = os.path.join(CONFIG_FOLDER, "keys/")

CLUSTER_MEMORY_FOLDER = KEY_FOLDER
CLUSTER_MEMORY_FILE = ".bibigrid.mem"
CLUSTER_MEMORY_PATH = os.path.join(CONFIG_FOLDER, CLUSTER_MEMORY_FILE)

"""
Module containing the most basic paths. Must stay at the same place relative to root.
"""

import os
from pathlib import Path

RESOURCES = "resources"
# if the relative path from this file to resources is altered, the next line must be adapted or files will not be found.
ROOT_PATH = Path(__file__).absolute().parents[4]
RESOURCES_PATH = os.path.join(ROOT_PATH, RESOURCES)

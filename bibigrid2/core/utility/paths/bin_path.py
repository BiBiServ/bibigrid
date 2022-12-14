"""
Paths that are used by bin script copying
"""


import os

import bibigrid2.core.utility.paths.basic_path as bP

BIN: str = "bin/"
BIN_PATH: str = os.path.join(bP.RESOURCES_PATH, BIN)

BIN_PATH_REMOTE: str = BIN

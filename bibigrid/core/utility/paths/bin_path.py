"""
Paths that are used by bin script copying
"""


import os

import bibigrid.core.utility.paths.basic_path as b_p

BIN: str = "bin/"
BIN_PATH: str = os.path.join(b_p.RESOURCES_PATH, BIN)

BIN_PATH_REMOTE: str = BIN

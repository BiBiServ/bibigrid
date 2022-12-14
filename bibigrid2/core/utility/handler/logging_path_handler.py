"""
This module holds methods to return the logfile's path.
"""

import logging

LOG = logging.getLogger("bibigrid")

def get_logging_path():
    """
    Returns the path were the logfile is stored
    @return: the path were the logfile is stored
    """
    for handler in LOG.getLoggerClass().root.handlers:
        if hasattr(handler, 'baseFilename'):
            log_path = handler.baseFilename
            return log_path
    return None

"""
Module that acts as a wrapper and uses validate_configuration to validate given configuration
"""
import logging
from bibigrid.core.utility import validate_configuration

LOG = logging.getLogger("bibigrid")


def check(configurations, providers):
    """
    Uses validate_configuration to validate given configuration.
    :param configurations: list of configurations (dicts)
    :param providers: list of providers
    :return:
    """
    success = validate_configuration.ValidateConfiguration(configurations, providers).validate()
    check_result = "succeeded! Cluster is ready to start." if success else "failed!"
    print(f"Total check {check_result}")
    LOG.info("Total check returned %s.", success)
    return 0

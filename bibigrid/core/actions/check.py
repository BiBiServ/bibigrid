"""
Module that acts as a wrapper and uses validate_configuration to validate given configuration
"""
from bibigrid.core.utility import validate_configuration


def check(configurations, providers, log):
    """
    Uses validate_configuration to validate given configuration.
    :param configurations: list of configurations (dicts)
    :param providers: list of providers
    :param log:
    :return:
    """
    success = validate_configuration.ValidateConfiguration(configurations, providers, log).validate()
    check_result = "succeeded! Cluster is ready to start." if success else "failed!"
    log.log(0, f"Total check {check_result}")
    log.info("Total check returned %s.", success)
    return 0

"""
This module contains different selectors to pick and create a connection to the right provider.
"""

from bibigrid.core.utility.handler import configuration_handler
from bibigrid.openstack import openstack_provider

PROVIDER_NAME_DICT = {"openstack": openstack_provider.OpenstackProvider}
PROVIDER_CLASS_DICT = {provider.__name__: provider for provider in PROVIDER_NAME_DICT.values()}


def get_provider_by_class_name(provider_name,
                               provider_dict=PROVIDER_CLASS_DICT):  # pylint: disable=dangerous-default-value
    """
    Returns provider that is associated with the key provider_name in provider_dict.
    Otherwise, a KeyError is thrown.
    :param provider_name: key of provider_dict
    :return: provider
    """
    return provider_dict[provider_name]


def get_provider_by_name(provider_name, provider_dict=PROVIDER_NAME_DICT):  # pylint: disable=dangerous-default-value
    """
    Returns provider that is associated with the key provider_name in provider_dict.
    Otherwise a KeyError is thrown.
    :param provider_name: key of provider_dict
    :return: provider
    """
    return provider_dict.get(provider_name)


def get_provider_list_by_name_list(provider_name_list, cloud_specifications):
    """
    Returns provider list for given provider_name_list
    If name is not found in PROVIDER_NAME_DICT, PROVIDER_CLASS_DICT is tried instead.
    If not found in both a key error is thrown.
    :param provider_name_list: list of provider names
    :param cloud_specifications: list of cloud specifications
    :return: list of providers
    """
    provider_list = [
        (get_provider_by_name(provider_name) or get_provider_by_class_name(provider_name))(cloud_specification) for
        provider_name, cloud_specification in zip(provider_name_list, cloud_specifications)]
    return provider_list


def get_providers(configurations, log):
    """
    Reads list of provider_names from configurations.
    Determines list of providers by provider_names and returns it.
    If providers don't match a key error is thrown and the program exits with failure state 1.
    :param configurations:
    :return:
    """
    cloud_specifications = configuration_handler.get_cloud_specifications(configurations)
    if cloud_specifications:
        try:
            provider_names = configuration_handler.get_list_by_key(configurations, "infrastructure")
            return get_provider_list_by_name_list(provider_names, cloud_specifications)
        except KeyError as exc:
            log.warning("Check infrastructure in configurations! Key: %s", str(exc))
    return None

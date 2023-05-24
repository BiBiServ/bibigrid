"""
This module contains methods to read the configuration and cloud specification.
"""

import logging
import os

import mergedeep
import yaml

CLOUDS_YAML_PATHS = ["~/.config/bibigrid", "/etc/bibigrid", ""]
CLOUDS_YAML = "clouds.yaml"
CLOUDS_PUBLIC_YAML = "clouds-public.yaml"
CLOUD_ROOT_KEY = "clouds"
CLOUD_PUBLIC_ROOT_KEY = "public-clouds"
CLOUDS_PUBLIC_NAME_KEY = "profile"
CLOUD_CONFIGURATION_KEY = "cloud"

LOG = logging.getLogger("bibigrid")


def read_configuration(path="bibigrid.yml"):
    """
    Reads yaml from file and returns the list of all configurations
    :param path: Path to yaml file
    :return: configurations (dict)
    """
    configuration = None
    if os.path.isfile(path):
        with open(path, mode="r", encoding="UTF-8") as stream:
            try:
                configuration = yaml.safe_load(stream)
            except yaml.YAMLError as exc:
                LOG.warning("Couldn't read configuration %s: %s", path, exc)
    else:
        LOG.warning("No such configuration file %s.", path)

    if not isinstance(list, configuration):
        LOG.warning("Configuration should be list. Attempting to rescue by assuming a single configuration.")
        return [configuration]
    return configuration


def get_list_by_key(configurations, key, get_empty=True):
    """
    Returns a list of objects which are value to the key.
    :param get_empty: if true empty configurations return None
    :param configurations: YAML of configuration File containing the configuration-data for each provider
    :param key: Key that is looked out for
    :return: List of values of said key through all configs
    """
    return [configuration.get(key) for configuration in configurations if configuration.get(key) or get_empty]


# def get_dict_list_by_key_list(configurations, keys, get_empty=True):
#    return [{key: configuration.get(key) for key in keys if configuration.get(key) or get_empty}
#            for configuration in configurations]


def find_file_in_folders(file_name, folders):
    """
    Searches all folders for a file with name file_name, loads (expects yaml) the first match and returns the dict
    @param file_name: name of the file to look for
    @param folders: folders to search for file named file_name
    @return: dict of match content or None if not found
    """
    for folder_path in folders:
        file_path = os.path.expanduser(os.path.join(folder_path, file_name))
        if os.path.isfile(file_path):
            LOG.debug("File %s found in folder %s.", file_name, folder_path)
            return read_configuration(file_path)
        LOG.debug("File %s not found in folder %s.", file_name, folder_path)
    return None


def get_clouds_files():
    """
    Wrapper to call find_file_in_folders with the right arguments to find the clouds.yaml and clouds-public.yaml
    @return: tuple of dicts containing the clouds.yaml and clouds-public.yaml data or None if not found.
    """
    clouds_yaml = find_file_in_folders(CLOUDS_YAML, CLOUDS_YAML_PATHS)
    clouds_public_yaml = find_file_in_folders(CLOUDS_PUBLIC_YAML, CLOUDS_YAML_PATHS)
    clouds = None
    clouds_public = None
    if clouds_yaml:
        clouds = clouds_yaml.get(CLOUD_ROOT_KEY)
        if not clouds:
            LOG.warning("%s is not valid. Must contain key '%s:'", CLOUDS_YAML, CLOUD_ROOT_KEY)
    else:
        LOG.warning("No %s at %s! Please copy your %s to one of those listed folders. Aborting...", CLOUDS_YAML,
                    CLOUDS_YAML_PATHS, CLOUDS_YAML)
    if clouds_public_yaml:
        clouds_public = clouds_public_yaml.get(CLOUD_PUBLIC_ROOT_KEY)
        if not clouds_public:
            LOG.warning("%s is not valid. Must contain key '%s'", CLOUDS_PUBLIC_YAML, CLOUD_PUBLIC_ROOT_KEY)
    return clouds, clouds_public


def get_cloud_specification(cloud_name, clouds, clouds_public):
    """
    As in openstack cloud_public_specification will be overwritten by cloud_private_specification
    :param cloud_name: name of the cloud to look for in clouds.yaml
    :param clouds: dict containing the data loaded from clouds.yaml
    :param clouds_public: dict containing the data loaded from clouds-public.yaml
    :return:
    """
    cloud_full_specification = {}
    cloud_private_specification = clouds.get(cloud_name)
    if cloud_private_specification:
        cloud_full_specification = cloud_private_specification
        public_cloud_name = cloud_private_specification.get(CLOUDS_PUBLIC_NAME_KEY)
        if public_cloud_name and clouds_public:
            LOG.debug("Trying to find profile...")
            cloud_public_specification = clouds_public.get(public_cloud_name)
            if not cloud_public_specification:
                LOG.warning("%s is not a valid profile name. "
                            "Must be contained under key '%s'", public_cloud_name, CLOUD_PUBLIC_ROOT_KEY)
            else:
                LOG.debug("Profile found. Merging begins...")
                try:
                    mergedeep.merge(cloud_full_specification, cloud_public_specification,
                                    strategy=mergedeep.Strategy.TYPESAFE_REPLACE)
                except TypeError as exc:
                    LOG.warning("Existing %s and %s configuration keys don't match in type: %s", CLOUDS_YAML,
                                CLOUDS_PUBLIC_YAML, exc)
                    return {}
        else:
            LOG.debug("Using only clouds.yaml since no clouds-public profile is set.")

        if not cloud_full_specification.get("identifier"):
            cloud_full_specification["identifier"] = cloud_name
    else:
        LOG.warning("%s is not a valid cloud name. Must be contained under key '%s'", cloud_name, CLOUD_ROOT_KEY)
    return cloud_full_specification


def get_cloud_specifications(configurations):
    """
    Calls get_cloud_specification to get the cloud_specification for every configuration
    @param configurations:
    @return: list of dicts: cloud_specifications of every configuration
    """
    clouds, clouds_public = get_clouds_files()
    LOG.debug("Loaded clouds.yml and clouds_public.yml")
    cloud_specifications = []
    if isinstance(clouds, dict):
        for configuration in configurations:
            cloud = configuration.get(CLOUD_CONFIGURATION_KEY)
            if cloud:
                cloud_specifications.append(get_cloud_specification(cloud, clouds, clouds_public))  # might be None
    return cloud_specifications

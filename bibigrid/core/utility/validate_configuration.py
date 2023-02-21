"""
Validates configuration and cloud_specification
"""

import logging
import os

from bibigrid.core.utility.handler import configuration_handler

ACCEPTED_KEY_IDENTIFIERS = {"RSA": 4096, "ECDSA": 521, "ED25519": 256}
LOG = logging.getLogger("bibigrid")


def evaluate(check_name, check_result):
    """
    Logs check_resul as warning if failed and as success if succeeded.
    :param check_name:
    :param check_result:
    :return:
    """
    if check_result:
        LOG.info("Checking %s: Success", check_name)
    else:
        LOG.warning("Checking %s: Failure", check_name)
    return check_result


def check_provider_data(provider_data_list, provider_count):
    """
    Checks if all provider datas are unique and if enough providers are given
    #ToDo for multiple cloud locations additional provider data needs to be added
    :param provider_data_list: list of all provider data
    :param provider_count: number of providers
    :return: True if enough providers are given and all providers are unique
    """
    LOG.info("Checking provider names")
    success = True
    duplicates = []
    seen = []
    for elem in provider_data_list:
        if elem in seen:
            duplicates.append(elem)
        else:
            seen.append(elem)
    if duplicates:
        LOG.warning("Duplicate provider(s) %s. For each provider you can only create one configuration. "
                    "Please check your configurations.", duplicates)
        success = False
    else:
        LOG.info("All providers are unique.")
    if not len(provider_data_list) == provider_count:
        LOG.warning("Not enough providers given. %s/%s", len(provider_data_list), provider_count)
        success = False
    else:
        LOG.info("Enough providers given. %s/%s", len(provider_data_list), provider_count)
    return success


def evaluate_ssh_public_key_file_security(ssh_public_key_file):
    """
    Checks if key encryption is sufficiently strong. Uses empiric values and therefore will fail if key type is unknown
    @param ssh_public_key_file:
    @return:
    """
    success = True
    # length, key, comment list, identifier_dirty
    key_info = os.popen(f'ssh-keygen -l -f {ssh_public_key_file}').read().split()
    length = key_info[0]
    identifier_clean = key_info[-1].strip("()\n")
    minimum_size = ACCEPTED_KEY_IDENTIFIERS.get(identifier_clean)

    if not minimum_size:
        LOG.warning("sshPublicKey '%s' is %s. Which secure length is unknown to bibigrid.\n"
                    "Known encryptions are (with minimum size): %s",
                    ssh_public_key_file, identifier_clean, ACCEPTED_KEY_IDENTIFIERS)
    else:
        LOG.info("sshPublicKey '%s' is a known encryption.", ssh_public_key_file)
        if minimum_size > int(length):
            LOG.warning("sshPublicKey '%s' is not long enough! %s should be >= %s, but is %s",
                        ssh_public_key_file, identifier_clean, minimum_size, int(length))
        else:
            LOG.info("sshPublicKey '%s' is long enough (%s/%s)!", ssh_public_key_file, int(length), minimum_size)
    return success


def has_enough(maximum, needed, keeper, thing):
    """
    Method logs and compares whether enough free things are available
    :param maximum: maximum (available) resources of thing
    :param needed: minimum needed to run
    :param keeper: description of the object having the thing that is checked (for logging)
    :param thing: description of what resource is checked (RAM for example) (for logging)
    :return: True if maximum is larger or equal to the needed
    """
    success = True
    if maximum >= needed:
        LOG.info("%s has enough %s: %s/%s", keeper, thing, needed, maximum)
    elif maximum < 0:
        LOG.warning("%s returns no valid value for %s: %s/%s -- Ignored.", keeper, thing, needed, maximum)
    else:
        LOG.warning("%s has not enough %s: %s/%s", keeper, thing, needed, maximum)
        success = False
    return success


def check_clouds_yaml_security():
    """
    Checks security of all clouds in clouds.yaml i.e. whether sensitive information is stored in clouds-public.yaml
    @return: True if no sensitive information is stored in clouds-public.yaml. False else.
    """
    success = True
    LOG.info("Checking validity of entire clouds.yaml and clouds-public.yaml")
    clouds, clouds_public = configuration_handler.get_clouds_files()  # pylint: disable=unused-variable
    if clouds_public:
        for cloud in clouds_public:
            if clouds_public[cloud].get("profile"):
                LOG.warning(f"{cloud}: Profiles should be placed in clouds.yaml not clouds-public.yaml! "
                            f"Key ignored.")
                success = False
            if clouds_public[cloud].get("auth"):
                for key in ["password", "username", "application_credential_id", "application_credential_secret"]:
                    if clouds_public[cloud]["auth"].get(key):
                        LOG.warning(f"{cloud}: {key} shouldn't be shared. Move {key} to clouds.yaml!")
                        success = False
    return success


def check_cloud_yaml(cloud_specification):
    """
    Check if cloud_specification is valid i.e. contains the necessary authentification data.
    @param cloud_specification: dict to check whether it is a valid cloud_specification
    @return: True if cloud_specification is valid. False else.
    """
    success = True
    if cloud_specification:
        keys = cloud_specification.keys()
        auth = cloud_specification.get("auth")
        if auth:
            auth_keys = auth.keys()
            if not ("password" in auth_keys and "username" in auth_keys) \
                    and not ("auth_type" in keys and "application_credential_id" in auth_keys and
                             "application_credential_secret" in auth_keys):
                LOG.warning("Insufficient authentication information. Needs either password and username or "
                            "if using application credentials: "
                            "auth_type, application_credential_id and application_credential_secret.")
                success = False
            if "auth_url" not in auth_keys:
                LOG.warning("Authentification URL auth_url is missing.")
                success = False
        else:
            LOG.warning("Missing all auth information!")
            success = False
        if "region_name" not in keys:
            LOG.warning("region_name is missing.")
            success = False
    else:
        LOG.warning("Missing all cloud_specification information!")
    return success


class ValidateConfiguration:
    """
    This class contains necessary algorithms to validate configuration files
    """

    def __init__(self, configurations, providers):
        """
        Sets configurations, providers and prepares the required_resources_dict.
        While executing the checks, needed resources are counted.
        In the end check_quotas will decide whether enough resources are available.
        :param configurations: List of configurations (dicts)
        :param providers: List of providers
        """
        self.configurations = configurations
        self.providers = providers
        self.required_resources_dict = {'total_cores': 0, 'floating_ips': 0, 'instances': 0, 'total_ram': 0,
                                        'Volumes': 0, 'VolumeGigabytes': 0, 'Snapshots': 0, 'Backups': 0,
                                        'BackupGigabytes': 0}

    def validate(self):
        """
            Validation of the configuration file with the selected cloud provider.
            The validation steps are as follows:
            Check connection can be established
            Check provider uniqueness
            Check servergroup
            Check instances are available
            Check images and volumes are available
            Check network and subnet are available
            Check quotas
        :return:
        """
        success = bool(self.providers)
        LOG.info("Validating config file...")
        success = check_provider_data(
            configuration_handler.get_list_by_key(self.configurations, "infrastructure"),
            len(self.configurations)) and success
        if not success:
            LOG.warning("Providers not set correctly in configuration file. Check log for more detail.")
            return success
        checks = [("master/vpn", self.check_master_vpn_worker), ("servergroup", self.check_server_group),
                  ("instances", self.check_instances), ("volumes", self.check_volumes),
                  ("network", self.check_network), ("quotas", self.check_quotas),
                  ("sshPublicKeyFiles", self.check_ssh_public_key_files), ("cloudYamls", self.check_clouds_yamls),
                  ("nfs", self.check_nfs)]
        if success:
            for check_name, check_function in checks:
                success = evaluate(check_name, check_function()) and success
        return success

    def check_master_vpn_worker(self):
        """
        Checks if first configuration has a masterInstance defined
        and every other configuration has a vpnInstance defined.
        If one is missing said provider wouldn't be reachable over the cluster, because no floating IP would be given.
        :return: True if first configuration has a masterInstance and every other a vpnInstance
        """
        LOG.info("Checking master/vpn")
        success = True
        if not self.configurations[0].get("masterInstance") or self.configurations[0].get("vpnInstance"):
            success = False
        for configuration in self.configurations[1:]:
            if not configuration.get("vpnInstance") or configuration.get("masterInstance"):
                success = False
        return success

    def check_provider_connections(self):
        """
        Checks if all providers are reachable
        :return: True if all providers are reachable
        """
        success = True
        providers_unconnectable = []
        for provider in self.providers:
            if not provider.conn:
                providers_unconnectable.append(provider.name)
        if providers_unconnectable:
            LOG.warning("API connection to %s not successful. Please check your configuration.",
                        providers_unconnectable)
            success = False
        return success

    def check_instances(self):
        """
        Checks if all instances exist and image and instance-type are compatible
        :return: true if image and instance-type (flavor) exist for all instances and are compatible
        """
        LOG.info("Checking instance images and type")
        success = True
        configuration = None
        try:
            for configuration, provider in zip(self.configurations, self.providers):
                self.required_resources_dict["floating_ips"] += 1
                if configuration.get("masterInstance"):
                    success = self.check_instance("masterInstance", configuration["masterInstance"], provider) \
                              and success
                else:
                    success = self.check_instance("vpnInstance", configuration["vpnInstance"], provider) and success
                for worker in configuration.get("workerInstances", []):
                    success = self.check_instance("workerInstance", worker, provider) and success
        except KeyError as exc:
            LOG.warning("Not found %s, but required in configuration %s.", str(exc), configuration)
            success = False
        return success

    def check_instance(self, instance_name, instance, provider):
        """
        Checks if instance image exists and whether it is compatible with the defined instance/server type (flavor).
        :param instance_name: containing name for logging purposes
        :param instance: dict containing image, type and count (count is not used)
        :param provider: provider
        :return: true if type and image compatible and existing
        """
        self.required_resources_dict["instances"] += instance.get("count") or 1
        instance_image_id_or_name = instance["image"]
        instance_image = provider.get_image_by_id_or_name(image_id_or_name=instance_image_id_or_name)
        if not instance_image:
            LOG.warning("Instance %s image: %s not found", instance_name, instance_image_id_or_name)
            print("Available active images:")
            print("\n".join(provider.get_active_images()))
            return False
        if instance_image["status"] != "active":
            LOG.warning("Instance %s image: %s not active", instance_name, instance_image_id_or_name)
            print("Available active images:")
            print("\n".join(provider.get_active_images()))
            return False
        LOG.info("Instance %s image: %s found", instance_name, instance_image_id_or_name)
        instance_type = instance["type"]
        return self.check_instance_type_image_combination(instance_type, instance_image, provider)

    def check_instance_type_image_combination(self, instance_type, instance_image, provider):
        """
        Checks, if enough ram, disk space for instance_image are provided by instance_type on provider.
        :param instance_type
        :param instance_image
        :param provider
        :return true, if enough resources available
        """
        success = True
        # check
        flavor = provider.get_flavor(instance_type)
        if not flavor:
            LOG.warning("Flavor %s does not exist.", instance_type)
            print("Available flavors:")
            print("\n".join(provider.get_active_flavors()))
            return False
        type_max_disk_space = flavor["disk"]
        type_max_ram = flavor["ram"]
        image_min_disk_space = provider.get_image_by_id_or_name(instance_image)["min_disk"]
        image_min_ram = provider.get_image_by_id_or_name(instance_image)["min_ram"]
        for maximum, needed, thing in [(type_max_disk_space, image_min_disk_space, "disk space"),
                                       (type_max_ram, image_min_ram, "ram")]:
            success = has_enough(maximum, needed, f"Type {instance_type}", thing) and success
        # prepare check quotas
        self.required_resources_dict["total_ram"] += type_max_ram
        self.required_resources_dict["total_cores"] += flavor["vcpus"]
        return success

    def check_volumes(self):
        """
        Checking if volume or snapshot exists for all volumes
        :return: True if all snapshot and volumes are found. Else false.
        """
        LOG.info("Checking volumes...")
        success = True
        for configuration, provider in zip(self.configurations, self.providers):
            volume_identifiers = configuration.get("masterMounts")
            if volume_identifiers:
                # check individually if volumes exist
                for volume_identifier in volume_identifiers:
                    if ":" in volume_identifier:
                        volume_name_or_id = volume_identifier[:volume_identifier.index(":")]
                    else:
                        volume_name_or_id = volume_identifier
                    volume = provider.get_volume_by_id_or_name(volume_name_or_id)
                    if not volume:
                        snapshot = provider.get_volume_snapshot_by_id_or_name(volume_name_or_id)
                        if not snapshot:
                            LOG.warning("Neither Volume nor Snapshot '%s' found", volume_name_or_id)
                            success = False
                        else:
                            LOG.info("Snapshot '%s' found", volume_name_or_id)
                            self.required_resources_dict["Volumes"] += 1
                            self.required_resources_dict["VolumeGigabytes"] += snapshot["size"]
                    else:
                        LOG.info(f"Volume '{volume_name_or_id}' found")
        return success

    def check_network(self):
        """
        Check if network (or subnet) is accessible
        :return True if any given network or subnet is accessible by provider
        """
        LOG.info("Checking network...")
        success = True
        for configuration, provider in zip(self.configurations, self.providers):
            network_name_or_id = configuration.get("network")
            if network_name_or_id:
                network = provider.get_network_by_id_or_name(network_name_or_id)
                if not network:
                    LOG.warning(f"Network '{network_name_or_id}' not found", network_name_or_id)
                    success = False
                else:
                    LOG.info(f"Network '{subnet_name_or_id}' found")
            subnet_name_or_id = configuration.get("subnet")
            if subnet_name_or_id:
                subnet = provider.get_subnet_by_id_or_name(subnet_name_or_id)
                if not subnet:
                    LOG.warning(f"Subnet '{subnet_name_or_id}' not found")
                    success = False
                else:
                    LOG.info(f"Subnet '{subnet_name_or_id}' found")
        return bool(success and (network_name_or_id or subnet_name_or_id))

    def check_server_group(self):
        """
        :return: True if server group accessible
        """
        success = True
        for configuration, provider in zip(self.configurations, self.providers):
            server_group_name_or_id = configuration.get("serverGroup")
            if server_group_name_or_id:
                server_group = provider.get_server_group_by_id_or_name(server_group_name_or_id)
                if not server_group:
                    LOG.warning("ServerGroup '%s' not found", server_group_name_or_id)
                    success = False
                else:
                    LOG.info("ServerGroup '%s' found", server_group_name_or_id)
        return success

    def check_quotas(self):
        """
        Gets remaining resources from the provider and compares them to the needed resources.
        Needed resources are set during the other checks.
        Covered resources are: cores, floating_ips, instances, ram, volumes, volumeGigabytes, snapshots, backups and
        backupGigabytes. If a concrete provider implementation is unable to return remaining resources a maximum number
        is returned to make the check not fail because of the missing API implementation.
        :return: True if check succeeded. Else false.
        """
        LOG.info("Checking quotas")
        success = True
        LOG.info("required/available")
        for provider in self.providers:
            free_resources_dict = provider.get_free_resources()
            for key, value in self.required_resources_dict.items():
                success = has_enough(free_resources_dict[key],
                                     value,
                                     f"Project {self.providers[0].cloud_specification['identifier']}",
                                     key) and success
        return success

    def check_ssh_public_key_files(self):
        """
        Checks if keys listed in the config exist
        :return: True if check succeeded. Else false.
        """
        success = True
        for configuration in self.configurations:
            for ssh_public_key_file in configuration.get("sshPublicKeyFiles") or []:
                if not os.path.isfile(ssh_public_key_file):
                    LOG.warning("sshPublicKeyFile '%s' not found", ssh_public_key_file)
                    success = False
                else:
                    LOG.info("sshPublicKeyFile '%s' found", ssh_public_key_file)
                    success = evaluate_ssh_public_key_file_security(ssh_public_key_file) and success
        return success

    def check_clouds_yamls(self):
        """
        Checks if every cloud in clouds_yaml is valid
        @return: True if all clouds are valid
        """
        LOG.info("Checking cloud specifications...")
        success = True
        cloud_specifications = configuration_handler.get_cloud_specifications(self.configurations)
        for index, cloud_specification in enumerate(cloud_specifications):
            if not check_cloud_yaml(cloud_specification):
                success = False
                LOG.warning("Cloud specification %s is faulty. BiBiGrid understood %s.", index, cloud_specification)
            success = check_clouds_yaml_security() and success
        return success

    def check_nfs(self):
        """
        Checks whether nfsshares => nfs holds and logs if failed. Returns True in every case as it is not fatale.
        @return: True
        """
        LOG.info("Checking nfs...")
        success = True
        master_configuration = self.configurations[0]
        nfs_shares = master_configuration.get("nfsShares")
        nfs = master_configuration.get("nfs")
        if nfs_shares and not nfs:
            success = True
            LOG.warning("nfsShares exist, but nfs is False. nfsShares will be ignored!")
        else:
            success = True
        return success

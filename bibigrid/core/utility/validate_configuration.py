"""
Validates configuration and cloud_specification
"""

import os

from bibigrid.core.utility import image_selection
from bibigrid.core.utility import validate_schema
from bibigrid.core.utility.handler import configuration_handler
from bibigrid.models.exceptions import ImageNotActiveException

ACCEPTED_KEY_IDENTIFIERS = {"RSA": 4096, "ECDSA": 521, "ED25519": 256}


def evaluate(check_name, check_result, log):
    """
    Logs check_result as warning if failed and as success if succeeded.
    @param check_name:
    @param check_result:
    @param log:
    @return:
    """
    if check_result:
        log.info("Checking %s: Success", check_name)
    else:
        log.warning("Checking %s: Failure", check_name)
    return check_result


def check_provider_data(provider_data_list, provider_count, log):
    """
    Checks if all provider datas are unique and if enough providers are given
    #ToDo for multiple cloud locations additional provider data needs to be added
    @param provider_data_list: list of all provider data
    @param provider_count: number of providers
    @param log:
    @return: True if enough providers are given and all providers are unique
    """
    log.info("Checking provider names")
    success = True
    duplicates = []
    seen = []
    for elem in provider_data_list:
        if elem in seen:
            duplicates.append(elem)
        else:
            seen.append(elem)
    if duplicates:
        log.warning("Duplicate provider(s) %s. For each provider you can only create one configuration. "
                    "Please check your configurations.", duplicates)
        success = False
    else:
        log.info("All providers are unique.")
    if not len(provider_data_list) == provider_count:
        log.warning("Not enough providers given. %s/%s", len(provider_data_list), provider_count)
        success = False
    else:
        log.info("Enough providers given. %s/%s", len(provider_data_list), provider_count)
    return success


def evaluate_ssh_public_key_file_security(ssh_public_key_file, log):
    """
    Checks if key encryption is sufficiently strong. Uses empiric values and therefore will fail if key type is unknown
    @param ssh_public_key_file:
    @param log:
    @return:
    """
    success = True
    # length, key, comment list, identifier_dirty
    key_info = os.popen(f'ssh-keygen -l -f {ssh_public_key_file}').read().split()
    length = key_info[0]
    identifier_clean = key_info[-1].strip("()\n")
    minimum_size = ACCEPTED_KEY_IDENTIFIERS.get(identifier_clean)

    if not minimum_size:
        log.warning("sshPublicKey '%s' is %s. Which secure length is unknown to bibigrid.\n"
                    "Known encryptions are (with minimum size): %s", ssh_public_key_file, identifier_clean,
                    ACCEPTED_KEY_IDENTIFIERS)
    else:
        log.info("sshPublicKey '%s' is a known encryption.", ssh_public_key_file)
        if minimum_size > int(length):
            log.warning("sshPublicKey '%s' is not long enough! %s should be >= %s, but is %s", ssh_public_key_file,
                        identifier_clean, minimum_size, int(length))
        else:
            log.info("sshPublicKey '%s' is long enough (%s/%s)!", ssh_public_key_file, int(length), minimum_size)
    return success


def has_enough(maximum, needed, keeper, thing, log):
    """
    Method logs and compares whether enough free things are available
    @param maximum: maximum (available) resources of thing
    @param needed: minimum needed to run
    @param keeper: description of the object having the thing that is checked (for logging)
    @param thing: description of what resource is checked (RAM for example) (for logging)
    @param log:
    @return: True if maximum is larger or equal to the needed
    """
    success = True
    if maximum >= needed:
        log.info("%s has enough %s: %s/%s", keeper, thing, needed, maximum)
    elif maximum < 0:
        log.warning("%s returns no valid value for %s: %s/%s -- Ignored.", keeper, thing, needed, maximum)
    else:
        log.warning("%s has not enough %s: %s/%s", keeper, thing, needed, maximum)
        success = False
    return success


def check_clouds_yaml_security(log):
    """
    Checks security of all clouds in clouds.yaml i.e. whether sensitive information is stored in clouds-public.yaml
    @param log:
    @return: True if no sensitive information is stored in clouds-public.yaml. False else.
    """
    success = True
    log.info("Checking validity of entire clouds.yaml and clouds-public.yaml")
    clouds, clouds_public = configuration_handler.get_clouds_files(log)  # pylint: disable=unused-variable
    if clouds_public:
        for cloud in clouds_public:
            if clouds_public[cloud].get("profile"):
                log.warning(f"{cloud}: Profiles should be placed in clouds.yaml not clouds-public.yaml!")
                success = False
            if clouds_public[cloud].get("auth"):
                for key in ["password", "username", "application_credential_id", "application_credential_secret"]:
                    if clouds_public[cloud]["auth"].get(key):
                        log.warning(f"{cloud}: {key} shouldn't be shared. Move {key} to clouds.yaml!")
                        success = False
    return success


def check_cloud_yaml(cloud_specification, log):
    """
    Check if cloud_specification is valid i.e. contains the necessary authentication data.
    @param cloud_specification: dict to check whether it is a valid cloud_specification
    @param log
    @return: True if cloud_specification is valid. False else.
    """
    success = True
    if cloud_specification:
        keys = cloud_specification.keys()
        auth = cloud_specification.get("auth")
        if auth:
            auth_keys = auth.keys()
            if not ("password" in auth_keys and "username" in auth_keys) and not (
                    "auth_type" in keys and "application_credential_id" in auth_keys and
                    "application_credential_secret" in auth_keys):
                log.warning("Insufficient authentication information. Needs either password and username or "
                            "if using application credentials: "
                            "auth_type, application_credential_id and application_credential_secret. "
                            f"In cloud specification {cloud_specification.get('identifier')}")
                success = False
            if "auth_url" not in auth_keys:
                log.warning(f"Authentication URL auth_url is missing in cloud specification "
                            f"{cloud_specification.get('identifier')}")
                success = False
        else:
            log.warning(f"Missing all auth information in cloud specification {cloud_specification.get('identifier')}!")
            success = False
        if "region_name" not in keys:
            log.warning(f"region_name is missing in cloud specification {cloud_specification.get('identifier')}.")
            success = False
    else:
        log.warning(f"{cloud_specification.get('identifier')} missing all cloud_specification information!")
    return success


class ValidateConfiguration:
    """
    This class contains necessary algorithms to validate configuration files
    """

    def __init__(self, configurations, providers, log):
        """
        Sets configurations, providers and prepares the required_resources_dict.
        While executing the checks, needed resources are counted.
        In the end check_quotas will decide whether enough resources are available.
        @param configurations: List of configurations (dicts)
        @param providers: List of providers
        """
        self.log = log
        self.configurations = configurations
        self.providers = providers
        if providers:
            self.required_resources_dict = {
                provider.cloud_specification['identifier']: {'total_cores': 0, 'floating_ips': 0, 'instances': 0,
                                                             'total_ram': 0, 'volumes': 0, 'volume_gigabytes': 0,
                                                             'snapshots': 0, 'backups': 0, 'backup_gigabytes': 0} for
                provider in providers}

    def validate(self):
        """
            Validation of the configuration file with the selected cloud provider.
            The validation steps are as follows:
            Check connection can be established
            Check provider uniqueness
            Check server group
            Check instances are available
            Check images and volumes are available
            Check network and subnet are available
            Check quotas
        @return:
        """
        success = bool(self.providers)
        success = validate_schema.validate_configurations(self.configurations, self.log) and success

        checks = [("master/vpn", self.check_master_vpn_worker), ("servergroup", self.check_server_group),
                  ("instances", self.check_instances), ("volumes", self.check_volumes), ("network", self.check_network),
                  ("quotas", self.check_quotas), ("sshPublicKeyFiles", self.check_ssh_public_key_files),
                  ("cloudYamls", self.check_clouds_yamls), ("nfs", self.check_nfs), ("global security groups",
                  self.check_configurations_security_groups)]
        if success:
            for check_name, check_function in checks:
                success = evaluate(check_name, check_function(), self.log) and success
        return success

    def _check_security_groups(self, provider, security_groups):
        success = True
        if not security_groups:
            return success
        for security_group_name in security_groups:
            security_group = provider.get_security_group(security_group_name)
            if not security_group:
                self.log.warning(f"Couldn't find security group {security_group} on "
                      f"cloud {provider.cloud_specification['identifier']}")
                success = False
            else:
                self.log.debug(f"Found {security_group_name} on cloud {provider.cloud_specification['identifier']}")
        return success

    def check_configurations_security_groups(self):
        self.log.info("Checking configurations security groups!")
        success = True
        for configuration, provider in zip(self.configurations, self.providers):
            success = self._check_security_groups(provider, configuration.get("securityGroups")) and success
        return success

    def check_master_vpn_worker(self):
        """
        Checks if first configuration has a masterInstance defined
        and every other configuration has a vpnInstance defined.
        If one is missing said provider wouldn't be reachable over the cluster, because no floating IP would be given.
        @return: True if first configuration has a masterInstance and every other a vpnInstance
        """
        self.log.info("Checking master/vpn")
        success = True
        if not self.configurations[0].get("masterInstance") or self.configurations[0].get("vpnInstance"):
            self.log.warning(f"{self.configurations[0].get('cloud')} has no master instance!")
            success = False
        for configuration in self.configurations[1:]:
            if not configuration.get("vpnInstance") or configuration.get("masterInstance"):
                self.log.warning(f"{configuration.get('cloud')} has no vpn instance!")
                success = False
        return success

    def check_provider_connections(self):
        """
        Checks if all providers are reachable
        @return: True if all providers are reachable
        """
        success = True
        providers_unconnectable = []
        for provider in self.providers:
            if not provider.conn:
                self.log.warning(f"API connection to {providers_unconnectable} not successful. "
                                 f"Please check your configuration for cloud "
                                 f"{provider.cloud_specification['identifier']}.")
                providers_unconnectable.append(provider.cloud_specification["identifier"])
        if providers_unconnectable:
            self.log.warning(f"Unconnected clouds: {providers_unconnectable}")
            success = False
        return success

    def check_instances(self):
        """
        Checks if all instances exist and image and instance-type are compatible
        @return: true if image and instance-type (flavor) exist for all instances and are compatible
        """
        self.log.info("Checking instance images and type")
        success = True
        for configuration, provider in zip(self.configurations, self.providers):
            try:
                self.required_resources_dict[provider.cloud_specification['identifier']]["floating_ips"] += 1
                master_instance = configuration.get("masterInstance")
                if master_instance:
                    success = self._check_security_groups(provider, master_instance.get("securityGroups")) and success
                    success = self.check_instance("masterInstance", master_instance,
                                                  provider) and success
                else:
                    vpn_instance = configuration["vpnInstance"]
                    success = self._check_security_groups(provider, vpn_instance.get("securityGroups")) and success
                    success = self.check_instance("vpnInstance", vpn_instance, provider) and success
                for worker in configuration.get("workerInstances", []):
                    success = self._check_security_groups(provider, worker.get("securityGroups")) and success
                    success = self.check_instance("workerInstance", worker, provider) and success
            except KeyError as exc:
                self.log.warning("Not found %s, but required on %s.", str(exc),
                                 provider.cloud_specification['identifier'])
                success = False
        return success

    def check_instance(self, instance_name, instance, provider):
        """
        Checks if instance image exists and whether it is compatible with the defined instance/server type (flavor).
        @param instance_name: containing name for logging purposes
        @param instance: dict containing image, type and count (count is not used)
        @param provider: provider
        @return: true if type and image compatible and existing
        """
        self.required_resources_dict[provider.cloud_specification['identifier']]["instances"] += instance.get(
            "count") or 1
        instance_image_id_or_name = instance["image"]
        try:
            instance_image = image_selection.select_image(provider, instance_image_id_or_name, self.log)
            self.log.info(f"Instance {instance_name} image: {instance_image_id_or_name} found on "
                          f"{provider.cloud_specification['identifier']}")
            instance_type = instance["type"]
        except ImageNotActiveException:
            active_images = '\n'.join(provider.get_active_images())
            self.log.warning(f"Instance {instance_name} image: {instance_image_id_or_name} not found among"
                             f" active images on {provider.cloud_specification['identifier']}.\n"
                             f"Available active images:\n{active_images}")
            return False
        return self.check_instance_type_image_combination(instance_type, instance_image, provider)

    def check_instance_type_image_combination(self, instance_type, instance_image, provider):
        """
        Checks, if enough ram, disk space for instance_image are provided by instance_type on provider.
        @param instance_type
        @param instance_image
        @param provider
        @return true, if enough resources available
        """
        success = True
        # check
        flavor = provider.get_flavor(instance_type)
        if not flavor:
            available_flavors = '\n'.join(provider.get_active_flavors())
            self.log.warning(f"Flavor {instance_type} does not exist on {provider.cloud_specification['identifier']}.\n"
                             f"Available flavors:\n{available_flavors}")
            return False

        type_max_disk_space = flavor["disk"]
        type_max_ram = flavor["ram"]
        image_min_disk_space = provider.get_image_by_id_or_name(instance_image)["min_disk"]
        image_min_ram = provider.get_image_by_id_or_name(instance_image)["min_ram"]
        for maximum, needed, thing in [(type_max_disk_space, image_min_disk_space, "disk space"),
                                       (type_max_ram, image_min_ram, "ram")]:
            success = has_enough(maximum, needed, f"Type {instance_type}", thing, self.log) and success
        # prepare check quotas
        self.required_resources_dict[provider.cloud_specification['identifier']]["total_ram"] += type_max_ram
        self.required_resources_dict[provider.cloud_specification['identifier']]["total_cores"] += flavor["vcpus"]
        return success

    def _check_volume(self, provider, volume, count):
        success = True
        if volume.get("exists"):
            if volume.get("name"):
                volume_object = provider.get_volume_by_id_or_name(volume["name"])
                if volume_object:
                    self.log.debug(
                        f"Found volume {volume['name']} on cloud "
                        f"{provider.cloud_specification['identifier']}.")
                else:
                    self.log.warning(
                        f"Couldn't find volume {volume['name']} on cloud "
                        f"{provider.cloud_specification['identifier']}. "
                        "No size added to resource requirements dict."
                    )
                    success = False
            else:
                self.log.warning(
                    f"Key exists is set, but no name is given for {volume}. "
                    "No size added to resource requirements dict.")
                success = False
        else:
            self.required_resources_dict[provider.cloud_specification['identifier']]["volumes"] += count

            if volume.get("snapshot"):
                snapshot_object = provider.get_volume_snapshot_by_id_or_name(volume["snapshot"])
                if snapshot_object:
                    self.log.debug(
                        f"Found snapshot {volume['snapshot']} on cloud "
                        f"{provider.cloud_specification['identifier']}.")
                    self.required_resources_dict[provider.cloud_specification['identifier']][
                        "volume_gigabytes"] += snapshot_object["size"] * count
                else:
                    self.log.warning(
                        f"Couldn't find snapshot {volume['snapshot']} on cloud "
                        f"{provider.cloud_specification['identifier']}. "
                        "No size added to resource requirements dict.")
                    success = False
            else:
                self.required_resources_dict[provider.cloud_specification['identifier']][
                    "volume_gigabytes"] += volume.get("size", 50) * count
        return success

    def check_volumes(self):  # pylint: disable=too-many-nested-blocks,too-many-branches
        """
        Checking if volume or snapshot exists for all volumes
        @return: True if all snapshot and volumes are found. Else false.
        """
        self.log.info("Checking volumes...")
        success = True
        for configuration, provider in zip(self.configurations,
                                           self.providers):
            master_volumes = (
                1, configuration.get("masterInstance", []) and configuration["masterInstance"].get("volumes",
                                                                                                   []))
            worker_volumes = configuration.get("workerInstances", (1, [])) and [
                (worker_instance.get("count", 1), worker_instance.get("volumes", [])) for
                worker_instance in configuration.get("workerInstances", [])]
            volume_groups = [master_volumes] + worker_volumes

            for count, volume_group in volume_groups:
                for volume in volume_group:
                    success = self._check_volume(provider, volume, count) and success
        return success

    def check_network(self):
        """
        Check if network (or subnet) is accessible
        @return True if any given network or subnet is accessible by provider
        """
        self.log.info("Checking network...")
        success = True
        for configuration, provider in zip(self.configurations, self.providers):
            network_name_or_id = configuration.get("network")
            subnet_name_or_id = configuration.get("subnet")
            if network_name_or_id:
                network = provider.get_network_by_id_or_name(network_name_or_id)
                if not network:
                    self.log.warning(
                        f"Network '{network_name_or_id}' not found on {provider.cloud_specification['identifier']}")
                    success = False
                else:
                    self.log.info(
                        f"Network '{network_name_or_id}' found on {provider.cloud_specification['identifier']}")
            elif subnet_name_or_id:
                subnet = provider.get_subnet_by_id_or_name(subnet_name_or_id)
                if not subnet:
                    self.log.warning(
                        f"Subnet '{subnet_name_or_id}' not found on {provider.cloud_specification['identifier']}")
                    success = False
                else:
                    self.log.info(f"Subnet '{subnet_name_or_id}' found")
            else:
                self.log.warning(
                    f"Neither network nor subnet given in configuration {provider.cloud_specification['identifier']}.")
                success = False
        return success

    def check_server_group(self):
        """
        @return: True if server group accessible
        """
        success = True
        for configuration, provider in zip(self.configurations, self.providers):
            server_group_name_or_id = configuration.get("serverGroup")
            if server_group_name_or_id:
                server_group = provider.get_server_group_by_id_or_name(server_group_name_or_id)
                if not server_group:
                    self.log.warning(f"ServerGroup '{server_group_name_or_id}' not found on "
                                     f"{provider.cloud_specification['identifier']}")
                    success = False
                else:
                    self.log.info(f"ServerGroup '{server_group_name_or_id}' found on "
                                  f"{provider.cloud_specification['identifier']}")
        return success

    def check_quotas(self):
        """
        Gets remaining resources from the provider and compares them to the needed resources.
        Needed resources are set during the other checks.
        Covered resources are: cores, floating_ips, instances, ram, volumes, volumeGigabytes, snapshots, backups and
        backupGigabytes. If a concrete provider implementation is unable to return remaining resources a maximum number
        is returned to make the check not fail because of the missing API implementation.
        @return: True if check succeeded. Else false.
        """
        self.log.info("Checking quotas")
        success = True
        self.log.info("required/available")
        for provider in self.providers:
            free_resources_dict = provider.get_free_resources()
            for key, value in self.required_resources_dict[provider.cloud_specification['identifier']].items():
                success = has_enough(free_resources_dict[key], value,
                                     f"Project {provider.cloud_specification['identifier']}", key, self.log) and success
        return success

    def check_ssh_public_key_files(self):
        """
        Checks if keys listed in the config exist
        @return: True if check succeeded. Else false.
        """
        success = True
        for configuration in self.configurations:
            for ssh_public_key_file in configuration.get("sshPublicKeyFiles") or []:
                if not os.path.isfile(ssh_public_key_file):
                    self.log.warning(
                        f"sshPublicKeyFile '{ssh_public_key_file}' not found on {configuration.get('cloud')}")
                    success = False
                else:
                    self.log.info(f"sshPublicKeyFile '{ssh_public_key_file}' found on {configuration.get('cloud')}")
                    success = evaluate_ssh_public_key_file_security(ssh_public_key_file, self.log) and success
        return success

    def check_clouds_yamls(self):
        """
        Checks if every cloud in clouds_yaml is valid
        @return: True if all clouds are valid
        """
        self.log.info("Checking cloud specifications...")
        success = True
        cloud_specifications = configuration_handler.get_cloud_specifications(self.configurations, self.log)
        for index, cloud_specification in enumerate(cloud_specifications):
            if not check_cloud_yaml(cloud_specification, self.log):
                success = False
                self.log.warning(f"Cloud specification {cloud_specification.get('identifier', index)} is faulty.")
            success = check_clouds_yaml_security(self.log) and success
        return success

    def check_nfs(self):
        """
        Checks whether nfsshares => nfs holds and logs if failed. Returns True in every case as it is not fatale.
        @return: True
        """
        self.log.info("Checking nfs...")
        master_configuration = self.configurations[0]
        nfs_shares = master_configuration.get("nfsShares")
        nfs = master_configuration.get("nfs")
        if nfs_shares and not nfs:
            success = False
            self.log.warning("nfsShares exist, but nfs is False.")
        else:
            success = True
        return success

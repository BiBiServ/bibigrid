"""
Holds the abstract class Provider
"""
from abc import ABC, abstractmethod


class Provider(ABC):  # pylint: disable=too-many-public-methods
    """
    See in detailed return value information in tests>provider>test_Provider.
    Make sure to register your newly implemented provider in provider_handler: name:class
    This will automatically register it for testing when startupTests main is called.
    """
    NAME = "Provider"

    class QuotaExceededException(Exception):
        """
        Just a renamed Exception.
        """

    def __init__(self, cloud_specification):
        """
        Call necessary methods to create a connection and save cloud_specification data as needed.
        """
        self.cloud_specification = cloud_specification  # contains sensitive information!

    @abstractmethod
    def create_application_credential(self, name=None):
        """
        Creates an application credential with name name
        @param name: Name of new application credential
        @return: the application credential dictionary
        """

    @abstractmethod
    def delete_application_credential_by_id_or_name(self, ac_id_or_name):
        """
        Deletes existing application credential by id or name and returns true.
        If application credential not found it returns false.
        @param ac_id_or_name: application credential id or name
        @return: True if deleted else false
        """

    @abstractmethod
    def get_image_by_id_or_name(self, image_id_or_name):
        """
        Returns image that has id or name image_id_or_name
        @param image_id_or_name: identifier
        @return: said image (dict) or none if not found
        """

    @abstractmethod
    def get_flavor(self, instance_type):
        """
        Returns flavor that has id or name flavor_id_or_name
        @param instance_type: identifier
        @return: said flavor (dict) or none if not found
        """

    @abstractmethod
    def get_volume_snapshot_by_id_or_name(self, snapshot_id_or_name):
        """
        Returns snapshot that has id or name snapshot_id_or_name
        @param snapshot_id_or_name: identifier
        @return: said snapshot (dict) or none if not found
        """

    @abstractmethod
    def get_network_by_id_or_name(self, network_id_or_name):
        """
        Returns network that has id or name network_id_or_name
        @param network_id_or_name: identifier
        @return: said network (dict) or none if not found
        """

    @abstractmethod
    def get_subnet_by_id_or_name(self, subnet_id_or_name):
        """
        Returns subnet that has id or name subnet_id_or_name
        @param subnet_id_or_name: identifier
        @return: said subnet (dict) or none if not found
        """

    @abstractmethod
    def list_servers(self):
        """
        Returns a list of all servers on logged in provider
        @return: said list of servers or empty list if none found
        """

    @abstractmethod
    def create_server(self, name, flavor, image, network, key_name=None, wait=True, volumes=None, security_groups=None,
                      boot_volume=None, boot_from_volume=False,
                      terminate_boot_volume=False, volume_size=50):  # pylint: disable=too-many-arguments
        """
        Creates a new server and waits for it to be accessible if wait=True. If volumes are given, they are attached.
        Returns said server (dict)
        @param volume_size: Size of boot volume if set. Defaults to 50.
        @param terminate_boot_volume: if True, boot volume gets terminated on server termination
        @param boot_from_volume: if True, a boot volume is created from the image
        @param boot_volume: if a volume is given, that volume is used as the boot volume
        @param name: name (str)
        @param flavor: flavor/type (str)
        @param image: image/bootable-medium (str)
        @param network: network (str)
        @param key_name: (str)
        @param wait: (bool)
        @param volumes: List of volumes (list (str))
        @param security_groups: List of security_groups list (str)
        @return: server (dict)
        """

    @abstractmethod
    def delete_server(self, name_or_id, delete_ips=True):
        """
        Deletes server and floating_ip as well if delete_ips is true. The resource is then free again
        @param name_or_id:
        @param delete_ips:
        @return: True if delete succeeded, False otherwise
        """

    @abstractmethod
    def delete_keypair(self, key_name):
        """
        Deletes keypair with key_name
        @param key_name: (str)
        @return: True if delete succeeded, False otherwise
        """

    @abstractmethod
    def get_server_group_by_id_or_name(self, server_group_id_or_name):
        """
        Returns server_group that has id or name server_group_id_or_name
        @param server_group_id_or_name: identifier
        @return: said server_group (dict) or none if not found
        """

    @abstractmethod
    def close(self):
        """
        Closes connection
        @return:
        """

    @abstractmethod
    def create_keypair(self, name, public_key):
        """
        Creates a new keypair with name and public_key
        @param name: name of new keypair
        @param public_key: public_key of new keypair
        @return:
        """

    @abstractmethod
    def get_network_id_by_subnet(self, subnet):
        """
        Gets network_id by subnet
        @param subnet: id (str)
        @return: (str)
        """

    @abstractmethod
    def get_subnet_ids_by_network(self, network):
        """
        Gets subnet_ids (list (str)) by network_id
        @param network: id (str)
        @return: subnet_ids (list (str))
        """

    @abstractmethod
    def get_free_resources(self):
        """
        Gets free resources. If a resource cannot be determined, assume maximum is free.
        @return: Dictionary containing the free resources
        """

    @abstractmethod
    def get_volume_by_id_or_name(self, name_or_id):
        """
        Returns volume that has id or name name_or_id
        @param name_or_id: identifier
        @return: said volume (dict) or none if not found
        """

    @abstractmethod
    def create_volume_from_snapshot(self, snapshot_name_or_id):
        """
        Creates a volume from snapshot.
        @param snapshot_name_or_id: name or id of snapshot
        @return: id of created volume or none if failed
        """

    @abstractmethod
    def get_external_network(self, network_name_or_id):
        """
        Finds router interface with network id equal to given network and by that the external network.
        @param network_name_or_id: Name or id of network
        @return: Corresponding external network
        """

    @abstractmethod
    def attach_available_floating_ip(self, network=None, server=None):
        """
        Get a floating IP from a network or a pool and attach it to the server
        @param network:
        @param server:
        @return:
        """

    @abstractmethod
    def get_images(self):
        """
        Get a generator able ot generate all images
        @return: A generator able ot generate all images
        """

    @abstractmethod
    def get_flavors(self):
        """
        Get a generator able ot generate all flavors
        @return: A generator able ot generate all flavors
        """

    def get_active_images(self):
        """
        Return a list of active images.
        @return: A list of active images.
        """
        return [image["name"] for image in self.get_images() if image["status"].lower() == "active"]

    def get_active_flavors(self):
        return [flavor["name"] for flavor in self.get_flavors() if
                "legacy" not in flavor["name"].lower() and "deprecated" not in flavor["name"].lower()]

    @abstractmethod
    def set_allowed_addresses(self, id_or_ip, allowed_address_pairs):
        """
        Set allowed address (or CIDR) for the given network interface/port
        @param id_or_ip: id or ipv4 ip-address of the port/interface
        @param allowed_address_pairs: a list of allowed address pairs. For example:
                [{
                    "ip_address": "23.23.23.1",
                    "mac_address": "fa:16:3e:c4:cd:3f"
                }]
        @return:
        """

    @abstractmethod
    def create_security_group(self, name, rules):
        """
        Create a security group and add given rules
        @param name:  Name of the security group to be created
        @param rules: List of firewall rules to be added
        @return: id of created security group
        """

    @abstractmethod
    def delete_security_group(self, name_or_id):
        """
        Delete a security group
        @param name_or_id : Name or id of the security group to be deleted
        @return: True if delete succeeded, False otherwise.
        """

    @abstractmethod
    def append_rules_to_security_group(self, name_or_id, rules):
        """
        Append firewall rules to given security group
        @param name_or_id:
        @param rules:
        @return:
        """

    @abstractmethod
    def get_security_group(self, name_or_id):
        """
        Returns security group if found else None.
        @param name_or_id:
        @return:
        """

    @abstractmethod
    def create_volume(self, *, name, size, wait=True, volume_type=None, description=None):
        """
        Creates a volume
        @param name: name of the created volume
        @param size: size of the created volume in GB
        @param wait: if true waits for volume to be created
        @param volume_type: depends on the location, but for example NVME or HDD
        @param description: a non-functional description to help dashboard users
        @return: the created volume
        """

    @abstractmethod
    def get_server(self, name_or_id):
        """
        Returns server if found else None.
        @param name_or_id:
        @return:
        """

    @abstractmethod
    def delete_volume(self, name_or_id):
        """
        Deletes the volume that has name_or_id.
        @param name_or_id:
        @return: True if deletion was successful, else False
        """

    @abstractmethod
    def list_volumes(self):
        """
        Returns a list of all volumes on the provider.
        @return: list of volumes
        """

    def get_mount_info_from_server(self, server):
        """
        @param server: server to get the attachment list from
        @return: list of dicts containing name and device node of all attached volumes
        """
        volumes = []
        for server_volume in server["volumes"]:
            volume = self.get_volume_by_id_or_name(server_volume["id"])
            for attachment in volume["attachments"]:
                if attachment["server_id"] == server["id"]:
                    volumes.append({"name": volume["name"], "device": attachment["device"]})
                    break
        return volumes

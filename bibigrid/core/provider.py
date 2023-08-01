"""
Holds the abstract class Provider
"""


class Provider:  # pylint: disable=too-many-public-methods
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
        self.cloud_specification["identifier"] = self.cloud_specification['identifier']

    def create_application_credential(self, name=None):
        """
        Creates an application credential with name name
        :param name: Name of new application credential
        :return: the application credential dictionary
        """

    def delete_application_credential_by_id_or_name(self, ac_id_or_name):
        """
        Deletes existing application credential by id or name and returns true.
        If application credential not found it returns false.
        :param ac_id_or_name: application credential id or name
        :return: True if deleted else false
        """

    def get_image_by_id_or_name(self, image_id_or_name):
        """
        Returns image that has id or name image_id_or_name
        :param image_id_or_name: identifier
        :return: said image (dict) or none if not found
        """

    def get_flavor(self, instance_type):
        """
        Returns flavor that has id or name flavor_id_or_name
        :param instance_type: identifier
        :return: said flavor (dict) or none if not found
        """

    def get_volume_snapshot_by_id_or_name(self, snapshot_id_or_name):
        """
        Returns snapshot that has id or name snapshot_id_or_name
        :param snapshot_id_or_name: identifier
        :return: said snapshot (dict) or none if not found
        """

    def get_network_by_id_or_name(self, network_id_or_name):
        """
        Returns network that has id or name network_id_or_name
        :param network_id_or_name: identifier
        :return: said network (dict) or none if not found
        """

    def get_subnet_by_id_or_name(self, subnet_id_or_name):
        """
        Returns subnet that has id or name subnet_id_or_name
        :param subnet_id_or_name: identifier
        :return: said subnet (dict) or none if not found
        """

    def list_servers(self):
        """
        Returns a list of all servers on logged in provider
        :return: said list of servers or empty list if none found
        """

    def create_server(self, name, flavor, image, network, key_name=None, wait=True,
                      volumes=None, security_groups=None):  # pylint: disable=too-many-arguments
        """
        Creates a new server and waits for it to be accessible if wait=True. If volumes are given, they are attached.
        Returns said server (dict)
        :param name: name (str)
        :param flavor: flavor/type (str)
        :param image: image/bootable-medium (str)
        :param network: network (str)
        :param key_name: (str)
        :param wait: (bool)
        :param volumes: List of volumes (list (str))
        :param security_groups: List of security_groups list (str)
        :return: server (dict)
        """

    def delete_server(self, name_or_id, delete_ips=True):
        """
        Deletes server and floating_ip as well if delete_ips is true. The resource is then free again
        :param name_or_id:
        :param delete_ips:
        :return: True if delete succeeded, False otherwise
        """

    def delete_keypair(self, key_name):
        """
        Deletes keypair with key_name
        :param key_name: (str)
        :return: True if delete succeeded, False otherwise
        """

    def get_server_group_by_id_or_name(self, server_group_id_or_name):
        """
        Returns server_group that has id or name server_group_id_or_name
        :param server_group_id_or_name: identifier
        :return: said server_group (dict) or none if not found
        """

    def close(self):
        """
        Closes connection
        :return:
        """

    def create_keypair(self, name, public_key):
        """
        Creates a new keypair with name name and public_key public_key
        :param name: name of new keypair
        :param public_key: public_key of new keypair
        :return:
        """

    def get_network_id_by_subnet(self, subnet):
        """
        Gets network_id by subnet
        :param subnet: id (str)
        :return: (str)
        """

    def get_subnet_ids_by_network(self, network):
        """
        Gets subnet_ids (list (str)) by network_id
        :param network: id (str)
        :return: subnet_ids (list (str))
        """

    def get_free_resources(self):
        """
        Gets free resources. If a resource cannot be determined, assume maximum is free.
        :return: Dictionary containing the free resources
        """

    def get_volume_by_id_or_name(self, name_or_id):
        """
        Returns volume that has id or name name_or_id
        :param name_or_id: identifier
        :return: said volume (dict) or none if not found
        """

    def create_volume_from_snapshot(self, snapshot_name_or_id):
        """
        Creates a volume from snapshot.
        :param snapshot_name_or_id: name or id of snapshot
        :return: id of created volume or none if failed
        """

    def get_external_network(self, network_name_or_id):
        """
        Finds router interface with network id equal to given network and by that the external network.
        :param network_name_or_id: Name or id of network
        :return: Corresponding external network
        """

    def add_auto_ip(self, server, wait=False, timeout=60, reuse=True):
        """
        Add a floating IP to a server.
        Will reuse floating ips or create a new one if no floating-ip is down.
        :param server: the server that said floating ip will be attached to
        :param wait: wait for floating-ip to be assigned
        :param timeout: when to accept failing
        :param reuse: if False will just create a new floating-ip and not reuse an existing down one
        :return: the floating-ip
        """

    def attach_available_floating_ip(self, network=None, server=None):
        """
        Get a floating IP from a network or a pool and attach it to the server
        :param network:
        :param server:
        :return:
        """

    def get_images(self):
        """
        Get a generator able ot generate all images
        @return: A generator able ot generate all images
        """

    def get_flavors(self):
        """
        Get a generator able ot generate all flavors
        @return: A generator able ot generate all flavors
        """

    def get_active_images(self):
        """
        Return a list of active images.
        :return: A list of active images.
        """
        return [image["name"] for image in self.get_images() if image["status"].lower() == "active"]

    def get_active_flavors(self):
        return [flavor["name"] for flavor in self.get_flavors()
                if "legacy" not in flavor["name"].lower() and "deprecated" not in flavor["name"].lower()]

    def set_allowed_addresses(self, id_or_ip, allowed_address_pairs):
        """
        Set allowed address (or CIDR) for the given network interface/port
        :param id_or_ip: id or ipv4 ip-address of the port/interface
        :param allowed_address_pairs: a list of allowed address pairs. For example:
                [{
                    "ip_address": "23.23.23.1",
                    "mac_address": "fa:16:3e:c4:cd:3f"
                }]
        :return:
        """

    def create_security_group(self, name, rules):
        """
        Create a security group and add given rules
        :param name:  Name of the security group to be created
        :param rules: List of firewall rules to be added
        :return: id of created security group
        """

    def delete_security_group(self, name_or_id):
        """
        Delete a security group
        :param name_or_id : Name or Id of the security group to be deleted
        :return: True if delete succeeded, False otherwise.

        """

    def append_rules_to_security_group(self, name_or_id, rules):
        """
        Append firewall rules to given security group
        :param name_or_id:
        :param rules:
        :return:
        """

    def get_mount_info_from_server(self, server):
        volumes = []
        for server_volume in server["volumes"]:
            volume = self.get_volume_by_id_or_name(server_volume["id"])
            for attachment in volume["attachments"]:
                if attachment["server_id"] == server["id"]:
                    volumes.append({"name": volume["name"], "device": attachment["device"]})
                    break
        return volumes

"""
Module containing integration and unit tests regarding the provider
"""

import logging
import os
import unittest

from bibigrid.core import startup
from bibigrid.core.utility import image_selection
import bibigrid.core.utility.paths.basic_path as bP
from bibigrid.core.utility.handler import configuration_handler
from bibigrid.core.utility.handler import provider_handler
from bibigrid.models.exceptions import ExecutionException

SERVER_KEYS = {'id', 'name', 'flavor', 'image', 'block_device_mapping', 'location', 'volumes', 'has_config_drive',
               'host_id', 'progress', 'disk_config', 'power_state', 'task_state', 'vm_state', 'launched_at',
               'terminated_at', 'hypervisor_hostname', 'instance_name', 'user_data', 'host', 'hostname', 'kernel_id',
               'launch_index', 'ramdisk_id', 'reservation_id', 'root_device_name', 'scheduler_hints', 'security_groups',
               'created_at', 'accessIPv4', 'accessIPv6', 'addresses', 'adminPass', 'created', 'description', 'key_name',
               'metadata', 'networks', 'personality', 'private_v4', 'public_v4', 'public_v6', 'server_groups', 'status',
               'updated', 'user_id', 'tags', 'interface_ip', 'properties', 'hostId', 'config_drive', 'project_id',
               'tenant_id', 'region', 'cloud', 'az', 'OS-DCF:diskConfig', 'OS-EXT-AZ:availability_zone',
               'OS-SRV-USG:launched_at', 'OS-SRV-USG:terminated_at', 'OS-EXT-STS:task_state', 'OS-EXT-STS:vm_state',
               'OS-EXT-STS:power_state', 'os-extended-volumes:volumes_attached', 'OS-EXT-SRV-ATTR:ramdisk_id',
               'max_count', 'trusted_image_certificates', 'OS-SCH-HNT:scheduler_hints',
               'OS-EXT-SRV-ATTR:reservation_id', 'OS-EXT-SRV-ATTR:host', 'locked', 'host_status',
               'OS-EXT-SRV-ATTR:hypervisor_hostname', 'OS-EXT-SRV-ATTR:launch_index',
               'OS-EXT-SRV-ATTR:root_device_name', 'OS-EXT-SRV-ATTR:instance_name', 'OS-EXT-SRV-ATTR:user_data',
               'min_count', 'OS-EXT-SRV-ATTR:kernel_id', 'OS-EXT-SRV-ATTR:hostname'}
FLOATING_IP_KEYS = {'attached', 'fixed_ip_address', 'floating_ip_address', 'id', 'location', 'network', 'port',
                    'router', 'status', 'created_at', 'updated_at', 'description', 'revision_number', 'properties',
                    'port_id', 'router_id', 'project_id', 'tenant_id', 'floating_network_id', 'port_details',
                    'dns_domain', 'dns_name', 'port_forwardings', 'tags'}
SUBNET_KEYS = {'id', 'name', 'tenant_id', 'network_id', 'ip_version', 'subnetpool_id', 'enable_dhcp', 'ipv6_ra_mode',
               'ipv6_address_mode', 'gateway_ip', 'cidr', 'allocation_pools', 'host_routes', 'dns_nameservers',
               'description', 'service_types', 'tags', 'created_at', 'updated_at', 'revision_number', 'project_id'}
NETWORK_KEYS = {'id', 'name', 'tenant_id', 'admin_state_up', 'mtu', 'status', 'subnets', 'shared',
                'availability_zone_hints', 'availability_zones', 'ipv4_address_scope', 'ipv6_address_scope',
                'router:external', 'description', 'port_security_enabled', 'dns_domain', 'tags', 'created_at',
                'updated_at', 'revision_number', 'project_id'}

FLAVOR_KEYS = {'links', 'name', 'description', 'disk', 'is_public', 'ram', 'vcpus', 'swap', 'ephemeral', 'is_disabled',
               'rxtx_factor', 'extra_specs', 'id', 'location'}

IMAGE_KEYS = {'location', 'created_at', 'updated_at', 'checksum', 'container_format', 'direct_url', 'disk_format',
              'file', 'id', 'name', 'owner', 'tags', 'status', 'min_ram', 'min_disk', 'size', 'virtual_size',
              'is_protected', 'locations', 'properties', 'is_public', 'visibility', 'description',
              'owner_specified.openstack.md5', 'owner_specified.openstack.object', 'owner_specified.openstack.sha256',
              'os_hidden', 'os_hash_algo', 'os_hash_value', 'os_distro', 'os_version', 'schema', 'protected',
              'metadata', 'created', 'updated', 'minDisk', 'minRam', 'stores'}

SNAPSHOT_KEYS = {'id', 'created_at', 'updated_at', 'name', 'description', 'volume_id', 'status', 'size', 'metadata',
                 'os-extended-snapshot-attributes:project_id', 'os-extended-snapshot-attributes:progress'}

VOLUME_KEYS = {'location', 'id', 'name', 'description', 'size', 'attachments', 'status', 'migration_status', 'host',
               'replication_driver', 'replication_status', 'replication_extended_status', 'snapshot_id', 'created_at',
               'updated_at', 'source_volume_id', 'consistencygroup_id', 'volume_type', 'metadata', 'is_bootable',
               'is_encrypted', 'can_multiattach', 'properties', 'display_name', 'display_description', 'bootable',
               'encrypted', 'multiattach', 'availability_zone', 'source_volid', 'user_id',
               'os-vol-tenant-attr:tenant_id'}

FREE_RESOURCES_KEYS = {'total_cores', 'floating_ips', 'instances', 'total_ram', 'volumes', 'volume_gigabytes',
                       'snapshots', 'backups', 'backup_gigabytes'}

KEYPAIR = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDORPauyW3O7M4Uk8/Qo557h2zxd9fwByljG9S1/zHKIEzOMcOBb7WUSmyNa5XHh5IB0/" \
          "BTsQvSag/O9IAhax2wlp9A2za6EkALYiRdEXeGOMNORw8yylRBqzLluKTErZ5sKYxENf1WGHsE3ifzct0G/moEPmIkixTHR9fZrZgOzQwj" \
          "4bgJXhgQT8wxpc8FwWncvDSazZ/OAefXKh16Dz8dVz2VbMbYEUMY+XXqZxcnHwJABIpU1mrJV7h1F4DW+E8eUF1b6UNQRibX8VJ11V1mq3" \
          "9zMV9Az6W2ZOR6OXjDXK2r6P8y07+9Lh0rrwzeeZMYF17ACZbxIu8crTCZF0Lr6NtX+KWfdT6usUyFcNwuktIvUYv3ylP/7wcQlaPl0g1F" \
          "MFbUTTukAiDf4jAgvJkg7ayE0MPapGpI/OhSK2gyN45VAzs2m7uykun87B491JagZ57qr16vt8vxGYpFCEe8QqAcrUszUPqyPrb0auA8bz" \
          "jO8S41Kx8FfG+7eTu4dQ0= user"

CONFIGURATIONS = configuration_handler.read_configuration(logging,
                                                          os.path.join(bP.ROOT_PATH,
                                                                       "resources/tests/bibigrid_test.yaml"))
PROVIDERS = provider_handler.get_providers(CONFIGURATIONS, logging)


class ProviderServer:
    """
        Class for testing create_server method
    """

    def __init__(self, provider, name, configuration, key_name=None):
        self.provider = provider
        self.name = name
        self.log = startup.LOG
        configuration["image"] = image_selection.select_image(provider=provider, image=configuration["image"],
                                                              log=self.log)
        self.server_dict = provider.create_server(name=self.name, flavor=configuration["flavor"],
                                                  image=configuration["image"], network=configuration["network"],
                                                  key_name=key_name, security_groups=[])

    def __enter__(self):
        return self.server_dict

    def __exit__(self, not_type, value, traceback):  # type
        self.provider.delete_server(name_or_id=self.name)


class TestProvider(unittest.TestCase):
    """
    Class to unittest provider
    """

    def test_get_free_resources(self):
        for provider in PROVIDERS:
            with self.subTest(provider.NAME):
                free_dict = provider.get_free_resources()
                self.assertEqual(FREE_RESOURCES_KEYS, set(free_dict.keys()))
                for key, value in free_dict.items():
                    if key != "floating_ips":
                        self.assertLessEqual(0, value)

    def test_server_start_type_error(self):
        for provider, configuration in zip(PROVIDERS, CONFIGURATIONS):
            with self.subTest(provider.NAME):
                with self.assertRaises(TypeError):
                    provider.create_server(name="name", flavor=configuration["flavor"],
                                           network=configuration["network"], security_groups=[])
                with self.assertRaises(TypeError):
                    provider.create_server(name="name", image=configuration["image"], network=configuration["network"],
                                           security_groups=[])
                with self.assertRaises(TypeError):
                    provider.create_server(flavor=configuration["flavor"], image=configuration["image"],
                                           security_groups=[], network=configuration["network"])
                with self.assertRaises(TypeError):
                    provider.create_server(name="name", flavor=configuration["flavor"], image=configuration["image"],
                                           security_groups=[])

    def test_server_start_attribute_error(self):
        for provider, configuration in zip(PROVIDERS, CONFIGURATIONS):
            with self.subTest(provider.NAME):
                with self.assertRaises(ExecutionException):
                    provider.create_server(name="name", image="ERROR", flavor=configuration["flavor"],
                                           network=configuration["network"], security_groups=[])
                with self.assertRaises(ExecutionException):
                    provider.create_server(name="name", flavor="ERROR", image=configuration["image"],
                                           network=configuration["network"], security_groups=[])
                with self.assertRaises(ExecutionException):
                    provider.create_server(name="name", flavor=configuration["flavor"], image=configuration["image"],
                                           network="ERROR", security_groups=[])
                with self.assertRaises(ExecutionException):
                    provider.create_server(name="name", flavor=configuration["flavor"], image=configuration["image"],
                                           network=configuration["network"], key_name="ERROR", security_groups=[])

    def test_create_keypair_create_delete_false_delete(self):
        for provider in PROVIDERS:
            with self.subTest(provider.NAME):
                provider.create_keypair("bibigrid_test_keypair", KEYPAIR)
                self.assertTrue(provider.delete_keypair("bibigrid_test_keypair"))
                self.assertFalse(provider.delete_keypair("bibigrid_test_keypair"))

    def test_active_server_methods(self):
        for provider, configuration in zip(PROVIDERS, CONFIGURATIONS):
            provider.create_keypair("bibigrid_test_keypair", KEYPAIR)
            with self.subTest(provider.NAME):
                with ProviderServer(provider, "bibigrid_test_server", configuration,
                                    "bibigrid_test_keypair") as provider_server:
                    floating_ip = provider.attach_available_floating_ip(
                        provider.get_external_network(configuration["network"]), provider_server)
                    server_list = provider.list_servers()
                self.assertEqual(SERVER_KEYS, set(provider_server.keys()))
                self.assertEqual("bibigrid_test_keypair", provider_server["key_name"])
                self.assertEqual(FLOATING_IP_KEYS, set(floating_ip.keys()))
                self.assertTrue([server for server in server_list if
                                 server["name"] == "bibigrid_test_server" and server[
                                     "public_v4"] == floating_ip.floating_ip_address])
            provider.delete_keypair("bibigrid_test_keypair")

    def test_get_external_network(self):
        for provider, configuration in zip(PROVIDERS, CONFIGURATIONS):
            with self.subTest(provider.NAME):
                print("FIRE", provider.get_external_network(configuration["network"]))
                self.assertTrue(provider.get_external_network(configuration["network"]))
                with self.assertRaises(TypeError):
                    provider.get_external_network("ERROR")

    def test_get_network_get_subnet(self):
        for provider, configuration in zip(PROVIDERS, CONFIGURATIONS):
            with self.subTest(provider.NAME):
                network = provider.get_network_by_id_or_name(configuration["network"])
                self.assertEqual(NETWORK_KEYS, set(network.keys()))
                subnet_id = provider.get_subnet_ids_by_network(network["id"])[0]
                self.assertEqual(SUBNET_KEYS, set(provider.get_subnet_by_id_or_name(subnet_id).keys()))
                network2 = provider.get_network_id_by_subnet(subnet_id)
                self.assertEqual(network2, network["id"])

    def test_get_network_get_subnet_mismatch(self):
        for provider in PROVIDERS:
            with self.subTest(provider.NAME):
                self.assertIsNone(provider.get_network_by_id_or_name("NONE"))

    def test_get_subnet_by_name_or_id_mismatch(self):
        for provider in PROVIDERS:
            with self.subTest(provider.NAME):
                self.assertIsNone(provider.get_subnet_by_id_or_name("NONE"))

    def test_get_subnet_by_network_mismatch(self):
        for provider in PROVIDERS:
            with self.subTest(provider.NAME):
                self.assertIsNone(provider.get_subnet_ids_by_network("NONE"))

    def test_get_server_group_mismatch(self):
        for provider in PROVIDERS:
            with self.subTest(provider.NAME):
                self.assertIsNone(provider.get_server_group_by_id_or_name("NONE"))

    def test_get_flavor_detail_mismatch(self):
        for provider in PROVIDERS:
            with self.subTest(provider.NAME):
                self.assertIsNone(provider.get_flavor("NONE"))

    def test_get_flavor_detail(self):
        for provider, configuration in zip(PROVIDERS, CONFIGURATIONS):
            with self.subTest(provider.NAME):
                self.assertEqual(FLAVOR_KEYS, set(provider.get_flavor(configuration["flavor"]).keys()))

    def test_get_image(self):
        for provider, configuration in zip(PROVIDERS, CONFIGURATIONS):
            with self.subTest(provider.NAME):
                self.assertEqual(IMAGE_KEYS, set(provider.get_image_by_id_or_name(configuration["image"]).keys()))

    def test_get_image_mismatch(self):
        for provider in PROVIDERS:
            with self.subTest(provider.NAME):
                self.assertIsNone(provider.get_image_by_id_or_name("NONE"))

    if CONFIGURATIONS[0].get("snapshotImage"):
        def test_get_snapshot(self):
            for provider, configuration in zip(PROVIDERS, CONFIGURATIONS):
                with self.subTest(provider.NAME):
                    self.assertEqual(SNAPSHOT_KEYS, set(provider.get_volume_snapshot_by_id_or_name(
                        configuration["snapshotImage"]).keys()))

        def test_create_volume_from_snapshot(self):
            for provider, configuration in zip(PROVIDERS, CONFIGURATIONS):
                with self.subTest(provider.NAME):
                    volume_id = provider.create_volume_from_snapshot(configuration["snapshotImage"])
                    print(volume_id)
                    volume = provider.get_volume_by_id_or_name(volume_id)
                    self.assertEqual(VOLUME_KEYS, set(volume.keys()))

"""
Tests for validate configuration
"""

import os
from unittest import TestCase
from unittest.mock import Mock, patch, MagicMock, call

from bibigrid.core.utility import validate_configuration
from bibigrid.models.exceptions import ImageNotActiveException


class TestValidateConfiguration(TestCase):
    """
    Class to test ValidateConfiguration
    """

    # pylint: disable=R0904
    def test_check_provider_data_count(self):
        provider_data_1 = {"PROJECT_ID": "abcd", "PROJECT_NAME": "1234"}
        provider_data_2 = {"PROJECT_ID": "9999", "PROJECT_NAME": "9999"}
        self.assertTrue(validate_configuration.check_provider_data([provider_data_1, provider_data_2], 2, log=Mock()))
        self.assertFalse(validate_configuration.check_provider_data([provider_data_1, provider_data_2], 3, log=Mock()))
        self.assertTrue(validate_configuration.check_provider_data([], 0, log=Mock()))

    def test_check_provider_data_unique(self):
        provider_data_1 = {"PROJECT_ID": "abcd", "PROJECT_NAME": "1234"}
        provider_data_2 = {"PROJECT_ID": "9999", "PROJECT_NAME": "9999"}
        self.assertTrue(validate_configuration.check_provider_data([provider_data_1, provider_data_2], 2, log=Mock()))
        self.assertFalse(validate_configuration.check_provider_data([provider_data_1, provider_data_1], 2, log=Mock()))
        self.assertTrue(validate_configuration.check_provider_data([], 0, log=Mock()))

    @patch("bibigrid.core.utility.image_selection.select_image")
    def test_check_master_vpn_worker_ordered(self, mock_select_image):  # pylint: disable=unused-argument
        master = {"masterInstance": "Value"}
        vpn = {"vpnInstance": "Value"}
        vpn_master = {}
        vpn_master.update(master)
        vpn_master.update(vpn)
        v_c = validate_configuration.ValidateConfiguration(providers=None, configurations=[master], log=Mock())
        self.assertTrue(v_c.check_master_vpn_worker())
        v_c.configurations = [master, vpn]
        self.assertTrue(v_c.check_master_vpn_worker())
        v_c.configurations = [vpn]
        self.assertFalse(v_c.check_master_vpn_worker())
        v_c.configurations = [master, master]
        self.assertFalse(v_c.check_master_vpn_worker())

    def test_check_master_vpn_worker_unique(self):
        master = {"masterInstance": "Value"}
        vpn = {"vpnInstance": "Value"}
        vpn_master = {}
        vpn_master.update(master)
        vpn_master.update(vpn)
        v_c = validate_configuration.ValidateConfiguration(providers=None, configurations=[vpn_master], log=Mock())
        self.assertFalse(v_c.check_master_vpn_worker())
        v_c.configurations = [master, vpn_master]
        self.assertFalse(v_c.check_master_vpn_worker())

    def test_evaluate(self):
        self.assertTrue(validate_configuration.evaluate("some", True, log=Mock()))
        self.assertFalse(validate_configuration.evaluate("some", False, log=Mock()))

    def test_check_provider_connection(self):
        mock = MagicMock()
        mock.conn = False
        v_c = validate_configuration.ValidateConfiguration(providers=[mock], configurations=None, log=Mock())
        self.assertFalse(v_c.check_provider_connections())
        mock.conn = True
        self.assertTrue(v_c.check_provider_connections())

    def test_check_instances_master(self):
        v_c = validate_configuration.ValidateConfiguration(providers=["31"], configurations=[{"masterInstance": "42"}],
                                                           log=Mock())
        with patch.object(v_c, "check_instance") as mock:
            v_c.check_instances()
            mock.assert_called_with("masterInstance", "42", "31")

    def test_check_instances_vpn(self):
        v_c = validate_configuration.ValidateConfiguration(providers=["31"], configurations=[{"vpnInstance": "42"}],
                                                           log=Mock())
        with patch.object(v_c, "check_instance") as mock:
            v_c.check_instances()
            mock.assert_called_with("vpnInstance", "42", "31")

    def test_check_instances_vpn_worker(self):
        v_c = validate_configuration.ValidateConfiguration(providers=["31"], configurations=[
            {"masterInstance": "42", "workerInstances": ["42"]}], log=Mock())
        with patch.object(v_c, "check_instance") as mock:
            v_c.check_instances()
            mock.assert_called_with("workerInstance", "42", "31")

    def test_check_instances_vpn_master_missing(self):
        v_c = validate_configuration.ValidateConfiguration(providers=["31"], configurations=[{}], log=Mock())
        self.assertFalse(v_c.check_instances())
        v_c = validate_configuration.ValidateConfiguration(providers=["31"],
                                                           configurations=[{"workerInstances": ["42"]}], log=Mock())
        self.assertFalse(v_c.check_instances())

    def test_check_instances_vpn_master_count(self):
        for i in range(1, 4):
            v_c = validate_configuration.ValidateConfiguration(providers=["31"] * i,
                                                               configurations=[{"masterInstance": {"count": 1}}] + [
                                                                   {"vpnInstance": {"count": 1}}] * (i - 1), log=Mock())
            # with patch.object(v_c, "check_instance") as mock:
            with patch.object(v_c, "check_instance", return_value=True):
                v_c.check_instances()
            self.assertTrue(v_c.required_resources_dict["floating_ips"] == i)

    @patch("bibigrid.core.utility.image_selection.select_image")
    def test_check_instance_image_not_active(self, mock_select_image):
        mock_select_image.side_effect = ImageNotActiveException()
        v_c = validate_configuration.ValidateConfiguration(providers=None, configurations=None, log=Mock())
        provider = Mock()
        provider.get_active_images.return_value = []
        provider.get_flavor = MagicMock(return_value={"disk": None, "ram": None})
        provider.get_image_by_id_or_name = MagicMock(return_value={"min_disk": None, "min_ram": None})
        self.assertFalse(v_c.check_instance(None, {"count": 1, "image": 2, "type": 3}, provider))

    @patch("bibigrid.core.utility.image_selection.select_image")
    def test_check_instance_image_active_combination_call(self, mock_select_image):
        v_c = validate_configuration.ValidateConfiguration(providers=None, configurations=None, log=Mock())
        provider = Mock()
        provider.get_image_by_id_or_name = MagicMock(return_value={"status": "active"})
        with patch.object(v_c, "check_instance_type_image_combination") as mock:
            v_c.check_instance(42, {"count": 1, "image": 2, "type": 3}, provider)
            mock.assert_called_with(3, mock_select_image(2), provider)

    def test_check_instance_type_image_combination_has_enough_calls(self):
        log = Mock()
        v_c = validate_configuration.ValidateConfiguration(providers=None, configurations=None, log=log)
        provider = MagicMock()
        provider.get_flavor.return_value = {"disk": 42, "ram": 32, "vcpus": 10}
        provider.get_image_by_id_or_name.return_value = {"min_disk": 22, "min_ram": 12}
        with patch.object(validate_configuration, "has_enough") as mock:
            v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                      provider=provider)
            self.assertEqual(call(42, 22, "Type de.NBI tiny", "disk space", log), mock.call_args_list[0])
            self.assertEqual(call(32, 12, "Type de.NBI tiny", "ram", log), mock.call_args_list[1])

    def test_check_instance_type_image_combination_result(self):
        provider = MagicMock()
        provider.get_flavor.return_value = {"disk": 42, "ram": 32, "vcpus": 10}
        provider.get_image_by_id_or_name.return_value = {"min_disk": 22, "min_ram": 12}
        v_c = validate_configuration.ValidateConfiguration(providers=None, configurations=None, log=Mock())
        with patch.object(validate_configuration, "has_enough") as mock:
            mock.side_effect = [True, True, False, False, True, False, False, True]
            # True True
            self.assertTrue(v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                      provider=provider))
            # False False
            self.assertFalse(v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                       provider=provider))
            # True False
            self.assertFalse(v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                       provider=provider))
            # False True
            self.assertFalse(v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                       provider=provider))

    def test_check_instance_type_image_combination_count(self):
        for i in range(3):
            provider = MagicMock()
            provider.get_flavor.return_value = {"disk": 42, "ram": i * 32, "vcpus": i * 10}
            provider.get_image_by_id_or_name.return_value = {"min_disk": 22, "min_ram": 12}
            log = Mock()
            v_c = validate_configuration.ValidateConfiguration(providers=None, configurations=None, log=log)
            with patch.object(validate_configuration, "has_enough") as mock:
                v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                          provider=provider)
                self.assertEqual(32 * i, v_c.required_resources_dict["total_ram"])
                self.assertEqual(10 * i, v_c.required_resources_dict["total_cores"])
                mock.assert_called_with(32 * i, 12, 'Type de.NBI tiny', 'ram', log)

    def test_check_volumes_none(self):
        v_c = validate_configuration.ValidateConfiguration(providers=[42], configurations=[{}], log=Mock())
        self.assertTrue(v_c.check_volumes())

    def test_check_volumes_mismatch(self):
        provider = Mock()
        provider.get_volume_by_id_or_name = MagicMock(return_value=None)
        provider.get_volume_snapshot_by_id_or_name = MagicMock(return_value=None)
        v_c = validate_configuration.ValidateConfiguration(providers=[provider],
                                                           configurations=[{"masterMounts": ["Test"]}], log=Mock())
        self.assertFalse(v_c.check_volumes())

    def test_check_volumes_match_snapshot(self):
        provider = Mock()
        provider.get_volume_by_id_or_name = MagicMock(return_value=None)
        provider.get_volume_snapshot_by_id_or_name = MagicMock(return_value={"size": 1})
        v_c = validate_configuration.ValidateConfiguration(providers=[provider],
                                                           configurations=[{"masterMounts": ["Test"]}], log=Mock())
        self.assertTrue(v_c.check_volumes())

    def test_check_volumes_match_snapshot_count(self):
        for i in range(3):
            provider = Mock()
            provider.get_volume_by_id_or_name = MagicMock(return_value=None)
            provider.get_volume_snapshot_by_id_or_name = MagicMock(return_value={"size": i})
            v_c = validate_configuration.ValidateConfiguration(providers=[provider] * i,
                                                               configurations=[{"masterMounts": ["Test"] * i}],
                                                               log=Mock())
            self.assertTrue(v_c.check_volumes())
            self.assertTrue(v_c.required_resources_dict["Volumes"] == i)
            self.assertTrue(v_c.required_resources_dict["VolumeGigabytes"] == i ** 2)

    def test_check_volumes_match_volume(self):
        provider = Mock()
        provider.get_volume_by_id_or_name = MagicMock(return_value={"size": 1})
        provider.get_volume_snapshot_by_id_or_name = MagicMock(return_value=None)
        v_c = validate_configuration.ValidateConfiguration(providers=[provider],
                                                           configurations=[{"masterMounts": ["Test"]}], log=Mock())
        self.assertTrue(v_c.check_volumes())
        self.assertTrue(v_c.required_resources_dict["Volumes"] == 0)
        self.assertTrue(v_c.required_resources_dict["VolumeGigabytes"] == 0)

    def test_check_network_none(self):
        provider = Mock()
        provider.get_network_by_id_or_name = MagicMock(return_value=None)
        v_c = validate_configuration.ValidateConfiguration(providers=[provider], configurations=[{}], log=Mock())
        self.assertFalse(v_c.check_network())

    def test_check_network_no_network(self):
        provider = Mock()
        provider.get_subnet_by_id_or_name = MagicMock(return_value="network")
        v_c = validate_configuration.ValidateConfiguration(providers=[provider],
                                                           configurations=[{"subnet": "subnet_name"}], log=Mock())
        self.assertTrue(v_c.check_network())
        provider.get_subnet_by_id_or_name.assert_called_with("subnet_name")

    def test_check_network_no_network_mismatch_subnet(self):
        provider = Mock()
        provider.get_subnet_by_id_or_name = MagicMock(return_value=None)
        v_c = validate_configuration.ValidateConfiguration(providers=[provider],
                                                           configurations=[{"subnet": "subnet_name"}], log=Mock())
        self.assertFalse(v_c.check_network())
        provider.get_subnet_by_id_or_name.assert_called_with("subnet_name")

    def test_check_network_no_subnet_mismatch_network(self):
        provider = Mock()
        provider.get_network_by_id_or_name = MagicMock(return_value=None)
        v_c = validate_configuration.ValidateConfiguration(providers=[provider],
                                                           configurations=[{"network": "network_name"}], log=Mock())
        self.assertFalse(v_c.check_network())
        provider.get_network_by_id_or_name.assert_called_with("network_name")

    def test_check_network_no_subnet(self):
        provider = Mock()
        provider.get_network_by_id_or_name = MagicMock(return_value="network")
        v_c = validate_configuration.ValidateConfiguration(providers=[provider],
                                                           configurations=[{"network": "network_name"}], log=Mock())
        self.assertTrue(v_c.check_network())
        provider.get_network_by_id_or_name.assert_called_with("network_name")

    def test_check_network_subnet_network(self):
        provider = Mock()
        provider.get_network_by_id_or_name = MagicMock(return_value="network")
        provider.get_subnet_by_id_or_name = MagicMock(return_value="network")
        v_c = validate_configuration.ValidateConfiguration(providers=[provider],
                                                           configurations=[{"network": "network_name"}], log=Mock())
        self.assertTrue(v_c.check_network())
        provider.get_network_by_id_or_name.assert_called_with("network_name")

    def test_check_server_group_none(self):
        provider = Mock()
        provider.get_network_by_id_or_name = MagicMock(return_value=None)
        v_c = validate_configuration.ValidateConfiguration(providers=[provider], configurations=[{}], log=Mock())
        self.assertTrue(v_c.check_server_group())

    def test_check_server_group_mismatch(self):
        provider = Mock()
        provider.get_server_group_by_id_or_name = MagicMock(return_value=None)
        v_c = validate_configuration.ValidateConfiguration(providers=[provider],
                                                           configurations=[{"serverGroup": "GroupName"}], log=Mock())
        self.assertFalse(v_c.check_server_group())
        provider.get_server_group_by_id_or_name.assert_called_with("GroupName")

    def test_check_server_group_match(self):
        provider = Mock()
        provider.get_server_group_by_id_or_name = MagicMock(return_value="Group")
        v_c = validate_configuration.ValidateConfiguration(providers=[provider],
                                                           configurations=[{"serverGroup": "GroupName"}], log=Mock())
        self.assertTrue(v_c.check_server_group())
        provider.get_server_group_by_id_or_name.assert_called_with("GroupName")

    def test_check_quotas_true(self):
        provider = MagicMock()
        provider.cloud_specification = {"auth": {"project_name": "name"}, "identifier": "identifier"}
        test_dict = {'total_cores': 42, 'floating_ips': 42, 'instances': 42, 'total_ram': 42, 'Volumes': 42,
                     'VolumeGigabytes': 42, 'Snapshots': 42, 'Backups': 42, 'BackupGigabytes': 42}
        provider.get_free_resources.return_value = test_dict
        v_c = validate_configuration.ValidateConfiguration(providers=[provider], configurations=None, log=Mock())
        with patch.object(validate_configuration, "has_enough") as mock:
            mock.side_effect = [True] * len(test_dict)
            self.assertTrue(v_c.check_quotas())
            provider.get_free_resources.assert_called()

    def test_check_quotas_false(self):
        provider = MagicMock()
        test_dict = {'total_cores': 42, 'floating_ips': 42, 'instances': 42, 'total_ram': 42, 'Volumes': 42,
                     'VolumeGigabytes': 42, 'Snapshots': 42, 'Backups': 42, 'BackupGigabytes': 42}
        provider.get_free_resources.return_value = test_dict
        os.environ['OS_PROJECT_NAME'] = "name"
        v_c = validate_configuration.ValidateConfiguration(providers=[provider], configurations=None, log=Mock())
        with patch.object(validate_configuration, "has_enough") as mock:
            mock.side_effect = [True] * (len(test_dict) - 1) + [False]
            self.assertFalse(v_c.check_quotas())
            provider.get_free_resources.assert_called()
            mock.assert_called()

    def test_has_enough_lower(self):
        self.assertTrue(validate_configuration.has_enough(2, 1, "", "", log=Mock()))

    def test_has_enough_equal(self):
        self.assertTrue(validate_configuration.has_enough(2, 2, "", "", log=Mock()))

    def test_has_enough_higher(self):
        self.assertFalse(validate_configuration.has_enough(1, 2, "", "", log=Mock()))

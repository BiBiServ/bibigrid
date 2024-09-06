"""
Tests for validate configuration
"""

import os
from unittest import TestCase
from unittest.mock import Mock, patch, MagicMock, call

from bibigrid.core import startup
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
        provider1 = Mock()
        provider1.cloud_specification = {"identifier": "1"}
        master = {"masterInstance": "Value"}
        vpn = {"vpnInstance": "Value"}
        vpn_master = {}
        vpn_master.update(master)
        vpn_master.update(vpn)
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=[master], log=Mock())
        self.assertTrue(v_c.check_master_vpn_worker())
        v_c.configurations = [master, vpn]
        self.assertTrue(v_c.check_master_vpn_worker())
        v_c.configurations = [vpn]
        self.assertFalse(v_c.check_master_vpn_worker())
        v_c.configurations = [master, master]
        self.assertFalse(v_c.check_master_vpn_worker())

    def test_check_master_vpn_worker_unique(self):
        provider1 = Mock()
        provider1.cloud_specification = {"identifier": "1"}
        master = {"masterInstance": "Value"}
        vpn = {"vpnInstance": "Value"}
        vpn_master = {}
        vpn_master.update(master)
        vpn_master.update(vpn)
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=[vpn_master],
                                                           log=Mock())
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
        provider1 = Mock()
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"masterInstance": "42"}], log=Mock())
        with patch.object(v_c, "check_instance") as mock:
            v_c.check_instances()
            mock.assert_called_with("masterInstance", "42", provider1)

    def test_check_instances_vpn(self):
        provider1 = Mock()
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"vpnInstance": "42"}], log=Mock())
        with patch.object(v_c, "check_instance") as mock:
            v_c.check_instances()
            mock.assert_called_with("vpnInstance", "42", provider1)

    def test_check_instances_vpn_worker(self):
        provider1 = Mock()
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=[
            {"masterInstance": "42", "workerInstances": ["42"]}], log=Mock())
        with patch.object(v_c, "check_instance") as mock:
            v_c.check_instances()
            mock.assert_called_with("workerInstance", "42", provider1)

    def test_check_instances_vpn_master_missing(self):
        provider1 = Mock()
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=[{}], log=Mock())
        self.assertFalse(v_c.check_instances())
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"workerInstances": ["42"]}], log=Mock())
        self.assertFalse(v_c.check_instances())

    def test_check_instances_vpn_master_count(self):
        for i in range(1, 4):
            provider1 = Mock()
            provider1.cloud_specification = {"identifier": i}
            v_c = validate_configuration.ValidateConfiguration(providers=[provider1] * i,
                                                               configurations=[{"masterInstance": {"count": 1}}] + [
                                                                   {"vpnInstance": {"count": 1}}] * (i - 1), log=Mock())
            # with patch.object(v_c, "check_instance") as mock:
            with patch.object(v_c, "check_instance", return_value=True):
                v_c.check_instances()
            self.assertTrue(v_c.required_resources_dict[i]["floating_ips"] == i)

    @patch("bibigrid.core.utility.image_selection.select_image")
    def test_check_instance_image_not_active(self, mock_select_image):
        mock_select_image.side_effect = ImageNotActiveException()

        provider1 = Mock()
        provider1.cloud_specification = {"identifier": "1"}
        provider1.get_active_images.return_value = []
        provider1.get_flavor = MagicMock(return_value={"disk": None, "ram": None})
        provider1.get_image_by_id_or_name = MagicMock(return_value={"min_disk": None, "min_ram": None})
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=None, log=Mock())

        self.assertFalse(v_c.check_instance(None, {"count": 1, "image": 2, "type": 3}, provider1))

    @patch("bibigrid.core.utility.image_selection.select_image")
    def test_check_instance_image_active_combination_call(self, mock_select_image):
        provider1 = MagicMock()
        provider1.get_image_by_id_or_name = MagicMock(return_value={"status": "active"})
        provider1.cloud_specification = {"identifier": "1"}
        image = 2
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=None, log=startup.LOG)
        type3 = 3

        with patch.object(v_c, "check_instance_type_image_combination") as mock:
            v_c.check_instance(42, {"count": 1, "image": image, "type": type3}, provider1)
            mock.assert_called_with(type3, mock_select_image(), provider1)
        mock_select_image.assert_called()

    def test_check_instance_type_image_combination_has_enough_calls(self):
        log = Mock()
        provider1 = MagicMock()
        provider1.get_flavor.return_value = {"disk": 42, "ram": 32, "vcpus": 10}
        provider1.get_image_by_id_or_name.return_value = {"min_disk": 22, "min_ram": 12}
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=None, log=log)
        with patch.object(validate_configuration, "has_enough") as mock:
            v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                      provider=provider1)
            self.assertEqual(call(42, 22, "Type de.NBI tiny", "disk space", log), mock.call_args_list[0])
            self.assertEqual(call(32, 12, "Type de.NBI tiny", "ram", log), mock.call_args_list[1])

    def test_check_instance_type_image_combination_result(self):
        provider1 = MagicMock()
        provider1.get_flavor.return_value = {"disk": 42, "ram": 32, "vcpus": 10}
        provider1.get_image_by_id_or_name.return_value = {"min_disk": 22, "min_ram": 12}
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=None, log=Mock())
        with patch.object(validate_configuration, "has_enough") as mock:
            mock.side_effect = [True, True, False, False, True, False, False, True]
            # True True
            self.assertTrue(v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                      provider=provider1))
            # False False
            self.assertFalse(v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                       provider=provider1))
            # True False
            self.assertFalse(v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                       provider=provider1))
            # False True
            self.assertFalse(v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                       provider=provider1))

    def test_check_instance_type_image_combination_count(self):
        for i in range(3):
            provider1 = MagicMock()
            provider1.get_flavor.return_value = {"disk": 42, "ram": i * 32, "vcpus": i * 10}
            provider1.get_image_by_id_or_name.return_value = {"min_disk": 22, "min_ram": 12}
            provider1.cloud_specification = {"identifier": "1"}
            log = Mock()
            v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=None, log=log)
            with patch.object(validate_configuration, "has_enough") as mock:
                v_c.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                          provider=provider1)
                self.assertEqual(32 * i, v_c.required_resources_dict["1"]["total_ram"])
                self.assertEqual(10 * i, v_c.required_resources_dict["1"]["total_cores"])
                mock.assert_called_with(32 * i, 12, 'Type de.NBI tiny', 'ram', log)

    def test_check_volumes_none(self):
        provider1 = MagicMock()
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=[{}], log=Mock())
        self.assertTrue(v_c.check_volumes())

    def test_check_volumes_mismatch(self):
        provider1 = Mock()
        provider1.get_volume_by_id_or_name = MagicMock(return_value=None)
        provider1.get_volume_snapshot_by_id_or_name = MagicMock(return_value=None)
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"masterMounts": [{"name": "Test"}]}],
                                                           log=Mock())
        self.assertFalse(v_c.check_volumes())

    def test_check_volumes_match_snapshot(self):
        provider1 = Mock()
        provider1.get_volume_by_id_or_name = MagicMock(return_value=None)
        provider1.get_volume_snapshot_by_id_or_name = MagicMock(return_value={"size": 1})
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"masterMounts": [{"name": "Test"}]}],
                                                           log=Mock())
        self.assertTrue(v_c.check_volumes())

    def test_check_volumes_match_snapshot_count(self):
        for i in range(1, 3):
            provider1 = Mock()
            provider1.get_volume_by_id_or_name = MagicMock(return_value=None)
            provider1.get_volume_snapshot_by_id_or_name = MagicMock(return_value={"size": i})
            provider1.cloud_specification = {"identifier": i}
            v_c = validate_configuration.ValidateConfiguration(providers=[provider1] * i, configurations=[
                {"masterMounts": [{"name": "Test"}] * i}], log=Mock())
            self.assertTrue(v_c.check_volumes())
            self.assertTrue(v_c.required_resources_dict[i]["volumes"] == i)
            self.assertTrue(v_c.required_resources_dict[i]["volume_gigabytes"] == i ** 2)

    def test_check_volumes_match_volume(self):
        provider1 = Mock()
        provider1.get_volume_by_id_or_name = MagicMock(return_value={"size": 1})
        provider1.get_volume_snapshot_by_id_or_name = MagicMock(return_value=None)
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"masterMounts": [{"name": "Test"}]}],
                                                           log=Mock())
        self.assertTrue(v_c.check_volumes())
        self.assertTrue(v_c.required_resources_dict["1"]["volumes"] == 0)
        self.assertTrue(v_c.required_resources_dict["1"]["volume_gigabytes"] == 0)

    def test_check_network_none(self):
        provider1 = Mock()
        provider1.get_network_by_id_or_name = MagicMock(return_value=None)
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=[{}], log=Mock())
        self.assertFalse(v_c.check_network())

    def test_check_network_no_network(self):
        provider1 = Mock()
        provider1.get_subnet_by_id_or_name = MagicMock(return_value="network")
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"subnet": "subnet_name"}], log=Mock())
        self.assertTrue(v_c.check_network())
        provider1.get_subnet_by_id_or_name.assert_called_with("subnet_name")

    def test_check_network_no_network_mismatch_subnet(self):
        provider1 = Mock()
        provider1.get_subnet_by_id_or_name = MagicMock(return_value=None)
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"subnet": "subnet_name"}], log=Mock())
        self.assertFalse(v_c.check_network())
        provider1.get_subnet_by_id_or_name.assert_called_with("subnet_name")

    def test_check_network_no_subnet_mismatch_network(self):
        provider1 = Mock()
        provider1.get_network_by_id_or_name = MagicMock(return_value=None)
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"network": "network_name"}], log=Mock())
        self.assertFalse(v_c.check_network())
        provider1.get_network_by_id_or_name.assert_called_with("network_name")

    def test_check_network_no_subnet(self):
        provider1 = Mock()
        provider1.get_network_by_id_or_name = MagicMock(return_value="network")
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"network": "network_name"}], log=Mock())
        self.assertTrue(v_c.check_network())
        provider1.get_network_by_id_or_name.assert_called_with("network_name")

    def test_check_network_subnet_network(self):
        provider1 = Mock()
        provider1.get_network_by_id_or_name = MagicMock(return_value="network")
        provider1.get_subnet_by_id_or_name = MagicMock(return_value="network")
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"network": "network_name"}], log=Mock())
        self.assertTrue(v_c.check_network())
        provider1.get_network_by_id_or_name.assert_called_with("network_name")

    def test_check_server_group_none(self):
        provider1 = Mock()
        provider1.get_network_by_id_or_name = MagicMock(return_value=None)
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=[{}], log=Mock())
        self.assertTrue(v_c.check_server_group())

    def test_check_server_group_mismatch(self):
        provider1 = Mock()
        provider1.get_server_group_by_id_or_name = MagicMock(return_value=None)
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"serverGroup": "GroupName"}], log=Mock())
        self.assertFalse(v_c.check_server_group())
        provider1.get_server_group_by_id_or_name.assert_called_with("GroupName")

    def test_check_server_group_match(self):
        provider1 = Mock()
        provider1.get_server_group_by_id_or_name = MagicMock(return_value="Group")
        provider1.cloud_specification = {"identifier": "1"}
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1],
                                                           configurations=[{"serverGroup": "GroupName"}], log=Mock())
        self.assertTrue(v_c.check_server_group())
        provider1.get_server_group_by_id_or_name.assert_called_with("GroupName")

    def test_check_quotas_true(self):
        provider1 = MagicMock()
        provider1.cloud_specification = {"auth": {"project_name": "name"}, "identifier": "1"}
        test_dict = {'total_cores': 42, 'floating_ips': 42, 'instances': 42, 'total_ram': 42, 'volumes': 42,
                     'volume_gigabytes': 42, 'snapshots': 42, 'backups': 42, 'backup_gigabytes': 42}
        provider1.get_free_resources.return_value = test_dict
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=None, log=Mock())
        with patch.object(validate_configuration, "has_enough") as mock:
            mock.side_effect = [True] * len(test_dict)
            self.assertTrue(v_c.check_quotas())
            provider1.get_free_resources.assert_called()

    def test_check_quotas_false(self):
        provider1 = MagicMock()
        provider1.cloud_specification = {"identifier": "1"}
        test_dict = {'total_cores': 42, 'floating_ips': 42, 'instances': 42, 'total_ram': 42, 'volumes': 42,
                     'volume_gigabytes': 42, 'snapshots': 42, 'backups': 42, 'backup_gigabytes': 42}
        provider1.get_free_resources.return_value = test_dict
        os.environ['OS_PROJECT_NAME'] = "name"
        v_c = validate_configuration.ValidateConfiguration(providers=[provider1], configurations=None, log=Mock())
        with patch.object(validate_configuration, "has_enough") as mock:
            mock.side_effect = [True] * (len(test_dict) - 1) + [False]
            self.assertFalse(v_c.check_quotas())
            provider1.get_free_resources.assert_called()
            mock.assert_called()

    def test_has_enough_lower(self):
        self.assertTrue(validate_configuration.has_enough(2, 1, "", "", log=Mock()))

    def test_has_enough_equal(self):
        self.assertTrue(validate_configuration.has_enough(2, 2, "", "", log=Mock()))

    def test_has_enough_higher(self):
        self.assertFalse(validate_configuration.has_enough(1, 2, "", "", log=Mock()))

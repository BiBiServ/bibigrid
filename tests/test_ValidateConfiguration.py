import os
from unittest import TestCase
from unittest.mock import Mock, patch, MagicMock, call

from bibigrid.core.utility import validate_configuration


class TestValidateConfiguration(TestCase):
    def test_check_provider_data_count(self):
        provider_data_1 = {"PROJECT_ID": "abcd", "PROJECT_NAME": "1234"}
        provider_data_2 = {"PROJECT_ID": "9999", "PROJECT_NAME": "9999"}
        vc = validate_configuration
        self.assertTrue(vc.check_provider_data([provider_data_1, provider_data_2], 2))
        self.assertFalse(vc.check_provider_data([provider_data_1, provider_data_2], 3))
        self.assertTrue(vc.check_provider_data([], 0))

    def test_check_provider_data_unique(self):
        provider_data_1 = {"PROJECT_ID": "abcd", "PROJECT_NAME": "1234"}
        provider_data_2 = {"PROJECT_ID": "9999", "PROJECT_NAME": "9999"}
        vc = validate_configuration
        self.assertTrue(vc.check_provider_data([provider_data_1, provider_data_2], 2))
        self.assertFalse(vc.check_provider_data([provider_data_1, provider_data_1], 2))
        self.assertTrue(vc.check_provider_data([], 0))

    def test_check_master_vpn_worker_ordered(self):
        master = {"masterInstance": "Value"}
        vpn = {"vpnInstance": "Value"}
        vpn_master = {}
        vpn_master.update(master)
        vpn_master.update(vpn)
        vc = validate_configuration.ValidateConfiguration(providers=None, configurations=[master])
        self.assertTrue(vc.check_master_vpn_worker())
        vc.configurations = [master, vpn]
        self.assertTrue(vc.check_master_vpn_worker())
        vc.configurations = [vpn]
        self.assertFalse(vc.check_master_vpn_worker())
        vc.configurations = [master, master]
        self.assertFalse(vc.check_master_vpn_worker())

    def test_check_master_vpn_worker_unique(self):
        master = {"masterInstance": "Value"}
        vpn = {"vpnInstance": "Value"}
        vpn_master = {}
        vpn_master.update(master)
        vpn_master.update(vpn)
        vc = validate_configuration.ValidateConfiguration(providers=None, configurations=[vpn_master])
        self.assertFalse(vc.check_master_vpn_worker())
        vc.configurations = [master, vpn_master]
        self.assertFalse(vc.check_master_vpn_worker())

    def test_evaluate(self):
        vc = validate_configuration
        self.assertTrue(vc.evaluate("some", True))
        self.assertFalse(vc.evaluate("some", False))

    def test_check_provider_connection(self):
        mock = Mock()
        mock.conn = False
        vc = validate_configuration.ValidateConfiguration(providers=[mock], configurations=None)
        self.assertFalse(vc.check_provider_connections())
        mock.conn = True
        self.assertTrue(vc.check_provider_connections())

    def test_check_instances_master(self):
        vc = validate_configuration.ValidateConfiguration(providers=["31"], configurations=[{"masterInstance": "42"}])
        with patch.object(vc, "check_instance") as mock:
            vc.check_instances()
            mock.assert_called_with("masterInstance", "42", "31")

    def test_check_instances_vpn(self):
        vc = validate_configuration.ValidateConfiguration(providers=["31"], configurations=[{"vpnInstance": "42"}])
        with patch.object(vc, "check_instance") as mock:
            vc.check_instances()
            mock.assert_called_with("vpnInstance", "42", "31")

    def test_check_instances_vpn_worker(self):
        vc = validate_configuration.ValidateConfiguration(providers=["31"], configurations=[
            {"masterInstance": "42", "workerInstances": ["42"]}])
        with patch.object(vc, "check_instance") as mock:
            vc.check_instances()
            mock.assert_called_with("workerInstance", "42", "31")

    def test_check_instances_vpn_master_missing(self):
        vc = validate_configuration.ValidateConfiguration(providers=["31"], configurations=[{}])
        self.assertFalse(vc.check_instances())
        vc = validate_configuration.ValidateConfiguration(providers=["31"],
                                                          configurations=[{"workerInstances": ["42"]}])
        self.assertFalse(vc.check_instances())

    def test_check_instances_vpn_master_count(self):
        for i in range(3):
            vc = validate_configuration.ValidateConfiguration(providers=["31"] * i,
                                                              configurations=[{"masterInstance": "42"}] * i)
            # with patch.object(vc, "check_instance") as mock:
            vc.check_instances()
            self.assertTrue(vc.required_resources_dict["floating_ips"] == i)

    def test_check_instance_image_not_found(self):
        vc = validate_configuration.ValidateConfiguration(providers=None, configurations=None)
        provider = Mock()
        provider.get_image_by_id_or_name = MagicMock(return_value=None)
        self.assertFalse(vc.check_instance(None, {"count": 1, "image": 2}, provider))

    def test_check_instance_image_not_active(self):
        vc = validate_configuration.ValidateConfiguration(providers=None, configurations=None)
        provider = Mock()
        provider.get_image_by_id_or_name = MagicMock(return_value={"status": None})
        self.assertFalse(vc.check_instance(None, {"count": 1, "image": 2}, provider))

    def test_check_instance_image_active_combination_call(self):
        vc = validate_configuration.ValidateConfiguration(providers=None, configurations=None)
        provider = Mock()
        provider.get_image_by_id_or_name = MagicMock(return_value={"status": "active"})
        with patch.object(vc, "check_instance_type_image_combination") as mock:
            vc.check_instance(42, {"count": 1, "image": 2, "type": 3}, provider)
            mock.assert_called_with(3, {"status": "active"}, provider)

    def test_check_instance_image_not_found_count(self):
        provider = Mock()
        provider.get_image_by_id_or_name = MagicMock(return_value=None)
        for i in range(1, 3):
            vc = validate_configuration.ValidateConfiguration(providers=None, configurations=None)
            vc.check_instance(None, {"count": i, "image": 2}, provider)
            self.assertTrue(vc.required_resources_dict["instances"] == i)

    def test_check_instance_type_image_combination_has_enough_calls(self):
        vc = validate_configuration.ValidateConfiguration(providers=None, configurations=None)
        provider = MagicMock()
        provider.get_flavor.return_value = {"disk": 42, "ram": 32, "vcpus": 10}
        provider.get_image_by_id_or_name.return_value = {"minDisk": 22, "minRam": 12}
        with patch.object(vc, "has_enough") as mock:
            vc.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                     provider=provider)
            self.assertEqual(call(42, 22, "Type de.NBI tiny", "disk space"), mock.call_args_list[0])
            self.assertEqual(call(32, 12, "Type de.NBI tiny", "ram"), mock.call_args_list[1])

    def test_check_instance_type_image_combination_result(self):
        provider = MagicMock()
        provider.get_flavor.return_value = {"disk": 42, "ram": 32, "vcpus": 10}
        provider.get_image_by_id_or_name.return_value = {"minDisk": 22, "minRam": 12}
        vc = validate_configuration.ValidateConfiguration(providers=None, configurations=None)
        with patch.object(vc, "has_enough") as mock:
            mock.side_effect = [True, True, False, False, True, False, False, True]
            # True True
            self.assertTrue(vc.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                     provider=provider))
            # False False
            self.assertFalse(vc.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                      provider=provider))
            # True False
            self.assertFalse(vc.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                      provider=provider))
            # False True
            self.assertFalse(vc.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                                      provider=provider))

    def test_check_instance_type_image_combination_count(self):
        for i in range(3):
            provider = MagicMock()
            provider.get_flavor.return_value = {"disk": 42, "ram": i * 32, "vcpus": i * 10}
            provider.get_image_by_id_or_name.return_value = {"minDisk": 22, "minRam": 12}
            vc = validate_configuration.ValidateConfiguration(providers=None, configurations=None)
            with patch.object(vc, "has_enough") as mock:
                vc.check_instance_type_image_combination(instance_image=None, instance_type="de.NBI tiny",
                                                         provider=provider)
                self.assertEqual(32 * i, vc.required_resources_dict["total_ram"])
                self.assertEqual(10 * i, vc.required_resources_dict["total_cores"])
                mock.assert_called_with(32 * i, 12, 'Type de.NBI tiny', 'ram')

    def test_check_volumes_none(self):
        vc = validate_configuration.ValidateConfiguration(providers=[42], configurations=[{}])
        self.assertTrue(vc.check_volumes())

    def test_check_volumes_mismatch(self):
        provider = Mock()
        provider.get_volume_by_id_or_name = MagicMock(return_value=None)
        provider.get_volume_snapshot_by_id_or_name = MagicMock(return_value=None)
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{"masterMounts": ["Test"]}])
        self.assertFalse(vc.check_volumes())

    def test_check_volumes_match_snapshot(self):
        provider = Mock()
        provider.get_volume_by_id_or_name = MagicMock(return_value=None)
        provider.get_volume_snapshot_by_id_or_name = MagicMock(return_value={"size": 1})
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{"masterMounts": ["Test"]}])
        self.assertTrue(vc.check_volumes())

    def test_check_volumes_match_snapshot_count(self):
        for i in range(3):
            provider = Mock()
            provider.get_volume_by_id_or_name = MagicMock(return_value=None)
            provider.get_volume_snapshot_by_id_or_name = MagicMock(return_value={"size": i})
            vc = validate_configuration.ValidateConfiguration(providers=[provider] * i,
                                                              configurations=[{"masterMounts": ["Test"] * i}])
            self.assertTrue(vc.check_volumes())
            self.assertTrue(vc.required_resources_dict["Volumes"] == i)
            self.assertTrue(vc.required_resources_dict["VolumeGigabytes"] == i ** 2)

    def test_check_volumes_match_volume(self):
        provider = Mock()
        provider.get_volume_by_id_or_name = MagicMock(return_value={"size": 1})
        provider.get_volume_snapshot_by_id_or_name = MagicMock(return_value=None)
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{"masterMounts": ["Test"]}])
        self.assertTrue(vc.check_volumes())
        self.assertTrue(vc.required_resources_dict["Volumes"] == 0)
        self.assertTrue(vc.required_resources_dict["VolumeGigabytes"] == 0)

    def test_check_network_none(self):
        provider = Mock()
        provider.get_network_by_id_or_name = MagicMock(return_value=None)
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{}])
        self.assertFalse(vc.check_network())

    def test_check_network_no_network(self):
        provider = Mock()
        provider.get_subnet_by_id_or_name = MagicMock(return_value="network")
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{"subnet": "subnet_name"}])
        self.assertTrue(vc.check_network())
        provider.get_subnet_by_id_or_name.assert_called_with("subnet_name")

    def test_check_network_no_network_mismatch_subnet(self):
        provider = Mock()
        provider.get_subnet_by_id_or_name = MagicMock(return_value=None)
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{"subnet": "subnet_name"}])
        self.assertFalse(vc.check_network())
        provider.get_subnet_by_id_or_name.assert_called_with("subnet_name")

    def test_check_network_no_subnet_mismatch_network(self):
        provider = Mock()
        provider.get_network_by_id_or_name = MagicMock(return_value=None)
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{"network": "network_name"}])
        self.assertFalse(vc.check_network())
        provider.get_network_by_id_or_name.assert_called_with("network_name")

    def test_check_network_no_subnet(self):
        provider = Mock()
        provider.get_network_by_id_or_name = MagicMock(return_value="network")
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{"network": "network_name"}])
        self.assertTrue(vc.check_network())
        provider.get_network_by_id_or_name.assert_called_with("network_name")

    def test_check_network_subnet_network(self):
        provider = Mock()
        provider.get_network_by_id_or_name = MagicMock(return_value="network")
        provider.get_subnet_by_id_or_name = MagicMock(return_value="network")
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{"network": "network_name"}])
        self.assertTrue(vc.check_network())
        provider.get_network_by_id_or_name.assert_called_with("network_name")

    def test_check_server_group_none(self):
        provider = Mock()
        provider.get_network_by_id_or_name = MagicMock(return_value=None)
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{}])
        self.assertTrue(vc.check_server_group())

    def test_check_server_group_mismatch(self):
        provider = Mock()
        provider.get_server_group_by_id_or_name = MagicMock(return_value=None)
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{"serverGroup": "GroupName"}])
        self.assertFalse(vc.check_server_group())
        provider.get_server_group_by_id_or_name.assert_called_with("GroupName")

    def test_check_server_group_match(self):
        provider = Mock()
        provider.get_server_group_by_id_or_name = MagicMock(return_value="Group")
        vc = validate_configuration.ValidateConfiguration(providers=[provider],
                                                          configurations=[{"serverGroup": "GroupName"}])
        self.assertTrue(vc.check_server_group())
        provider.get_server_group_by_id_or_name.assert_called_with("GroupName")

    def test_check_quotas_true(self):
        provider = MagicMock()
        provider.cloud_specification = {"auth": {"project_name": "name"}, "identifier": "identifier"}
        test_dict = {'total_cores': 42, 'floating_ips': 42, 'instances': 42, 'total_ram': 42,
                     'Volumes': 42, 'VolumeGigabytes': 42, 'Snapshots': 42, 'Backups': 42, 'BackupGigabytes': 42}
        provider.get_free_resources.return_value = test_dict
        vc = validate_configuration.ValidateConfiguration(providers=[provider], configurations=None)
        with patch.object(vc, "has_enough") as mock:
            mock.side_effect = [True] * len(test_dict)
            self.assertTrue(vc.check_quotas())
            provider.get_free_resources.assert_called()
            for key in vc.required_resources_dict.keys():
                self.assertTrue(call(test_dict[key], vc.required_resources_dict[key],
                                     "Project identifier", key) in mock.call_args_list)

    def test_check_quotas_false(self):
        provider = MagicMock()
        test_dict = {'total_cores': 42, 'floating_ips': 42, 'instances': 42, 'total_ram': 42,
                     'Volumes': 42, 'VolumeGigabytes': 42, 'Snapshots': 42, 'Backups': 42, 'BackupGigabytes': 42}
        provider.get_free_resources.return_value = test_dict
        os.environ['OS_PROJECT_NAME'] = "name"
        vc = validate_configuration.ValidateConfiguration(providers=[provider], configurations=None)
        with patch.object(vc, "has_enough") as mock:
            mock.side_effect = [True] * (len(test_dict) - 1) + [False]
            self.assertFalse(vc.check_quotas())
            provider.get_free_resources.assert_called()
            mock.assert_called()

    def test_has_enough_lower(self):
        vc = validate_configuration
        self.assertTrue(vc.has_enough(2, 1, "", ""))

    def test_has_enough_equal(self):
        vc = validate_configuration
        self.assertTrue(vc.has_enough(2, 2, "", ""))

    def test_has_enough_higher(self):
        vc = validate_configuration
        self.assertFalse(vc.has_enough(1, 2, "", ""))

from unittest import TestCase

import yaml

import bibigrid2.core.utility.handler.configuration_handler as configurationHandler
from unittest.mock import patch, mock_open, MagicMock
import os


class TestConfigurationHandler(TestCase):
    def test_get_list_by_name_none(self):
        configurations = [{}, {}]
        self.assertEqual([None, None], configurationHandler.get_list_by_key(configurations, "key1"))
        self.assertEqual([], configurationHandler.get_list_by_key(configurations, "key1", False))

    def test_get_list_by_name_empty(self):
        configurations = [{"key1": "value1", "key2": "value1"}, {"key1": "value2"}]
        self.assertEqual(["value1", "value2"], configurationHandler.get_list_by_key(configurations, "key1"))
        self.assertEqual(["value1", "value2"], configurationHandler.get_list_by_key(configurations, "key1", False))
        self.assertEqual(["value1", None], configurationHandler.get_list_by_key(configurations, "key2"))
        self.assertEqual(["value1"], configurationHandler.get_list_by_key(configurations, "key2", False))

    @patch("os.path.isfile")
    def test_read_configuration_no_file(self, mock_isfile):
        mock_isfile.return_value = False
        test = MagicMock()
        configuration = "Test: 42"
        expected_result = None
        with patch("builtins.open", mock_open(test, read_data=configuration)):
            result = configurationHandler.read_configuration("path")
        mock_isfile.assert_called_with("path")
        test.assert_not_called()
        self.assertEqual(expected_result, result)

    @patch("os.path.isfile")
    def test_read_configuration_file(self, mock_isfile):
        mock_isfile.return_value = True
        opener = MagicMock()
        configuration = "Test: 42"
        expected_result = {"Test": 42}
        with patch("builtins.open", mock_open(opener, read_data=configuration)):
            result = configurationHandler.read_configuration("path")
        mock_isfile.assert_called_with("path")
        opener.assert_called_with("path", "r")
        self.assertEqual(expected_result, result)

    @patch("os.path.isfile")
    def test_read_configuration_file_yaml_exception(self, mock_isfile):
        mock_isfile.return_value = True
        opener = MagicMock()
        configuration = "]unbalanced brackets["
        expected_result = None
        with patch("builtins.open", mock_open(opener, read_data=configuration)):
            result = configurationHandler.read_configuration("path")
        mock_isfile.assert_called_with("path")
        opener.assert_called_with("path", "r")
        self.assertEqual(expected_result, result)

    def test_find_file_in_folders_not_found_no_folder(self):
        expected_result = None
        result = configurationHandler.find_file_in_folders("true_file", [])
        self.assertEqual(expected_result, result)

    def test_find_file_in_folders_not_found_no_file(self):
        expected_result = None
        with patch("os.path.isfile") as mock_isfile:
            mock_isfile.return_value = False
            result = configurationHandler.find_file_in_folders("false_file", ["or_false_folder"])
        self.assertEqual(expected_result, result)
        mock_isfile.called_with(os.path.expanduser(os.path.join("or_false_folder", "false_file")))

    @patch("os.path.isfile")
    @patch("bibigrid2.core.utility.handler.configurationHandler.read_configuration")
    def test_find_file_in_folders(self, mock_read_configuration, mock_isfile):
        expected_result = 42
        mock_isfile.return_value(True)
        mock_read_configuration.return_value = 42
        result = configurationHandler.find_file_in_folders("true_file", ["true_folder"])
        self.assertEqual(expected_result, result)
        mock_read_configuration.assert_called_with(os.path.expanduser(os.path.join("true_folder", "true_file")))

    @patch("bibigrid2.core.utility.handler.configurationHandler.find_file_in_folders")
    def test_get_cloud_files_none(self, mock_ffif):
        mock_ffif.return_value = None
        expected_result = None, None
        result = configurationHandler.get_clouds_files()
        self.assertEqual(expected_result, result)

    @patch("bibigrid2.core.utility.handler.configurationHandler.find_file_in_folders")
    def test_get_cloud_files_no_clouds_yaml(self, mock_ffif):
        mock_ffif.side_effect = [None, {configurationHandler.CLOUD_PUBLIC_ROOT_KEY: 42}]
        expected_result = None, 42
        result = configurationHandler.get_clouds_files()
        self.assertEqual(expected_result, result)

    @patch("bibigrid2.core.utility.handler.configurationHandler.find_file_in_folders")
    def test_get_cloud_files_no_public_clouds_yaml(self, mock_ffif):
        mock_ffif.side_effect = [{configurationHandler.CLOUD_ROOT_KEY: 42}, None]
        expected_result = 42, None
        result = configurationHandler.get_clouds_files()
        self.assertEqual(expected_result, result)

    @patch("bibigrid2.core.utility.handler.configurationHandler.find_file_in_folders")
    def test_get_cloud_files_no_root_key_public(self, mock_ffif):
        mock_ffif.side_effect = [{configurationHandler.CLOUD_ROOT_KEY: 42}, {"name": 42}]
        expected_result = 42, None
        result = configurationHandler.get_clouds_files()
        self.assertEqual(expected_result, result)

    @patch("bibigrid2.core.utility.handler.configurationHandler.find_file_in_folders")
    def test_get_cloud_files_no_root_key_cloud(self, mock_ffif):
        mock_ffif.side_effect = [{"name": 42}, {configurationHandler.CLOUD_PUBLIC_ROOT_KEY: 42}]
        expected_result = None, 42
        result = configurationHandler.get_clouds_files()
        self.assertEqual(expected_result, result)

    @patch("bibigrid2.core.utility.handler.configurationHandler.find_file_in_folders")
    def test_get_cloud_files(self, mock_ffif):
        mock_ffif.side_effect = [{configurationHandler.CLOUD_ROOT_KEY: 22},
                                 {configurationHandler.CLOUD_PUBLIC_ROOT_KEY: 42}]
        expected_result = 22, 42
        result = configurationHandler.get_clouds_files()
        self.assertEqual(expected_result, result)
        mock_ffif.assert_called_with(configurationHandler.CLOUDS_PUBLIC_YAML, configurationHandler.CLOUDS_YAML_PATHS)

    @patch("bibigrid2.core.utility.handler.configurationHandler.get_cloud_specification")
    @patch("bibigrid2.core.utility.handler.configurationHandler.get_clouds_files")
    def test_get_cloud_specifications_none(self, mock_get_clouds_files, mock_get_clouds_specification):
        mock_get_clouds_files.return_value = None, None
        expected_result = []
        result = configurationHandler.get_cloud_specifications([{"cloud": 42}])
        self.assertEqual(expected_result, result)
        mock_get_clouds_specification.assert_not_called()
        mock_get_clouds_files.assert_called()

    @patch("bibigrid2.core.utility.handler.configurationHandler.get_cloud_specification")
    @patch("bibigrid2.core.utility.handler.configurationHandler.get_clouds_files")
    def test_get_cloud_specifications_no_cloud_configuration_key(self, mock_get_clouds_files,
                                                                 mock_get_clouds_specification):
        mock_get_clouds_files.return_value = {"Some"}, {"Some"}
        expected_result = []
        result = configurationHandler.get_cloud_specifications([{"no_cloud": 42}])
        self.assertEqual(expected_result, result)
        mock_get_clouds_specification.assert_not_called()
        mock_get_clouds_files.assert_called()

    @patch("bibigrid2.core.utility.handler.configurationHandler.get_cloud_specification")
    @patch("bibigrid2.core.utility.handler.configurationHandler.get_clouds_files")
    def test_get_cloud_specifications_cloud(self, mock_get_clouds_files, mock_get_clouds_specification):
        mock_get_clouds_files.return_value = {"1"}, {"2"}
        mock_get_clouds_specification.return_value = 21
        expected_result = [21]
        result = configurationHandler.get_cloud_specifications([{"cloud": 42}])
        self.assertEqual(expected_result, result)
        mock_get_clouds_specification.assert_called_with(42, {"1"}, {"2"})
        mock_get_clouds_files.assert_called()

    @patch("bibigrid2.core.utility.handler.configurationHandler.get_cloud_specification")
    @patch("bibigrid2.core.utility.handler.configurationHandler.get_clouds_files")
    def test_get_cloud_specifications_no_config(self, mock_get_clouds_files, mock_get_clouds_specification):
        mock_get_clouds_files.return_value = {"1"}, {"2"}
        mock_get_clouds_specification.return_value = 21
        expected_result = []
        result = configurationHandler.get_cloud_specifications([])
        self.assertEqual(expected_result, result)
        mock_get_clouds_specification.assert_not_called()
        mock_get_clouds_files.assert_called()

    def test_get_cloud_specification_no_matching_cloud(self):
        expected_result = {}
        result = configurationHandler.get_cloud_specification("some_name", {}, {"some_some": "public"})
        self.assertEqual(expected_result, result)

    def test_get_cloud_specification_cloud(self):
        expected_result = {42: 42}
        result = configurationHandler.get_cloud_specification("some_name", {"some_name": {42: 42}}, None)
        self.assertEqual(expected_result, result)

    def test_get_cloud_specification_no_public_cloud(self):
        expected_result = {42: 42, "profile": "name2"}
        result = configurationHandler.get_cloud_specification("some_name", {"some_name": expected_result},
                                                              {"not_name2": {21: 21}})
        self.assertEqual(expected_result, result)

    def test_get_cloud_specification(self):
        cloud_private_specification = {42: 42, "profile": "name2", "test": {"recursive": "foo"}}
        expected_result = {42: 42, "profile": "name2", "test": {"recursive": "foo"}, "additional": "value"}
        result = configurationHandler.get_cloud_specification("some_name", {"some_name": cloud_private_specification},
                                                              {"name2": {42: 21, "test": {"recursive": "oof"},
                                                                         "additional": "value"}})
        self.assertEqual(expected_result, result)

    def test_get_cloud_specification_type_exception(self):
        cloud_private_specification = {42: 42, "profile": "name2", "test": {"recursive": "foo"}}
        result = configurationHandler.get_cloud_specification("some_name", {"some_name": cloud_private_specification},
                                                              {"name2": {42: 21, "test": ["recursive", 22],
                                                                         "additional": "value"}})
        self.assertEqual({}, result)

"""
Module to test provider_handler
"""
from unittest import TestCase
from unittest.mock import MagicMock, patch

from bibigrid.core.utility.handler import provider_handler
from bibigrid.core import startup


class TestProviderHandler(TestCase):
    """
    Class to test provider_handler
    """

    @patch("bibigrid.core.utility.handler.configuration_handler.get_cloud_specifications")
    @patch("bibigrid.core.utility.handler.provider_handler.get_provider_list_by_name_list")
    def test_get_providers(self, mock_provider_list, mock_get_cloud_specifications):
        mock_get_cloud_specifications.return_value = True  # for if not false
        configurations = [{"infrastructure": "some"}]
        mock_provider_list.return_value = 42
        with patch("bibigrid.core.utility.handler.configuration_handler.get_list_by_key") as mock_by_name:
            self.assertEqual(42, provider_handler.get_providers(configurations, startup.LOG))
            mock_by_name.assert_called_with(configurations, "infrastructure")
        mock_get_cloud_specifications.assert_called_with(configurations, startup.LOG)

    def test_get_provider_list_by_name_list(self):
        keys = provider_handler.PROVIDER_NAME_DICT.keys()
        values = [42]
        with patch("bibigrid.core.utility.handler.provider_handler.get_provider_by_name") as mock_by_name:
            mock_by_name.return_value = MagicMock(return_value=42)
            self.assertEqual(provider_handler.get_provider_list_by_name_list(keys, "nonempty_specification"), values)
            mock_by_name.assert_called_with(list(keys)[0])

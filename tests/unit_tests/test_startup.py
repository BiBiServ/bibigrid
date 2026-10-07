"""
Modul to test startup
"""

from unittest import TestCase
from unittest.mock import Mock, patch, MagicMock

from bibigrid.core import startup


class TestStartup(TestCase):
    """
    Class to test startup
    """

    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    def test_provider_closing(self, mock_get_providers):
        provider = Mock
        provider.close = MagicMock()
        configurations = {}
        mock_get_providers.return_value = [provider]
        with patch("bibigrid.core.actions.list_clusters.log_list") as mock_lc:
            mock_lc.return_value = 42
            self.assertTrue(
                startup.run_action(action="list", configurations=configurations, config_input="", cluster_id=12,
                                   debug=True) == 42)
            mock_get_providers.assert_called_with(configurations, startup.LOG)
            provider.close.assert_called()

    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    def test_list_clusters(self, get_providers):
        provider_mock = Mock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        configurations = {}
        with patch("bibigrid.core.actions.list_clusters.log_list") as mock_lc:
            mock_lc.return_value = 42
            self.assertTrue(
                startup.run_action(action="list", configurations=configurations, config_input="", cluster_id=12,
                                   debug=True) == 42)
            mock_lc.assert_called_with(12, [provider_mock], startup.LOG)

    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    def test_check(self, get_providers):
        provider_mock = Mock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        configurations = {}
        with patch("bibigrid.core.actions.check.check") as mock_lc:
            mock_lc.return_value = 42
            self.assertTrue(
                startup.run_action(action="check", configurations=configurations, config_input="", cluster_id=21,
                                   debug=True) == 42)
            mock_lc.assert_called_with(configurations, [provider_mock], startup.LOG)

    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    @patch('bibigrid.core.actions.create.Create')
    def test_create(self, mock_create, get_providers):
        provider_mock = Mock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        configurations = {}
        creator = Mock()
        creator.create = MagicMock(return_value=42)
        mock_create.return_value = creator
        self.assertTrue(
            startup.run_action(action="create", configurations=configurations, config_input="", cluster_id=21,
                               debug=True) == 42)
        mock_create.assert_called_with(providers=[provider_mock], configurations=configurations, log=startup.LOG,
                                       debug=True, config_path="", cluster_id=21)
        creator.create.assert_called()

    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    def test_terminate(self, get_providers):
        provider_mock = Mock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        configurations = {}
        with patch("bibigrid.core.actions.terminate.terminate") as mock_tc:
            mock_tc.return_value = 42
            self.assertTrue(
                startup.run_action(action="terminate", configurations=configurations, config_input="", cluster_id=21,
                                   debug=True) == 42)
            mock_tc.assert_called_with(cluster_id=21, providers=[provider_mock], log=startup.LOG, debug=True)

    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    @patch("bibigrid.core.actions.ide.ide")
    def test_ide(self, mock_ide, get_providers):
        provider_mock = MagicMock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        configurations = [{"test_key": "test_value"}]
        mock_ide.return_value = 42
        self.assertTrue(startup.run_action(action="ide", configurations=configurations, config_input="", cluster_id=21,
                                           debug=True) == 42)
        mock_ide.assert_called_with(21, provider_mock, {"test_key": "test_value"}, startup.LOG)

    @patch('bibigrid.core.utility.id_generation.is_unique_cluster_id')
    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    def test_check_cid_create_unique_passes_through(self, get_providers, mock_is_unique):
        provider_mock = Mock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        mock_is_unique.return_value = True
        self.assertEqual("abc123", startup.check_cid("abc123", configurations={}, action="create"))
        provider_mock.close.assert_called_once()

    @patch('bibigrid.core.utility.id_generation.is_unique_cluster_id')
    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    def test_check_cid_create_duplicate_raises_and_still_closes_providers(self, get_providers, mock_is_unique):
        provider_mock = Mock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        mock_is_unique.return_value = False
        with self.assertRaises(RuntimeError):
            startup.check_cid("abc123", configurations={}, action="create")
        provider_mock.close.assert_called_once()

    @patch('bibigrid.core.utility.id_generation.is_unique_cluster_id')
    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    def test_check_cid_update_skips_uniqueness_check(self, get_providers, mock_is_unique):
        self.assertEqual("abc123", startup.check_cid("abc123", configurations={}, action="update"))
        get_providers.assert_not_called()
        mock_is_unique.assert_not_called()

    @patch('bibigrid.core.utility.id_generation.is_unique_cluster_id')
    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    def test_check_cid_create_checks_uniqueness_of_normalized_cid(self, get_providers, mock_is_unique):
        # regression: the master-name -> cid normalization must happen before the uniqueness check runs,
        # otherwise "create -cid bibigrid-master-abc123" would check the wrong (unnormalized) value
        provider_mock = Mock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        mock_is_unique.return_value = True
        result = startup.check_cid("bibigrid-master-abc123", configurations={}, action="create")
        self.assertEqual("abc123", result)
        mock_is_unique.assert_called_once_with("abc123", [provider_mock])

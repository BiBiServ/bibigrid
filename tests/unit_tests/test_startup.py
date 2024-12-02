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
        args = Mock()
        args.list = True
        args.version = False
        args.cluster_id = 12
        provider = Mock
        provider.close = MagicMock()
        configurations = {}
        mock_get_providers.return_value = [provider]
        with patch("bibigrid.core.actions.list_clusters.log_list") as mock_lc:
            mock_lc.return_value = 42
            self.assertTrue(startup.run_action(args, configurations, "") == 42)
            mock_get_providers.assert_called_with(configurations, startup.LOG)
            provider.close.assert_called()

    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    def test_list_clusters(self, get_providers):
        provider_mock = Mock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        args = Mock()
        args.list = True
        args.version = False
        args.cluster_id = 12
        configurations = {}
        with patch("bibigrid.core.actions.list_clusters.log_list") as mock_lc:
            mock_lc.return_value = 42
            self.assertTrue(startup.run_action(args, configurations, "") == 42)
            mock_lc.assert_called_with(12, [provider_mock], startup.LOG)

    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    def test_check(self, get_providers):
        provider_mock = Mock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        args = Mock()
        args.list = False
        args.version = False
        args.check = True
        args.cluster_id = 12
        configurations = {}
        with patch("bibigrid.core.actions.check.check") as mock_lc:
            mock_lc.return_value = 42
            self.assertTrue(startup.run_action(args, configurations, "") == 42)
            mock_lc.assert_called_with(configurations, [provider_mock], startup.LOG)

    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    @patch('bibigrid.core.actions.create.Create')
    def test_create(self, mock_create, get_providers):
        provider_mock = Mock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        args = Mock()
        args.list = False
        args.version = False
        args.check = False
        args.create = True
        args.cluster_id = 12
        args.debug = True
        configurations = {}
        creator = Mock()
        creator.create = MagicMock(return_value=42)
        mock_create.return_value = creator
        self.assertTrue(startup.run_action(args, configurations, "") == 42)
        mock_create.assert_called_with(providers=[provider_mock], configurations=configurations, log=startup.LOG,
                                       debug=True, config_path="")
        creator.create.assert_called()

    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    def test_terminate(self, get_providers):
        provider_mock = Mock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        args = Mock()
        args.list = False
        args.version = False
        args.create = False
        args.check = False
        args.terminate_cluster = True
        args.cluster_id = 12
        args.debug = True
        configurations = {}
        with patch("bibigrid.core.actions.terminate.terminate") as mock_tc:
            mock_tc.return_value = 42
            self.assertTrue(startup.run_action(args, configurations, "") == 42)
            mock_tc.assert_called_with(cluster_id=12, providers=[provider_mock], log=startup.LOG, debug=True)

    @patch('bibigrid.core.utility.handler.provider_handler.get_providers')
    @patch("bibigrid.core.actions.ide.ide")
    def test_ide(self, mock_ide, get_providers):
        provider_mock = MagicMock()
        provider_mock.close = Mock()
        get_providers.return_value = [provider_mock]
        args = Mock()
        args.list = False
        args.version = False
        args.create = False
        args.check = False
        args.terminate = False
        args.ide = True
        args.cluster_id = 12
        args.debug = True
        configurations = [{"test_key": "test_value"}]
        mock_ide.return_value = 42
        self.assertTrue(startup.run_action(args, configurations, "") == 42)
        mock_ide.assert_called_with(12, provider_mock, {"test_key": "test_value"}, startup.LOG)

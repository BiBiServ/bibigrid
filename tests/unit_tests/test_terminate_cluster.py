"""
Module to test terminate
"""
from unittest import TestCase
from unittest.mock import MagicMock, patch

from bibigrid.core import startup
from bibigrid.core.actions import create
from bibigrid.core.actions import terminate


class TestTerminate(TestCase):
    """
    Class to test terminate.
    """

    @patch("bibigrid.core.actions.terminate.delete_local_keypairs")
    @patch("bibigrid.core.actions.terminate.terminate_output")
    def test_terminate(self, mock_output, mock_local):
        mock_local.return_value = True
        provider = MagicMock()
        provider.cloud_specification["auth"]["project_name"] = 32
        cluster_id = 42
        provider.list_servers.return_value = [{"name": create.MASTER_IDENTIFIER(cluster_id=str(cluster_id)), "id": 21}]
        provider.delete_server.return_value = True
        provider.delete_keypair.return_value = True
        provider.delete_volume.return_value = True
        provider.list_volumes.return_value = [
            {"name": f"{create.MASTER_IDENTIFIER(cluster_id=str(cluster_id))}-tmp-0", "id": 42}]
        provider.list_volumes([{"name": "bibigrid-master-i950vaoqzfbwpnq-tmp-0"}])
        provider.delete_security_group.return_value = True
        provider.delete_application_credentials.return_value = True
        terminate.terminate(str(cluster_id), [provider], startup.LOG, False, True)
        provider.delete_server.assert_called_with(21)
        provider.delete_keypair.assert_called_with(create.KEY_NAME.format(cluster_id=cluster_id))
        mock_output.assert_called_with([provider.delete_server.return_value], [provider.delete_keypair.return_value],
                                       [provider.delete_security_group.return_value], [[True]],
                                       provider.delete_application_credentials.return_value, str(cluster_id),
                                       startup.LOG)

    @patch("bibigrid.core.actions.terminate.delete_local_keypairs")
    @patch("logging.info")
    def test_terminate_none(self, _, mock_local):
        mock_local.return_value = True
        provider = MagicMock()
        provider[0].specification["auth"]["project_name"] = "test_project_name"
        cluster_id = 42
        provider.list_servers.return_value = [
            {"name": create.MASTER_IDENTIFIER(cluster_id=str(cluster_id + 1)), "id": 21}]
        provider.delete_keypair.return_value = False
        terminate.terminate(str(cluster_id), [provider], startup.LOG, False, True)
        provider.delete_server.assert_not_called()
        provider.delete_keypair.assert_called_with(
            create.KEY_NAME.format(cluster_id=str(cluster_id)))  # since keypair is not called

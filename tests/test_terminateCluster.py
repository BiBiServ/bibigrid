from unittest import TestCase
from unittest.mock import MagicMock, patch

from bibigrid.core.actions import create
from bibigrid.core.actions import terminate_cluster


class TestTerminate(TestCase):

    @patch("bibigrid.core.actions.terminate_cluster.terminate_output")
    def test_terminate_cluster(self, _, mock_output):
        provider = MagicMock()
        provider.cloud_specification["auth"]["project_name"] = 32
        cluster_id = 42
        provider.list_servers.return_value = [
            {"name": create.MASTER_IDENTIFIER + create.SEPARATOR + str(cluster_id), "id": 21}]
        provider.delete_server.return_value = True
        provider.delete_keypair.return_value = True
        terminate_cluster.terminate_cluster(str(cluster_id), [provider], False)
        provider.delete_server.assert_called_with(21)
        provider.delete_keypair.assert_called_with(
            create.KEY_PREFIX + provider.cloud_specification["auth"]["project_name"] +
            create.SEPARATOR + str(cluster_id))
        mock_output.assert_called_with([provider.delete_server.return_value],
                                       [provider.delete_keypair.return_value], str(cluster_id))

    @patch("logging.info")
    def test_terminate_cluster_none(self, _):
        provider = MagicMock()
        provider[0].specification["auth"]["project_name"] = "test_project_name"
        cluster_id = 42
        provider.list_servers.return_value = [
            {"name": create.MASTER_IDENTIFIER + create.SEPARATOR + str(cluster_id + 1), "id": 21}]
        provider.delete_keypair.return_value = False
        terminate_cluster.terminate_cluster(str(cluster_id), [provider], False)
        provider.delete_server.assert_not_called()
        provider.delete_keypair.assert_called_with('bibigrid42')  # since keypair is not called

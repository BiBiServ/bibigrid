"""
Module to test terminate
"""
from unittest import TestCase
from unittest.mock import MagicMock, patch, call

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
        mock_output.assert_called_with(cluster_server_state=[provider.delete_server.return_value],
                                       cluster_keypair_state=[provider.delete_keypair.return_value],
                                       cluster_security_group_state=[provider.delete_security_group.return_value],
                                       cluster_volume_state=[[True]],
                                       ac_state=provider.delete_application_credentials.return_value,
                                       cluster_id=str(cluster_id),
                                       log=startup.LOG)

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

    def test_delete_non_pemanent_volumes(self):
        cluster_id = "1234"
        provider = MagicMock()
        log = MagicMock()
        cluster_id=21

        # List of test volumes
        volumes = [
            # Should be captured by the regex
            {"name": f"bibigrid-master-{cluster_id}-tmp-0"},
            {"name": f"bibigrid-master-{cluster_id}-semiperm-0"},
            {"name": f"bibigrid-master-{cluster_id}-tmp-0-na<-0med"},
            {"name": f"bibigrid-master-{cluster_id}-semiperm-0-na<-0med"},
            {"name": f"bibigrid-worker-{cluster_id}-0-tmp-0"},
            {"name": f"bibigrid-worker-{cluster_id}-11-semiperm-0"},
            {"name": f"bibigrid-worker-{cluster_id}-0-tmp-0-na<-0med"},
            {"name": f"bibigrid-worker-{cluster_id}-11-semiperm-0-na<-0med"},

            # Should NOT be captured by the regex
            {"name": f"bibigrid-master-{cluster_id}-perm-0"},
            {"name": f"bibigrid-master-{cluster_id}-perm-11-na<-0med"},
            {"name": f"bibigrid-worker-{cluster_id}-112-perm-0"},
            {"name": f"bibigrid-worker-{cluster_id}-112-perm-11-na<-0med"},
            {"name": "somevolume"},
            {"name": "bibigrid-master-4242-0-tmp-0"},
            {"name": "bibigrid-master-4242-0-semiperm-0"},
            {"name": "bibigrid-master-4242-0-perm-0"},
            {"name": "bibigrid-worker-4242-0-tmp-0"},
            {"name": "bibigrid-worker-4242-0-semiperm-0"},
            {"name": "bibigrid-worker-4242-0-perm-0"},
            {"name": f"master-{cluster_id}-0-tmp-0"},
            {"name": f"master-{cluster_id}-0-semiperm-0"},
            {"name": f"master-{cluster_id}-0-perm-0"},
        ]

        provider.list_volumes.return_value = volumes

        # Call the method under test
        _ = terminate.delete_non_permanent_volumes(provider, cluster_id, log)

        # Expected captured volumes
        expected_calls = [call({'name': 'bibigrid-master-21-tmp-0'}),
                          call({'name': 'bibigrid-master-21-semiperm-0'}),
                          call({'name': 'bibigrid-master-21-tmp-0-na<-0med'}),
                          call({'name': 'bibigrid-master-21-semiperm-0-na<-0med'}),
                          call({'name': 'bibigrid-worker-21-0-tmp-0'}),
                          call({'name': 'bibigrid-worker-21-11-semiperm-0'}),
                          call({'name': 'bibigrid-worker-21-0-tmp-0-na<-0med'}),
                          call({'name': 'bibigrid-worker-21-11-semiperm-0-na<-0med'})]

        # Assert that the regex only captured the expected volumes
        self.assertEqual(expected_calls, provider.delete_volume.call_args_list)

    def test_terminate_servers(self):
        cluster_id = "21"
        provider = MagicMock()
        log = MagicMock()

        # List of test servers
        servers = [
            # Should be captured by the regex
            {"name": f"bibigrid-master-{cluster_id}", "id": 42},
            {"name": f"bibigrid-worker-{cluster_id}-0", "id": 42},
            {"name": f"bibigrid-worker-{cluster_id}-11", "id": 42},
            {"name": f"bibigrid-vpngtw-{cluster_id}-222", "id": 42},

            # Should NOT be captured by the regex
            {"name": "some-other-server", "id": 42},
            {"name": "bibigrid-master-4242", "id": 42},
            {"name": "bibigrid-worker-4242-0", "id": 42},
            {"name": "bibigrid-vpngtw-4242-0", "id": 42},
        ]

        provider.list_servers.return_value = servers

        # Patch terminate_server from bibigrid.core.actions.terminate
        with patch("bibigrid.core.actions.terminate.terminate_server") as mock_terminate_server:
            # Call the method under test
            _ = terminate.terminate_servers(cluster_id, provider, log)

            # Expected captured servers
            expected_calls = [
                call(provider, {"name": f"bibigrid-master-{cluster_id}", "id": 42}, log),
                call(provider, {"name": f"bibigrid-worker-{cluster_id}-0", "id": 42}, log),
                call(provider, {"name": f"bibigrid-worker-{cluster_id}-11", "id": 42}, log),
                call(provider, {"name": f"bibigrid-vpngtw-{cluster_id}-222", "id": 42}, log),
            ]

            # Assert that terminate_server was called only for the expected servers
            mock_terminate_server.assert_has_calls(expected_calls, any_order=False)

            # Assert that the total number of calls matches the expected calls
            self.assertEqual(mock_terminate_server.call_count, len(expected_calls))

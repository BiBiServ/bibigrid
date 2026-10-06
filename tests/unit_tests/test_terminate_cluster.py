"""
Module to test terminate
"""
from unittest import TestCase
from unittest.mock import MagicMock, patch, call

from bibigrid.core import startup
from bibigrid.core.utility.statics.create_statics import master_identifier, KEY_NAME
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
        provider.list_servers.return_value = [{"name": master_identifier(cluster_id=str(cluster_id)), "id": 21}]
        provider.delete_server.return_value = True
        provider.delete_keypair.return_value = True
        provider.delete_volume.return_value = True
        provider.list_volumes.return_value = [
            {"name": f"{master_identifier(cluster_id=str(cluster_id))}-tmp-0", "id": 42}]
        provider.list_volumes([{"name": "bibigrid-master-i950vaoqzfbwpnq-tmp-0"}])
        provider.delete_security_group.return_value = True
        provider.delete_application_credentials.return_value = True
        terminate.terminate(str(cluster_id), [provider], [None], startup.LOG, False, True)
        provider.delete_server.assert_called_with(21, delete_ips=True)
        provider.delete_keypair.assert_called_with(KEY_NAME.format(cluster_id=cluster_id))
        mock_output.assert_called_with(cluster_server_state=[provider.delete_server.return_value],
                                       cluster_keypair_state=[provider.delete_keypair.return_value],
                                       cluster_security_group_state=[provider.delete_security_group.return_value],
                                       cluster_volume_state=[[True]],
                                       ac_state=provider.delete_application_credentials.return_value,
                                       cluster_id=str(cluster_id),
                                       log=startup.LOG)

    @patch("bibigrid.core.actions.terminate.delete_local_keypairs")
    @patch("bibigrid.core.actions.terminate.terminate_output")
    def test_terminate_delete_ip_false(self, mock_output, mock_local):
        mock_local.return_value = True
        provider = MagicMock()
        provider.cloud_specification["auth"]["project_name"] = 32
        cluster_id = 42
        provider.list_servers.return_value = [{"name": master_identifier(cluster_id=str(cluster_id)), "id": 21}]
        provider.delete_server.return_value = True
        provider.delete_keypair.return_value = True
        provider.delete_volume.return_value = True
        provider.list_volumes.return_value = [
            {"name": f"{master_identifier(cluster_id=str(cluster_id))}-tmp-0", "id": 42}]
        provider.list_volumes([{"name": "bibigrid-master-i950vaoqzfbwpnq-tmp-0"}])
        provider.delete_security_group.return_value = True
        provider.delete_application_credentials.return_value = True
        terminate.terminate(str(cluster_id), [provider], ["123"], startup.LOG, False, True)
        provider.delete_server.assert_called_with(21, delete_ips=False)

    @patch("bibigrid.core.actions.terminate.delete_local_keypairs")
    @patch("logging.info")
    def test_terminate_none(self, _, mock_local):
        mock_local.return_value = True
        provider = MagicMock()
        provider[0].specification["auth"]["project_name"] = "test_project_name"
        cluster_id = 42
        provider.list_servers.return_value = [
            {"name": master_identifier(cluster_id=str(cluster_id + 1)), "id": 21}]
        provider.delete_keypair.return_value = False
        terminate.terminate(str(cluster_id), [provider], [None], startup.LOG, False, True)
        provider.delete_server.assert_not_called()
        provider.delete_keypair.assert_called_with(
            KEY_NAME.format(cluster_id=str(cluster_id)))  # since keypair is not called

    def test_delete_non_pemanent_volumes(self):
        provider = MagicMock()
        log = MagicMock()
        cluster_id = 21

        # List of test volumes
        volumes = [
            # Should be captured by the regex
            {"name": f"bibigrid-master-{cluster_id}-tmp-0", "id":0},
            {"name": f"bibigrid-master-{cluster_id}-semiperm-0", "id":1},
            {"name": f"bibigrid-master-{cluster_id}-tmp-0-named", "id":2},
            {"name": f"bibigrid-master-{cluster_id}-semiperm-0-named", "id":3},
            {"name": f"bibigrid-worker-{cluster_id}-0-tmp-0", "id":4},
            {"name": f"bibigrid-worker-{cluster_id}-11-semiperm-0", "id":5},
            {"name": f"bibigrid-worker-{cluster_id}-0-tmp-0-named", "id":6},
            {"name": f"bibigrid-worker-{cluster_id}-11-semiperm-0-named", "id":7},

            # Should NOT be captured by the regex
            {"name": f"bibigrid-master-{cluster_id}-perm-0", "id":"42"},
            {"name": f"bibigrid-master-{cluster_id}-perm-11-named", "id":"42"},
            {"name": f"bibigrid-worker-{cluster_id}-112-perm-0", "id":"42"},
            {"name": f"bibigrid-worker-{cluster_id}-112-perm-11-named", "id":"42"},
            {"name": "somevolume", "id":"42"},
            {"name": "bibigrid-master-4242-0-tmp-0", "id":"42"},
            {"name": "bibigrid-master-4242-0-semiperm-0", "id":"42"},
            {"name": "bibigrid-master-4242-0-perm-0", "id":"42"},
            {"name": "bibigrid-worker-4242-0-tmp-0", "id":"42"},
            {"name": "bibigrid-worker-4242-0-semiperm-0", "id":"42"},
            {"name": "bibigrid-worker-4242-0-perm-0", "id":"42"},
            {"name": f"master-{cluster_id}-0-tmp-0", "id":"42"},
            {"name": f"master-{cluster_id}-0-semiperm-0", "id":"42"},
            {"name": f"master-{cluster_id}-0-perm-0", "id":"42"},
        ]

        provider.list_volumes.return_value = volumes

        # Call the method under test
        _ = terminate.delete_non_permanent_volumes(provider, cluster_id, log)

        # Expected captured volumes
        expected_calls = [call(x) for x in range(8)]

        # Assert that the regex only captured the expected volumes
        self.assertEqual(expected_calls, provider.delete_volume.call_args_list)

    def test_terminate_servers(self):
        cluster_id = "21"
        provider = MagicMock()
        log = MagicMock()

        # List of test servers
        servers = [
            # Should be captured by the regex
            {"name": f"bibigrid-master-{cluster_id}", "id": 0},
            {"name": f"bibigrid-worker-{cluster_id}-0", "id": 1},
            {"name": f"bibigrid-worker-{cluster_id}-11", "id": 2},
            {"name": f"bibigrid-vpngtw-{cluster_id}-222", "id": 3},

            # Should NOT be captured by the regex
            {"name": "some-other-server", "id": 42},
            {"name": "bibigrid-master-4242", "id": 42},
            {"name": "bibigrid-worker-4242-0", "id": 42},
            {"name": "bibigrid-vpngtw-4242-0", "id": 42},
        ]
        provider.list_servers.return_value = servers

        expected_calls = [
            call(provider, servers[0], True, log),
            call(provider, servers[1], True, log),
            call(provider, servers[2], True, log),
            call(provider, servers[3], True, log),
        ]

        with patch("bibigrid.core.actions.terminate.terminate_server") as mock_terminate_server:
            _ = terminate.terminate_servers(cluster_id, provider, None, log)
            mock_terminate_server.assert_has_calls(expected_calls, any_order=False)
            self.assertEqual(mock_terminate_server.call_count, len(expected_calls))

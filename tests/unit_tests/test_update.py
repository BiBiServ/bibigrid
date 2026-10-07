"""
Module to test update
"""
from unittest import TestCase
from unittest.mock import patch, MagicMock

from bibigrid.core import startup
from bibigrid.core.actions import create, update
from bibigrid.models.exceptions import ConfigurationException, ExecutionException


# pylint: disable=too-many-positional-arguments
class TestUpdate(TestCase):
    """
    Class to test update and compute_worker_diff
    """

    @staticmethod
    def _creator(configurations, providers=None):
        provider = providers[0] if providers else MagicMock()
        provider.list_servers.return_value = []
        return create.Create(providers=providers or [provider], configurations=configurations, config_path="",
                             log=startup.LOG, cluster_id="abc123")

    def test_diff_pure_append_only_new_index_returned(self):
        configurations = [{"workerInstances": [{"type": "t", "image": "i", "count": 4, "onDemand": False}]}]
        creator = self._creator(configurations)
        cluster = {"workers": [{"name": f"bibigrid-worker-abc123-{i}"} for i in range(3)]}
        to_create = update.compute_worker_diff(creator, cluster)
        self.assertEqual([3], [index for _, index, _, _ in to_create])

    def test_diff_on_demand_group_needs_no_new_instance(self):
        configurations = [{"workerInstances": [{"type": "t", "image": "i", "count": 2, "onDemand": True}]}]
        creator = self._creator(configurations)
        self.assertEqual([], update.compute_worker_diff(creator, {"workers": []}))

    def test_diff_rejects_shrink_or_reorder(self):
        configurations = [{"workerInstances": [{"type": "t", "image": "i", "count": 2, "onDemand": False}]}]
        creator = self._creator(configurations)
        # index 2 is running but the new config only expects indices 0 and 1
        cluster = {"workers": [{"name": f"bibigrid-worker-abc123-{i}"} for i in range(3)]}
        with self.assertRaises(ConfigurationException):
            update.compute_worker_diff(creator, cluster)

    def test_diff_rejects_on_demand_flip_of_running_worker(self):
        configurations = [{"workerInstances": [{"type": "t", "image": "i", "count": 1, "onDemand": True}]}]
        creator = self._creator(configurations)
        cluster = {"workers": [{"name": "bibigrid-worker-abc123-0"}]}
        with self.assertRaises(ConfigurationException):
            update.compute_worker_diff(creator, cluster)

    @patch("bibigrid.core.actions.update.dict_clusters")
    @patch("bibigrid.core.actions.update.terminate_server")
    @patch.object(create.Create, "upload_data")
    @patch.object(create.Create, "start_worker")
    @patch.object(create.Create, "create_defaults")
    @patch.object(create.Create, "prepare_configurations")
    @patch("bibigrid.core.actions.update.ssh_handler.read_remote_file")
    @patch("bibigrid.core.actions.update.cluster_ssh_handler.get_ssh_connection_info")
    def test_update_happy_path_creates_only_the_delta(self, mock_ssh_info, mock_read_remote_file, mock_prepare,
                                                       mock_defaults, mock_start_worker, mock_upload,
                                                       mock_terminate_server, mock_dict_clusters):
        provider = MagicMock()
        mock_ssh_info.return_value = ("1.2.3.4", "ubuntu", "/tmp/key")
        mock_read_remote_file.return_value = "existing-munge-key"
        provider.get_server.return_value = {"private_v4": "10.0.0.1", "volumes": []}
        configuration = {"workerInstances": [{"type": "t", "image": "i", "count": 4, "onDemand": False}]}
        creator = self._creator([configuration], providers=[provider])
        cluster = {"master": {"name": "bibigrid-master-abc123"},
                  "workers": [{"name": f"bibigrid-worker-abc123-{i}"} for i in range(3)]}
        mock_dict_clusters.return_value = {"abc123": cluster}
        self.assertEqual(0, update.update(creator, startup.LOG))
        mock_start_worker.assert_called_once_with(configuration["workerInstances"][0], 3, configuration, provider)
        mock_upload.assert_called_once_with("/tmp/key", clean_playbook=True)
        mock_terminate_server.assert_not_called()
        # regression: update() must populate securityGroups itself (start_worker requires it) but must not
        # recreate the cluster's security group - that group already exists from the original create()
        self.assertEqual(["default-abc123"], configuration["securityGroups"])
        provider.create_security_group.assert_not_called()
        # regression: update() must reuse the master's already-deployed munge key rather than letting
        # ansible_configurator's SLURM_CONF default hand out (and redeploy) a fresh random one
        self.assertEqual("existing-munge-key", configuration["slurmConf"]["munge_key"])

    @patch("bibigrid.core.actions.update.dict_clusters")
    @patch("bibigrid.core.actions.update.terminate_server")
    @patch.object(create.Create, "upload_data")
    @patch.object(create.Create, "start_worker")
    @patch.object(create.Create, "create_defaults")
    @patch.object(create.Create, "prepare_configurations")
    @patch("bibigrid.core.actions.update.ssh_handler.read_remote_file")
    @patch("bibigrid.core.actions.update.cluster_ssh_handler.get_ssh_connection_info")
    def test_update_preserves_user_configured_security_groups(self, mock_ssh_info, mock_read_remote_file,
                                                               mock_prepare, mock_defaults, mock_start_worker,
                                                               mock_upload, mock_terminate_server,
                                                               mock_dict_clusters):
        provider = MagicMock()
        mock_ssh_info.return_value = ("1.2.3.4", "ubuntu", "/tmp/key")
        mock_read_remote_file.return_value = "existing-munge-key"
        provider.get_server.return_value = {"private_v4": "10.0.0.1", "volumes": []}
        configuration = {"securityGroups": ["extra-group"],
                         "workerInstances": [{"type": "t", "image": "i", "count": 1, "onDemand": False}]}
        creator = self._creator([configuration], providers=[provider])
        cluster = {"master": {"name": "bibigrid-master-abc123"}, "workers": []}
        mock_dict_clusters.return_value = {"abc123": cluster}
        self.assertEqual(0, update.update(creator, startup.LOG))
        self.assertEqual(["default-abc123", "extra-group"], configuration["securityGroups"])

    @patch("bibigrid.core.actions.update.dict_clusters")
    @patch("bibigrid.core.actions.update.terminate_server")
    @patch.object(create.Create, "upload_data")
    @patch.object(create.Create, "start_worker")
    @patch.object(create.Create, "create_defaults")
    @patch.object(create.Create, "prepare_configurations")
    @patch("bibigrid.core.actions.update.ssh_handler.read_remote_file")
    @patch("bibigrid.core.actions.update.cluster_ssh_handler.get_ssh_connection_info")
    def test_update_on_demand_only_starts_nothing_but_still_uploads(self, mock_ssh_info, mock_read_remote_file,
                                                                     mock_prepare, mock_defaults, mock_start_worker,
                                                                     mock_upload, mock_terminate_server,
                                                                     mock_dict_clusters):
        provider = MagicMock()
        mock_ssh_info.return_value = ("1.2.3.4", "ubuntu", "/tmp/key")
        mock_read_remote_file.return_value = "existing-munge-key"
        provider.get_server.return_value = {"private_v4": "10.0.0.1", "volumes": []}
        configuration = {"workerInstances": [{"type": "t", "image": "i", "count": 2, "onDemand": True}]}
        creator = self._creator([configuration], providers=[provider])
        cluster = {"master": {"name": "bibigrid-master-abc123"}, "workers": []}
        mock_dict_clusters.return_value = {"abc123": cluster}
        self.assertEqual(0, update.update(creator, startup.LOG))
        mock_start_worker.assert_not_called()
        mock_upload.assert_called_once_with("/tmp/key", clean_playbook=True)
        mock_terminate_server.assert_not_called()

    @patch("bibigrid.core.actions.update.dict_clusters")
    @patch("bibigrid.core.actions.update.terminate_server")
    @patch.object(create.Create, "upload_data")
    @patch.object(create.Create, "start_worker")
    @patch.object(create.Create, "create_defaults")
    @patch.object(create.Create, "prepare_configurations")
    @patch("bibigrid.core.actions.update.ssh_handler.read_remote_file")
    @patch("bibigrid.core.actions.update.cluster_ssh_handler.get_ssh_connection_info")
    def test_update_rejects_shrink_without_creating_or_terminating_anything(self, mock_ssh_info,
                                                                            mock_read_remote_file, mock_prepare,
                                                                            mock_defaults, mock_start_worker,
                                                                            mock_upload, mock_terminate_server,
                                                                            mock_dict_clusters):
        provider = MagicMock()
        mock_ssh_info.return_value = ("1.2.3.4", "ubuntu", "/tmp/key")
        mock_read_remote_file.return_value = "existing-munge-key"
        provider.get_server.return_value = {"private_v4": "10.0.0.1", "volumes": []}
        configuration = {"workerInstances": [{"type": "t", "image": "i", "count": 2, "onDemand": False}]}
        creator = self._creator([configuration], providers=[provider])
        cluster = {"master": {"name": "bibigrid-master-abc123"},
                  "workers": [{"name": f"bibigrid-worker-abc123-{i}"} for i in range(3)]}
        mock_dict_clusters.return_value = {"abc123": cluster}
        self.assertEqual(1, update.update(creator, startup.LOG))
        mock_start_worker.assert_not_called()
        mock_upload.assert_not_called()
        # an update failure must never fall back to the blanket, whole-cluster terminate() create() uses
        mock_terminate_server.assert_not_called()

    @patch("bibigrid.core.actions.update.dict_clusters")
    def test_update_rejects_multi_provider_clusters(self, mock_dict_clusters):
        creator = self._creator([{"workerInstances": []}, {"workerInstances": []}],
                                providers=[MagicMock(), MagicMock()])
        mock_dict_clusters.return_value = {"abc123": {"master": {"name": "bibigrid-master-abc123"}, "workers": []}}
        self.assertEqual(1, update.update(creator, startup.LOG))

    @patch("bibigrid.core.actions.update.dict_clusters")
    def test_update_aborts_if_cluster_not_found(self, mock_dict_clusters):
        creator = self._creator([{"workerInstances": []}])
        mock_dict_clusters.return_value = {}
        self.assertEqual(1, update.update(creator, startup.LOG))

    @patch("bibigrid.core.actions.update.dict_clusters")
    @patch("bibigrid.core.actions.update.terminate_server")
    @patch.object(create.Create, "upload_data")
    @patch.object(create.Create, "create_defaults")
    @patch.object(create.Create, "prepare_configurations")
    @patch("bibigrid.core.actions.update.ssh_handler.read_remote_file")
    @patch("bibigrid.core.actions.update.cluster_ssh_handler.get_ssh_connection_info")
    def test_update_aborts_if_existing_munge_key_cannot_be_read(self, mock_ssh_info, mock_read_remote_file,
                                                                 mock_prepare, mock_defaults, mock_upload,
                                                                 mock_terminate_server, mock_dict_clusters):
        provider = MagicMock()
        mock_ssh_info.return_value = ("1.2.3.4", "ubuntu", "/tmp/key")
        mock_read_remote_file.return_value = None
        provider.get_server.return_value = {"private_v4": "10.0.0.1", "volumes": []}
        configuration = {"workerInstances": [{"type": "t", "image": "i", "count": 4, "onDemand": False}]}
        creator = self._creator([configuration], providers=[provider])
        cluster = {"master": {"name": "bibigrid-master-abc123"}, "workers": []}
        mock_dict_clusters.return_value = {"abc123": cluster}
        self.assertEqual(1, update.update(creator, startup.LOG))
        # must not proceed to (re-)generate a fresh, mismatched munge key and deploy it
        mock_upload.assert_not_called()

    @patch("bibigrid.core.actions.update.dict_clusters")
    @patch("bibigrid.core.actions.update.terminate_server")
    @patch.object(create.Create, "upload_data")
    @patch.object(create.Create, "start_worker")
    @patch.object(create.Create, "create_defaults")
    @patch.object(create.Create, "prepare_configurations")
    @patch("bibigrid.core.actions.update.ssh_handler.read_remote_file")
    @patch("bibigrid.core.actions.update.cluster_ssh_handler.get_ssh_connection_info")
    def test_update_failure_rolls_back_only_workers_created_this_run(self, mock_ssh_info, mock_read_remote_file,
                                                                      mock_prepare, mock_defaults, mock_start_worker,
                                                                      mock_upload, mock_terminate_server,
                                                                      mock_dict_clusters):
        provider = MagicMock()
        mock_ssh_info.return_value = ("1.2.3.4", "ubuntu", "/tmp/key")
        mock_read_remote_file.return_value = "existing-munge-key"
        new_worker_server = {"name": "bibigrid-worker-abc123-3"}
        provider.get_server.side_effect = lambda name: (
            {"private_v4": "10.0.0.1", "volumes": []} if name == "bibigrid-master-abc123" else new_worker_server)
        mock_upload.side_effect = ExecutionException("upload failed")
        configuration = {"workerInstances": [{"type": "t", "image": "i", "count": 4, "onDemand": False}]}
        creator = self._creator([configuration], providers=[provider])
        cluster = {"master": {"name": "bibigrid-master-abc123"},
                  "workers": [{"name": f"bibigrid-worker-abc123-{i}"} for i in range(3)]}
        mock_dict_clusters.return_value = {"abc123": cluster}
        self.assertEqual(1, update.update(creator, startup.LOG))
        mock_start_worker.assert_called_once()
        # only the worker update() created this run is cleaned up; the rest of the cluster is left alone
        mock_terminate_server.assert_called_once_with(provider, new_worker_server, startup.LOG)

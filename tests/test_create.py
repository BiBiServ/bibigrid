"""
Module to test create
"""
import os
from unittest import TestCase
from unittest.mock import patch, Mock, MagicMock, mock_open

from bibigrid.core import startup
from bibigrid.core.actions import create


class TestCreate(TestCase):
    """
    Class to test create
    """

    # pylint: disable=R0904
    @patch("bibigrid.core.utility.handler.ssh_handler.get_add_ssh_public_key_commands")
    @patch("bibigrid.core.utility.id_generation.generate_safe_cluster_id")
    def test_init(self, mock_id, mock_ssh):
        cluster_id = "21"
        provider = MagicMock()
        provider_dict = {'cloud_specification': {'auth': {'project_name': 'project_name'}}}
        provider.__getitem__.side_effect = provider_dict.__getitem__
        key_name = create.KEY_NAME.format(cluster_id=cluster_id)
        mock_id.return_value = cluster_id
        mock_ssh.return_value = [32]
        creator = create.Create([provider], [{}], "path", startup.LOG, False)
        self.assertEqual(cluster_id, creator.cluster_id)
        self.assertEqual("ubuntu", creator.ssh_user)
        self.assertEqual([32], creator.ssh_add_public_key_commands)
        self.assertEqual(key_name, creator.key_name)
        mock_id.assert_called_with([provider])

    @patch("bibigrid.core.utility.handler.ssh_handler.get_add_ssh_public_key_commands")
    @patch("bibigrid.core.utility.id_generation.generate_safe_cluster_id")
    def test_init_with_cluster_id(self, mock_id, mock_ssh):
        cluster_id = "21"
        provider = MagicMock()
        provider_dict = {'cloud_specification': {'auth': {'project_name': 'project_name'}}}
        provider.__getitem__.side_effect = provider_dict.__getitem__
        key_name = create.KEY_NAME.format(cluster_id=cluster_id)
        mock_ssh.return_value = [32]
        creator = create.Create([provider], [{}], "path", startup.LOG, False, cluster_id)
        self.assertEqual(cluster_id, creator.cluster_id)
        self.assertEqual("ubuntu", creator.ssh_user)
        self.assertEqual([32], creator.ssh_add_public_key_commands)
        self.assertEqual(key_name, creator.key_name)
        mock_id.assert_not_called()

    @patch("bibigrid.core.utility.handler.ssh_handler.get_add_ssh_public_key_commands")
    @patch("bibigrid.core.utility.id_generation.generate_safe_cluster_id")
    def test_init_username(self, mock_id, mock_ssh):
        cluster_id = "21"
        mock_id.return_value = cluster_id
        mock_ssh.return_value = [32]
        creator = create.Create([MagicMock()], [{"sshUser": "ssh"}], "path", startup.LOG, False)
        self.assertEqual("ssh", creator.ssh_user)

    @patch("subprocess.check_output")
    def test_generate_keypair(self, mock_subprocess):
        provider = MagicMock()
        provider.list_servers.return_value = []
        creator = create.Create([provider], [{}], "", startup.LOG)
        public_key = "data"
        with patch("builtins.open", mock_open(read_data=public_key)):
            creator.generate_keypair()
        provider.create_keypair.assert_called_with(name=creator.key_name, public_key=public_key)
        mock_subprocess.assert_called_with(f'ssh-keygen -t ecdsa -f {create.KEY_FOLDER}{creator.key_name} -P ""',
                                           shell=True)

    # TODO: Rewrite start instance tests

    def test_prepare_master_args(self):
        provider = MagicMock()
        provider.list_servers.return_value = []
        external_network = "externalTest"
        provider.get_external_netowrk.return_value = external_network
        configuration = {"network": 42, "masterInstance": "Some"}
        creator = create.Create([provider], [configuration], "", startup.LOG)
        volume_return = [42]
        with patch.object(creator, "prepare_volumes", return_value=volume_return) as prepare_mock:
            self.assertEqual((create.MASTER_IDENTIFIER, configuration["masterInstance"], volume_return),
                             creator.prepare_vpn_or_master_args(configuration, provider))
            prepare_mock.assert_called_with(provider, [])

    def test_prepare_vpn_args(self):
        provider = MagicMock()
        provider.list_servers.return_value = []
        external_network = "externalTest"
        provider.get_external_netowrk.return_value = external_network
        configuration = {"network": 42, "vpnInstance": "Some"}
        creator = create.Create([provider], [configuration], "", startup.LOG)
        volume_return = [42]
        with patch.object(creator, "prepare_volumes", return_value=volume_return) as prepare_mock:
            self.assertEqual((create.VPN_WORKER_IDENTIFIER, configuration["vpnInstance"], []),
                             creator.prepare_vpn_or_master_args(configuration, provider))
            prepare_mock.assert_not_called()

    def test_prepare_args_keyerror(self):
        provider = MagicMock()
        provider.list_servers.return_value = []
        external_network = "externalTest"
        provider.get_external_netowrk.return_value = external_network
        configuration = {"network": 42}
        creator = create.Create([provider], [configuration], "", startup.LOG)
        volume_return = [42]
        with patch.object(creator, "prepare_volumes", return_value=volume_return) as prepare_mock:
            with self.assertRaises(KeyError):
                self.assertEqual((create.VPN_WORKER_IDENTIFIER, configuration["vpnInstance"], []),
                                 creator.prepare_vpn_or_master_args(configuration, provider))
            prepare_mock.assert_not_called()

    @patch("bibigrid.core.utility.handler.ssh_handler.ansible_preparation")
    def test_initialize_instances_master(self, mock_ansible):
        provider = MagicMock()
        provider.list_servers.return_value = []
        floating_ip = 21
        configuration = {"masterInstance": 42, "floating_ip": floating_ip}
        creator = create.Create([provider], [configuration], "", startup.LOG)
        creator.initialize_instances()
        mock_ansible.assert_called_with(floating_ip=floating_ip, private_key=create.KEY_FOLDER + creator.key_name,
                                        username=creator.ssh_user, commands=[], log=startup.LOG, gateway={})

    def test_prepare_volumes_none(self):
        provider = MagicMock()
        provider.list_servers.return_value = []
        provider.get_volume_by_id_or_name.return_value = 42
        provider.create_volume_from_snapshot = 21
        configuration = {"vpnInstance": 42}
        creator = create.Create([provider], [configuration], "", startup.LOG)
        self.assertEqual(set(), creator.prepare_volumes(provider, []))

    def test_prepare_volumes_volume(self):
        provider = MagicMock()
        provider.list_servers.return_value = []
        provider.get_volume_by_id_or_name.return_value = 42
        provider.create_volume_from_snapshot = 21
        configuration = {"vpnInstance": 42}
        c = create.Create([provider], [configuration], "")
        self.assertEqual([42], c.prepare_volumes(provider, ["Test"]))

    def test_prepare_volumes_snapshot(self):
        provider = MagicMock()
        provider.list_servers.return_value = []
        provider.get_volume_by_id_or_name.return_value = None
        provider.create_volume_from_snapshot.return_value = 21
        configuration = {"vpnInstance": 42}
        c = create.Create([provider], [configuration], "")
        self.assertEqual([21], c.prepare_volumes(provider, ["Test"]))

    @patch("logging.warning")
    def test_prepare_volumes_mismatch(self, mock_log):
        provider = MagicMock()
        provider.list_servers.return_value = []
        provider.get_volume_by_id_or_name.return_value = None
        provider.create_volume_from_snapshot.return_value = None
        configuration = {"vpnInstance": 42}
        c = create.Create([provider], [configuration], "")
        mount = "Test"
        self.assertEqual([], c.prepare_volumes(provider, [mount]))
        mock_log.assert_called_with(f"Mount {mount} is neither a snapshot nor a volume.")

    def test_prepare_configurations_no_network(self):
        provider = MagicMock()
        provider.list_servers.return_value = []
        network = "network"
        provider.get_network_id_by_subnet.return_value = network
        configuration = {"subnet": 42}
        c = create.Create([provider], [configuration], "")
        c.prepare_configurations()
        provider.get_network_id_by_subnet.assert_called_with(42)
        self.assertEqual(network, configuration["network"])
        self.assertEqual(c.ssh_user, configuration["sshUser"])

    def test_prepare_configurations_no_subnet(self):
        provider = MagicMock()
        provider.list_servers.return_value = []
        subnet = ["subnet"]
        provider.get_subnet_ids_by_network.return_value = subnet
        configuration = {"network": 42}
        creator = create.Create([provider], [configuration], "", startup.LOG)
        creator.prepare_configurations()
        provider.get_subnet_ids_by_network.assert_called_with(42)
        self.assertEqual(subnet, configuration["subnet"])
        self.assertEqual(creator.ssh_user, configuration["sshUser"])

    def test_prepare_configurations_none(self):
        provider = MagicMock()
        provider.list_servers.return_value = []
        configuration = {}
        creator = create.Create([provider], [configuration], "", startup.LOG)
        with self.assertRaises(KeyError):
            creator.prepare_configurations()

    @patch("bibigrid.core.utility.ansible_configurator.configure_ansible_yaml")
    @patch("bibigrid.core.utility.handler.ssh_handler.execute_ssh")
    def test_upload_playbooks(self, mock_ssh, mock_configure_ansible):
        provider = MagicMock()
        provider.list_servers.return_value = []
        configuration = {}
        creator = create.Create([provider], [configuration], "", startup.LOG)
        creator.master_ip = 42
        creator.upload_data()
        mock_configure_ansible.assert_called_with(providers=creator.providers, configurations=creator.configurations,
                                                  cluster_id=creator.cluster_id)
        mock_ssh.assert_called_with(floating_ip=creator.master_ip, private_key=create.KEY_FOLDER + creator.key_name,
                                    username=creator.ssh_user,
                                    filepaths=[(os.path.expanduser("/Documents/Repos/bibigrid/"
                                                                   "resources/playbook/"), "playbook")],
                                    commands=['echo ansible_start'])

    @patch("threading.Thread")
    def test_start_start_instances_thread(self, mock_thread):
        provider = MagicMock()
        provider.list_servers.return_value = []
        configuration = {}
        c = create.Create([provider], [configuration], "")
        start_instances_mock_thread = Mock()
        mock_thread.return_value = start_instances_mock_thread
        c.start_start_instances_threads()
        mock_thread.assert_called_with(target=c.start_instances, args=[configuration, provider])
        start_instances_mock_thread.start.assert_called()
        start_instances_mock_thread.join.assert_called()

    @patch.object(create.Create, "generate_keypair")
    @patch.object(create.Create, "prepare_configurations")
    @patch.object(create.Create, "start_start_instances_threads")
    @patch.object(create.Create, "upload_data")
    @patch.object(create.Create, "print_cluster_start_info")
    @patch("bibigrid.core.actions.terminateCluster.terminate_cluster")
    def test_create_non_debug(self, mock_terminate, mock_info, mock_up, mock_start, mock_conf, mock_key):
        provider = MagicMock()
        provider.list_servers.return_value = []
        configuration = {}
        c = create.Create([provider], [configuration], "", False)
        self.assertEqual(0, c.create())
        for mock in [mock_info, mock_up, mock_start, mock_conf, mock_key]:
            mock.assert_called()
        mock_terminate.assert_not_called()

    @patch.object(create.Create, "generate_keypair")
    @patch.object(create.Create, "prepare_configurations")
    @patch.object(create.Create, "start_start_instances_threads")
    @patch.object(create.Create, "upload_data")
    @patch.object(create.Create, "print_cluster_start_info")
    @patch("bibigrid.core.actions.terminateCluster.terminate_cluster")
    def test_create_non_debug_upload_raise(self, mock_terminate, mock_info, mock_up, mock_start, mock_conf, mock_key):
        provider = MagicMock()
        provider.list_servers.return_value = []
        configuration = {}
        c = create.Create([provider], [configuration], "", False)
        mock_up.side_effect = [ConnectionError()]
        self.assertEqual(1, c.create())
        for mock in [mock_start, mock_conf, mock_key, mock_up]:
            mock.assert_called()
        for mock in [mock_info]:
            mock.assert_not_called()
        mock_terminate.assert_called_with(cluster_id=c.cluster_id, providers=[provider], debug=False)

    @patch.object(create.Create, "generate_keypair")
    @patch.object(create.Create, "prepare_configurations")
    @patch.object(create.Create, "start_start_instances_threads")
    @patch.object(create.Create, "upload_data")
    @patch.object(create.Create, "print_cluster_start_info")
    @patch("bibigrid.core.actions.terminateCluster.terminate_cluster")
    def test_create_debug(self, mock_terminate, mock_info, mock_up, mock_start, mock_conf, mock_key):
        provider = MagicMock()
        provider.list_servers.return_value = []
        configuration = {}
        c = create.Create([provider], [configuration], "", True)
        self.assertEqual(0, c.create())
        for mock in [mock_info, mock_up, mock_start, mock_conf, mock_key]:
            mock.assert_called()
        mock_terminate.assert_called_with(cluster_id=c.cluster_id, providers=[provider], debug=True)

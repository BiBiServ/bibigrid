import socket
from unittest import TestCase
from unittest.mock import mock_open, Mock, MagicMock, patch, call

from paramiko.ssh_exception import NoValidConnectionsError

import bibigrid.core.utility.handler.ssh_handler as sshHandler


class TestSshHandler(TestCase):
    def test_get_add_ssh_public_key_commands_none(self):
        ssh_public_key_files = []
        self.assertEqual([], sshHandler.get_add_ssh_public_key_commands(ssh_public_key_files))

    def test_get_add_ssh_public_key_commands_line(self):
        ssh_public_key_files = [42]
        line = "42"
        expected = [f"echo {line} >> .ssh/authorized_keys"]
        with patch("builtins.open", mock_open(read_data=line)) as mock_file:
            self.assertEqual(expected, sshHandler.get_add_ssh_public_key_commands(ssh_public_key_files))
        mock_file.assert_called_with(42)

    def test_copy_to_server_file(self):
        sftp = Mock()
        sftp.put = MagicMock(return_value=True)
        with patch("os.path.isfile") as mock_isfile:
            mock_isfile.return_value = True
            sshHandler.copy_to_server(sftp, "Jim", "Joe")
        sftp.put.assert_called_with("Jim", "Joe")

    @patch("os.listdir")
    def test_copy_to_server_folder(self, mock_listdir):
        sftp = Mock()
        sftp.mkdir = MagicMock()
        mock_listdir.return_value = []
        with patch("os.path.isfile") as mock_isfile:
            mock_isfile.return_value = False
            sshHandler.copy_to_server(sftp, "Jim", "Joe")
        mock_listdir.assert_called_with("Jim")
        sftp.mkdir.assert_called_with("Joe")

    @patch("logging.info")
    def test_is_active(self, mock_log):
        client = Mock()
        client.connect = MagicMock(return_value=True)
        self.assertFalse(sshHandler.is_active(client, 42, 32, 22, timeout=5))
        mock_log.assert_not_called()

    @patch("logging.info")
    def test_is_active_second(self, mock_log):
        client = Mock()
        client.connect = MagicMock(side_effect=[NoValidConnectionsError({('127.0.0.1', 22): socket.error}), True])
        self.assertFalse(sshHandler.is_active(client, 42, 32, 22, timeout=5))
        mock_log.assert_called()

    @patch("logging.info")
    def test_is_active_exception(self, mock_log):
        client = Mock()
        client.connect = MagicMock(side_effect=NoValidConnectionsError({('127.0.0.1', 22): socket.error}))
        with self.assertRaises(ConnectionError):
            sshHandler.is_active(client, 42, 32, 22, timeout=0)
        client.connect.assert_called_with(hostname=42, username=22, pkey=32)
        mock_log.assert_called()

    @patch("bibigrid.core.utility.handler.sshHandler.execute_ssh_cml_commands")
    @patch("paramiko.ECDSAKey.from_private_key_file")
    @patch("paramiko.SSHClient")
    def test_execute_ssh(self, mock_client, mock_paramiko_key, mock_exec):
        mock_paramiko_key.return_value = 2
        client = Mock()
        mock = Mock()
        mock_client.return_value = mock
        mock.__enter__ = client
        mock.__exit__ = Mock(return_value=None)
        with patch("bibigrid.core.utility.handler.sshHandler.is_active") as mock_active:
            sshHandler.execute_ssh(42, 32, 22, [12], None)
            mock_client.assert_called_with()
            mock_active.assert_called_with(client=client(), floating_ip_address=42, username=22, private_key=2)
            mock_exec.assert_called_with(client(), [12])
            mock_paramiko_key.assert_called_with(32)

    @patch("bibigrid.core.utility.handler.sshHandler.execute_ssh")
    def test_ansible_preparation(self, mock_execute):
        sshHandler.ansible_preparation(1, 2, 3, [], [])
        mock_execute.assert_called_with(1, 2, 3, [] + sshHandler.ANSIBLE_SETUP, [(2, sshHandler.PRIVATE_KEY_FILE)])

    @patch("bibigrid.core.utility.handler.sshHandler.execute_ssh")
    def test_ansible_preparation_elem(self, mock_execute):
        sshHandler.ansible_preparation(1, 2, 3, [42], [42])
        mock_execute.assert_called_with(1, 2, 3, sshHandler.ANSIBLE_SETUP + [42],
                                        [42, (2, sshHandler.PRIVATE_KEY_FILE)])

    @patch("logging.warning")
    @patch("logging.info")
    def test_execute_ssh_cml_commands(self, mock_log_info, mock_log_warning):
        client = Mock()
        stdout_mock = Mock()
        stdout_mock.channel.recv_exit_status.side_effect = [0, 1]
        stdout_mock.readlines.return_value = 49
        client.exec_command.return_value = (0, stdout_mock, 2)
        commands = [42, 21]
        sshHandler.execute_ssh_cml_commands(client, commands)
        self.assertEqual([call('42:0')], mock_log_info.call_args_list)
        self.assertEqual([call('21:1|49')], mock_log_warning.call_args_list)

"""
Module to test ssh_handler
"""
import socket
from unittest import TestCase
from unittest.mock import mock_open, Mock, MagicMock, patch, call

from paramiko.ssh_exception import NoValidConnectionsError

from bibigrid.core import startup
from bibigrid.core.utility.handler import ssh_handler
from bibigrid.models.exceptions import ExecutionException, ConnectionException


class TestSshHandler(TestCase):
    """
    Class to test ssh_handler
    @todo: Test Gateway
    """

    def test_get_add_ssh_public_key_commands_none(self):
        ssh_public_key_files = []
        self.assertEqual([], ssh_handler.get_add_ssh_public_key_commands(ssh_public_key_files))

    def test_get_add_ssh_public_key_commands_line(self):
        ssh_public_key_files = [42]
        line = "42"
        expected = [(f"echo {line} >> .ssh/authorized_keys", f"Add SSH Key {line}.")]
        with patch("builtins.open", mock_open(read_data=line)) as mock_file:
            self.assertEqual(expected, ssh_handler.get_add_ssh_public_key_commands(ssh_public_key_files))
        mock_file.assert_called_with(42, mode='r', encoding='UTF-8')

    def test_copy_to_server_file(self):
        sftp = Mock()
        sftp.put = MagicMock(return_value=True)
        with patch("os.path.isfile") as mock_isfile:
            mock_isfile.return_value = True
            ssh_handler.copy_to_server(sftp, "Jim", "Joe", startup.LOG)
        sftp.put.assert_called_with("Jim", "Joe")

    @patch("os.listdir")
    def test_copy_to_server_folder(self, mock_listdir):
        sftp = Mock()
        sftp.mkdir = MagicMock()
        mock_listdir.return_value = []
        with patch("os.path.isfile") as mock_isfile:
            mock_isfile.return_value = False
            ssh_handler.copy_to_server(sftp, "Jim", "Joe", startup.LOG)
        mock_listdir.assert_called_with("Jim")
        sftp.mkdir.assert_called_with("Joe")

    @patch("logging.info")
    def test_is_active(self, mock_log):
        client = Mock()
        client.connect = MagicMock(return_value=True)
        self.assertFalse(ssh_handler.is_active(client, 42, 32, 22, startup.LOG, {}, timeout=5))
        mock_log.assert_not_called()

    def test_is_active_on_second_attempt(self):
        client = Mock()
        client.connect = MagicMock(side_effect=[NoValidConnectionsError({('127.0.0.1', 22): socket.error}), True])
        self.assertFalse(ssh_handler.is_active(client, 42, 32, 22, startup.LOG, {}, timeout=5))

    def test_is_active_exception(self):
        client = Mock()
        client.connect = MagicMock(side_effect=NoValidConnectionsError({('127.0.0.1', 22): socket.error}))
        with self.assertRaises(ConnectionException):
            ssh_handler.is_active(client, 42, 32, 22, startup.LOG, {}, timeout=0)
        client.connect.assert_called_with(hostname=42, username=22, pkey=32, timeout=7, auth_timeout=5, port=22)

    @patch("bibigrid.core.utility.handler.ssh_handler.execute_ssh_cml_commands")
    @patch("paramiko.ECDSAKey.from_private_key_file")
    @patch("paramiko.SSHClient")
    def test_execute_ssh(self, mock_client, mock_paramiko_key, mock_exec):
        mock_paramiko_key.return_value = 2
        client = Mock()
        mock = Mock()
        mock_client.return_value = mock
        mock.__enter__ = client
        mock.__exit__ = Mock(return_value=None)
        with patch("bibigrid.core.utility.handler.ssh_handler.is_active") as mock_active:
            ssh_handler.execute_ssh(42, 32, 22, startup.LOG, {}, [12])
            mock_client.assert_called_with()
            mock_active.assert_called_with(client=client(), floating_ip_address=42, username=22, private_key=2,
                                           log=startup.LOG, gateway={})
            mock_exec.assert_called_with(client=client(), commands=[12], log=startup.LOG)
            mock_paramiko_key.assert_called_with(32)

    @patch("bibigrid.core.utility.handler.ssh_handler.execute_ssh")
    def test_ansible_preparation(self, mock_execute):
        ssh_handler.ansible_preparation(1, 2, 3, startup.LOG, {}, [], [])
        mock_execute.assert_called_with(1, 2, 3, startup.LOG, {}, ssh_handler.ANSIBLE_SETUP,
                                        [(2, ssh_handler.PRIVATE_KEY_FILE)])

    @patch("bibigrid.core.utility.handler.ssh_handler.execute_ssh")
    def test_ansible_preparation_elem(self, mock_execute):
        ssh_handler.ansible_preparation(1, 2, 3, startup.LOG, {}, [42], [42])
        mock_execute.assert_called_with(1, 2, 3, startup.LOG, {}, ssh_handler.ANSIBLE_SETUP + [42],
                                        [42, (2, ssh_handler.PRIVATE_KEY_FILE)])

    def test_execute_ssh_cml_commands(self):
        client = Mock()
        stdout_mock = Mock()
        stdout_mock.channel.recv_exit_status.side_effect = [0, 0]
        stdout_mock.readline.side_effect = ["First Line", "", "First Line", ""]
        client.exec_command.return_value = (0, stdout_mock, 2)
        commands = [(42, 0), (21, 1)]
        ssh_handler.execute_ssh_cml_commands(client, commands, startup.LOG)

        stdout_mock.channel.recv_exit_status.assert_called()
        stdout_mock.channel.recv_exit_status.call_count = 2
        stdout_mock.readline.assert_called()
        assert stdout_mock.readline.call_count == 4
        client.exec_command.assert_has_calls([call(42), call(21)])
        assert client.exec_command.call_count == 2

    def test_execute_ssh_cml_commands_execution_exception(self):
        client = Mock()
        stdout_mock = Mock()
        stdout_mock.channel.recv_exit_status.side_effect = [0, 1]
        stdout_mock.readline.side_effect = ["First Line", "", "First Line", ""]
        client.exec_command.return_value = (0, stdout_mock, 2)
        commands = [(42, 0), (21, 1)]
        with self.assertRaises(ExecutionException):
            ssh_handler.execute_ssh_cml_commands(client, commands, startup.LOG)
        stdout_mock.channel.recv_exit_status.assert_called()
        stdout_mock.readline.assert_called()
        client.exec_command.assert_called_with(21)

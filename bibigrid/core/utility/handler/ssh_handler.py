"""
This module handles ssh and sftp connections to master and vpngtw. It also holds general execution routines used to
setup the Cluster.
"""
import os
import socket
import time

import paramiko
import yaml
import sympy

from bibigrid.core.utility import ansible_commands as aC
from bibigrid.models.exceptions import ConnectionException, ExecutionException

PRIVATE_KEY_FILE = ".ssh/id_ecdsa"  # to name bibigrid-temp keys identically on remote
ANSIBLE_SETUP = [aC.NO_UPDATE, aC.UPDATE, aC.PYTHON3_PIP, aC.ANSIBLE_PASSLIB,
                 (f"chmod 600 {PRIVATE_KEY_FILE}", "Adjust private key permissions."), aC.PLAYBOOK_HOME,
                 aC.PLAYBOOK_HOME_RIGHTS, aC.ADD_PLAYBOOK_TO_LINUX_HOME]
# ANSIBLE_START = [aC.WAIT_READY, aC.UPDATE, aC.MV_ANSIBLE_CONFIG, aC.EXECUTE]  # another UPDATE seems to not necessary.
ANSIBLE_START = [aC.WAIT_READY, aC.MV_ANSIBLE_CONFIG, aC.EXECUTE]
VPN_SETUP = [("echo Example", "Echos an Example")]


def get_ac_command(providers, name):
    """
    Get command to write application credentials to remote (
    @param providers: providers
    @param name: how the application credential shall be called
    @return: command to execute on remote to create application credential
    """
    ac_clouds_yaml = {"clouds": {}}
    for provider in providers:
        cloud_specification = provider.cloud_specification
        auth = cloud_specification["auth"]
        if auth.get("application_credential_id") and auth.get("application_credential_secret"):
            wanted_keys = ["auth", "region_name", "interface", "identity_api_version", "auth_type"]
            ac_cloud_specification = {wanted_key: cloud_specification[wanted_key] for wanted_key in wanted_keys if
                                      wanted_key in cloud_specification}
        else:
            wanted_keys = ["region_name", "interface", "identity_api_version"]
            ac = provider.create_application_credential(name=name)  # pylint: disable=invalid-name
            ac_dict = {"application_credential_id": ac["id"], "application_credential_secret": ac["secret"],
                       "auth_type": "v3applicationcredential", "auth_url": auth["auth_url"]}
            ac_cloud_specification = {wanted_key: cloud_specification[wanted_key] for wanted_key in wanted_keys if
                                      wanted_key in cloud_specification}
            ac_cloud_specification.update(ac_dict)
        ac_clouds_yaml["clouds"][cloud_specification["identifier"]] = ac_cloud_specification
    return (f"echo '{yaml.safe_dump(ac_clouds_yaml)}' | sudo install -D /dev/stdin /etc/openstack/clouds.yaml",
            "Copy application credentials.")


def get_add_ssh_public_key_commands(ssh_public_key_files):
    """
    Builds and returns the necessary commands to add given public keys to remote for additional access.
    :param ssh_public_key_files: public keys to add
    :return: list of public key add commands
    """
    commands = []
    if ssh_public_key_files:
        for ssh_public_key_file in ssh_public_key_files:
            with open(ssh_public_key_file, mode="r", encoding="UTF-8") as ssh_public_key:
                commands.append((f"echo {ssh_public_key.readline().strip()} >> .ssh/authorized_keys",
                                 f"Add SSH Key {ssh_public_key_file}."))
    return commands


def copy_to_server(sftp, local_path, remote_path, log):
    """
    Recursively copies files and folders to server.
    If a folder is given as local_path, the structure within will be kept.
    :param sftp: sftp connection
    :param local_path: file or folder locally
    :param remote_path: file or folder locally
    :param log:
    :return:
    """
    log.debug("Copy %s to %s...", local_path, remote_path)
    if os.path.isfile(local_path):
        sftp.put(local_path, remote_path)
    else:
        try:
            sftp.mkdir(remote_path)
        except OSError:
            pass
        for filename in os.listdir(local_path):
            copy_to_server(sftp, os.path.join(local_path, filename), os.path.join(remote_path, filename), log)


def is_active(client, floating_ip_address, private_key, username, log, gateway, timeout=5):
    """
    Checks if connection is possible and therefore if server is active.
    Raises paramiko.ssh_exception.NoValidConnectionsError if timeout is reached
    :param client: created client
    :param floating_ip_address: ip to connect to
    :param private_key: SSH-private_key
    :param username: SSH-username
    :param log:
    :param timeout: how long to wait between ping
    :param gateway: if node should be reached over a gateway port is set to 30000 + subnet * 256 + host
    (waiting grows quadratically till 2**timeout before accepting failure)
    """
    attempts = 0
    establishing_connection = True
    while establishing_connection:
        try:
            port = 22
            if gateway:
                log.info(f"Using SSH Gateway {gateway.get('ip')}")
                octets = {f'oct{enum+1}': int(elem) for enum, elem in enumerate(floating_ip_address.split("."))}
                port = sympy.sympify(gateway["portFunction"]).subs(dict(octets))
            client.connect(hostname=gateway.get("ip") or floating_ip_address, username=username,
                           pkey=private_key, timeout=7, auth_timeout=5, port=int(port))
            establishing_connection = False
            log.info(f"Successfully connected to {floating_ip_address}")
        except paramiko.ssh_exception.NoValidConnectionsError as exc:
            log.info(f"Attempting to connect to {floating_ip_address}... This might take a while", )
            if attempts < timeout:
                time.sleep(2 ** attempts)
                attempts += 1
            else:
                log.error(f"Attempt to connect to {floating_ip_address} failed.")
                raise ConnectionException(exc) from exc
        except socket.timeout as exc:
            log.warning("Socket timeout exception occurred. Try again ...")
            if attempts < timeout:
                attempts += 1
            else:
                log.error(f"Attempt to connect to {floating_ip_address} failed, due to a socket timeout.")
                raise ConnectionException(exc) from exc
        except TimeoutError as exc:  # pylint: disable=duplicate-except
            log.error("The attempt to connect to %s failed. Possible known reasons:"
                      "\n\t-Your network's security group doesn't allow SSH.", floating_ip_address)
            raise ConnectionException(exc) from exc


def line_buffered(f):
    """
    https://stackoverflow.com/questions/25260088/paramiko-with-continuous-stdout
    temporary hangs?
    :param f:
    :return:
    """
    line_buf = b""
    while not f.channel.exit_status_ready():

        line_buf += f.read(1024)
        if line_buf.endswith(b'\n'):
            yield line_buf
            line_buf = b''


def execute_ssh_cml_commands(client, commands, log):
    """
    Executes commands and logs exit_status accordingly.
    :param client: Client with connection to remote
    :param commands: Commands to execute on remote
    :param log:
    """
    for command in commands:
        ssh_stdin, ssh_stdout, ssh_stderr = client.exec_command(command[0])  # pylint: disable=unused-variable
        ssh_stdout.channel.set_combine_stderr(True)
        log.info(f"REMOTE: {command[1]}")

        while True:
            line = ssh_stdout.readline()
            if len(line) == 0:
                break
            if "[BIBIGRID]" in line:
                log.info(f"REMOTE: {line.strip()}")
            else:
                log.debug(f"REMOTE: {line.strip()}")

        # get exit status
        exit_status = ssh_stdout.channel.recv_exit_status()
        # close handler
        ssh_stdout.close()

        if exit_status:
            msg = f"{command[1]} ... Exit status: {exit_status}"
            log.warning(msg)
            raise ExecutionException(msg)


def ansible_preparation(floating_ip, private_key, username, log, gateway, commands=None, filepaths=None):
    """
    Installs python and pip. Then installs ansible over pip.
    Copies private key to instance so cluster-nodes are reachable and sets permission as necessary.
    Copies additional files and executes additional commands if given.
    The playbook is copied later, because it needs all servers setup and is not time intensive.
    See: create.update_playbooks
    :param floating_ip: public ip of server to ansible-prepare
    :param private_key: generated private key of all cluster-server
    :param username: username of all server
    :param log:
    :param commands: additional commands to execute
    :param filepaths: additional files to copy: (localpath, remotepath)
    :param gateway
    """
    if filepaths is None:
        filepaths = []
    if commands is None:
        commands = []
    log.info("Ansible preparation...")
    commands = ANSIBLE_SETUP + commands
    filepaths.append((private_key, PRIVATE_KEY_FILE))
    execute_ssh(floating_ip, private_key, username, log, gateway, commands, filepaths)


def execute_ssh(floating_ip, private_key, username, log, gateway, commands=None, filepaths=None):
    """
    Executes commands on remote and copies files given in filepaths
    :param floating_ip: public ip of remote
    :param private_key: key of remote
    :param username: username of remote
    :param commands: commands
    :param log:
    :param filepaths: filepaths (localpath, remotepath)
    :param gateway: gateway if used
    """
    if commands is None:
        commands = []
    paramiko_key = paramiko.ECDSAKey.from_private_key_file(private_key)
    with paramiko.SSHClient() as client:
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        try:
            is_active(client=client, floating_ip_address=floating_ip, username=username, private_key=paramiko_key,
                      log=log, gateway=gateway)
        except ConnectionException as exc:
            log.error(f"Couldn't connect to ip {gateway or floating_ip} using private key {private_key}.")
            raise exc
        else:
            log.debug(f"Setting up {floating_ip}")
            if filepaths:
                log.debug(f"Setting up filepaths for {floating_ip}")
                sftp = client.open_sftp()
                for local_path, remote_path in filepaths:
                    copy_to_server(sftp=sftp, local_path=local_path, remote_path=remote_path, log=log)
                log.debug("SFTP: Files %s copied.", filepaths)
            if commands:
                log.debug(f"Setting up commands for {floating_ip}")
                execute_ssh_cml_commands(client=client, commands=commands, log=log)

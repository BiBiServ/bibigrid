"""
This module handles ssh and sftp connections to master and vpngtw. It also holds general execution routines used to
set up the Cluster.
"""
import os
import socket
import time

import paramiko
import socks
import sympy
import yaml

from bibigrid.core.utility import ansible_commands as a_c
from bibigrid.core.utility import yaml_dumper
from bibigrid.core.utility.paths import ansible_resources_path as a_rp
from bibigrid.core.utility.paths.basic_path import RESOURCES_PATH, CONFIG_FOLDER
from bibigrid.models.exceptions import ConnectionException, ExecutionException

PRIVATE_KEY_FILE = ".ssh/id_ecdsa"  # to name bibigrid-temp keys identically on remote
ANSIBLE_SETUP = [a_c.NO_UPDATE, a_c.WAIT_READY, a_c.UPDATE, a_c.PYTHON3_PIP, a_c.VENV_SETUP, a_c.ANSIBLE_PASSLIB,
                 a_c.ANSIBLE_GALAXY, (f"chmod 600 {PRIVATE_KEY_FILE}", "Adjust private key permissions."),
                 a_c.PLAYBOOK_HOME, a_c.PLAYBOOK_HOME_RIGHTS, a_c.ADD_PLAYBOOK_TO_LINUX_HOME]
VPN_SETUP = [("echo Example", "Echos an Example")]


def ansible_start(ansible_run_node_list):
    return [a_c.WAIT_READY, a_c.MV_ANSIBLE_CONFIG,
            (f"/opt/bibigrid-venv/bin/ansible-playbook {os.path.join(a_rp.PLAYBOOK_PATH_REMOTE, a_rp.SITE_YAML)} -i "
             f"{os.path.join(a_rp.PLAYBOOK_PATH_REMOTE, a_rp.ANSIBLE_HOSTS)} -l {ansible_run_node_list}",
             "Execute ansible playbook. Be patient.")]


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


def get_add_ssh_public_key_commands(ssh_public_key_files=None, ssh_public_keys=None):
    """
    Builds and returns the necessary commands to add given public keys to remote for additional access.
    @param ssh_public_keys: public keys to add
    @param ssh_public_key_files: public keys to add from file
    @return: list of public key add commands
    """
    commands = []
    if ssh_public_key_files:
        for ssh_public_key_file in ssh_public_key_files:
            with open(ssh_public_key_file, mode="r", encoding="UTF-8") as ssh_public_key_stream:
                commands.append((f"echo {ssh_public_key_stream.readline().strip()} >> .ssh/authorized_keys",
                                 f"Add SSH Key {ssh_public_key_file}."))
    if ssh_public_keys:
        for i, ssh_public_key in enumerate(ssh_public_keys):
            commands.append((f"echo {ssh_public_key} >> .ssh/authorized_keys",
                             f"Add SSH Key {i}."))
    return commands


def copy_to_server(sftp, local_path, remote_path, log):
    """
    Recursively copies files and folders to server.
    If a folder is given as local_path, the structure within will be kept.
    @param sftp: sftp connection
    @param local_path: file or folder locally
    @param remote_path: file or folder locally
    @param log:
    @return:
    """
    log.debug("Copy %s to %s...", local_path, remote_path)
    local_path = os.path.normpath(local_path)
    if not local_path.startswith(os.path.abspath(RESOURCES_PATH)) and not local_path.startswith(
            os.path.abspath(CONFIG_FOLDER)):
        raise ValueError(f"Invalid local path: Only paths in {RESOURCES_PATH} or {CONFIG_FOLDER} are allowed.")
    if os.path.isfile(local_path):
        sftp.put(local_path, remote_path)
    else:
        try:
            sftp.mkdir(remote_path)
        except OSError:
            pass
        for filename in os.listdir(local_path):
            copy_to_server(sftp, os.path.join(local_path, filename), os.path.join(remote_path, filename), log)


def is_active(client, paramiko_key, ssh_data, log):
    """
    Checks if connection is possible and therefore if server is active.
    Raises paramiko.ssh_exception.NoValidConnectionsError if timeout is reached
    @param client: created client
    @param paramiko_key: SSH-private_key
    @param log:
    @param ssh_data: dict containing among other things gateway, floating_ip, username
    (waiting grows quadratically till 2**timeout before accepting failure)
    """
    attempts = 0
    establishing_connection = True
    log.info(f"Attempting to connect to {ssh_data['floating_ip']}... This might take a while")
    port = 22

    sock = None

    if ssh_data.get('gateway'):
        log.info(f"Using SSH Gateway {ssh_data['gateway'].get('ip')}")
        octets = {f'oct{enum + 1}': int(elem) for enum, elem in enumerate(ssh_data['floating_ip'].split("."))}
        port = int(sympy.sympify(ssh_data['gateway']["portFunction"]).subs(dict(octets)))
        log.info(f"Port {port} will be used (see {ssh_data['gateway']['portFunction']} and octets {octets}).")

    while establishing_connection:
        try:
            log.info(f"Attempt {attempts}/{ssh_data['timeout']}. Connecting to {ssh_data['floating_ip']}")
            if ssh_data.get('sock5_proxy'):
                log.debug("Trying to connect socket.")
                sock = socks.socksocket()
                sock.set_proxy(socks.SOCKS5, addr=ssh_data["sock5_proxy"]["address"],
                               port=ssh_data["sock5_proxy"]["port"])
                sock.connect((ssh_data['gateway'].get("ip") or ssh_data['floating_ip'], 22))
            client.connect(hostname=ssh_data['gateway'].get("ip") or ssh_data['floating_ip'],
                           username=ssh_data['username'], pkey=paramiko_key, timeout=7,
                           auth_timeout=ssh_data['timeout'], port=port, look_for_keys=False, allow_agent=False,
                           sock=sock)
            establishing_connection = False
            log.info(f"Successfully connected to {ssh_data['floating_ip']}.")
        except (paramiko.ssh_exception.NoValidConnectionsError, socket.timeout, socket.error) as exc:
            if attempts < ssh_data['timeout']:
                sleep_time = 2 ** (attempts + 2)
                time.sleep(sleep_time)
                log.info(f"Waiting {sleep_time} before attempting to reconnect.")
                attempts += 1
            else:
                log.error(f"Attempt to connect to {ssh_data['floating_ip']} failed.")
                raise ConnectionException(exc) from exc
        except socket.timeout as exc:
            log.warning("Socket timeout exception occurred. Try again ...")
            if attempts < ssh_data['timeout']:
                attempts += 1
            else:
                log.error(f"Attempt to connect to {ssh_data['floating_ip']} failed, due to a socket timeout.")
                raise ConnectionException(exc) from exc
        except TimeoutError as exc:  # pylint: disable=duplicate-except
            log.error("The attempt to connect to %s failed. Possible known reasons:"
                      "\n\t-Your network's security group doesn't allow SSH.", ssh_data['floating_ip'])
            raise ConnectionException(exc) from exc


def line_buffered(f):
    """
    https://stackoverflow.com/questions/25260088/paramiko-with-continuous-stdout
    temporary hangs?
    @param f:
    @return:
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
    Do not log commands as they contain cloud credentials.
    @param client: Client with connection to remote
    @param commands: Commands to execute on remote
    @param log:
    """
    for command in commands:
        _, ssh_stdout, _ = client.exec_command(command[0])
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


def execute_ssh(ssh_data, log):
    """
    Executes commands on remote and copies files given in filepaths
    Do not log commands as they contain cloud credentials.
    @param ssh_data: Dict containing floating_ip, private_key, username, commands, filepaths, gateway, timeout
    @param log:
    """
    log.debug("Running execute_ssh")
    for key in ssh_data:
        if key not in ["commands", "filepaths"]:
            log.debug(f"{key}: {ssh_data[key]}")
    if ssh_data.get("filepaths") is None:
        ssh_data["filepaths"] = []
    if ssh_data.get("commands") is None:
        ssh_data["commands"] = []
    if ssh_data.get("write_remote") is None:
        ssh_data["write_remote"] = []
    paramiko_key = paramiko.ECDSAKey.from_private_key_file(ssh_data["private_key"])
    with paramiko.SSHClient() as client:
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        try:
            is_active(client=client, paramiko_key=paramiko_key, ssh_data=ssh_data, log=log)
        except ConnectionException as exc:
            log.error(f"Couldn't connect to ip {ssh_data['gateway'] or ssh_data['floating_ip']} using private key "
                      f"{ssh_data['private_key']}.")
            raise exc

        log.debug(f"Setting up {ssh_data['floating_ip']}")
        if ssh_data['filepaths'] or ssh_data["write_remote"]:
            sftp = client.open_sftp()
            if ssh_data['filepaths']:
                log.debug(f"Setting up filepaths for {ssh_data['floating_ip']}")
                for local_path, remote_path in ssh_data['filepaths']:
                    copy_to_server(sftp=sftp, local_path=local_path, remote_path=remote_path, log=log)
                log.debug("SFTP: Files %s copied.", ssh_data['filepaths'])
            if ssh_data["write_remote"]:
                log.debug(f"Writing files for {ssh_data['floating_ip']}")
                for data, remote_path in ssh_data['write_remote']:
                    write_to_remote_file(sftp=sftp, data=data, remote_path=remote_path, log=log)
                log.debug("SFTP: Files %s created.", ssh_data['filepaths'])
        if ssh_data["floating_ip"]:
            log.debug(f"Setting up commands for {ssh_data['floating_ip']}")
            execute_ssh_cml_commands(client=client, commands=ssh_data["commands"], log=log)


def write_to_remote_file(sftp, remote_path, data, log):
    """
    Writes data to a file on the server.

    @param sftp: sftp connection
    @param remote_path: path to the file on the remote server
    @param data: data to be written to the file
    @param log: logger for logging activities
    """
    log.debug("Writing data to %s...", remote_path)

    try:
        with sftp.file(remote_path, 'w') as remote_file:
            remote_file.write(yaml.dump(data=data, Dumper=yaml_dumper.NoAliasSafeDumper))
        log.debug("Successfully wrote data to %s", remote_path)
    except Exception as e:
        log.error("Failed to write data to %s: %s", remote_path, str(e))
        raise

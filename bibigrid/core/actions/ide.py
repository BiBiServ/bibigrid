"""
This module contains methods to establish port forwarding in order to access an ide (theia).
"""

import random
import re
import signal
import subprocess
import sys
import time
import webbrowser

import sshtunnel
import sympy

from bibigrid.core.utility.handler import cluster_ssh_handler

DEFAULT_IDE_WORKSPACE = "${HOME}"
DEFAULT_IDE_PORT_END = 8383
REMOTE_BIND_ADDRESS = 8181
LOCAL_BIND_ADDRESS = 9191
MAX_JUMP = 100
LOCALHOST = "127.0.0.1"



def sigint_handler(caught_signal, frame):  # pylint: disable=unused-argument
    """
    Is called when SIGINT is thrown and terminates the program
    @param caught_signal:
    @param frame:
    @return: 0
    """
    print("Exiting...")
    sys.exit(0)


signal.signal(signal.SIGINT, sigint_handler)


def is_used(ip_address):
    """
    https://stackoverflow.com/questions/62000168/how-to-check-if-ssh-tunnel-is-being-used
    :return:
    """
    ports_used = []
    with subprocess.Popen(["netstat", "-na"], stdout=subprocess.PIPE) as process:
        out = process.stdout.read()
        lines = out.decode('utf-8').split('\n')
        for line in lines:
            is_open = re.match(rf'tcp.*{ip_address}:([0-9][0-9]*).*ESTABLISHED\s*$', line)
            if is_open is not None:
                ports_used.append(is_open[1])


def ide(cluster_id, master_provider, master_configuration, log):
    """
    Creates a port forwarding from LOCAL_BIND_ADDRESS to REMOTE_BIND_ADDRESS from localhost to master of specified
    cluster
    @param cluster_id: cluster_id or ip
    @param master_provider: master's provider
    @param master_configuration: master's configuration
    @param log:
    @return:
    """
    log.info("Starting port forwarding for ide")
    master_ip, ssh_user, used_private_key = cluster_ssh_handler.get_ssh_connection_info(cluster_id, master_provider,
                                                                                        master_configuration, log)
    used_local_bind_address = LOCAL_BIND_ADDRESS
    if master_ip and ssh_user and used_private_key:
        attempts = 0
        if master_configuration.get("gateway"):
            octets = {f'oct{enum + 1}': int(elem) for enum, elem in enumerate(master_ip.split("."))}
            port = sympy.sympify(master_configuration["gateway"]["portFunction"]).subs(dict(octets))
            gateway = (master_configuration["gateway"]["ip"], int(port))
        else:
            gateway = None
        while attempts < 16:
            attempts += 1
            try:
                with sshtunnel.SSHTunnelForwarder(ssh_address_or_host=gateway or master_ip, ssh_username=ssh_user,
                                                  ssh_pkey=used_private_key,
                                                  local_bind_address=(LOCALHOST, used_local_bind_address),
                                                  remote_bind_address=(LOCALHOST, REMOTE_BIND_ADDRESS)) as server:
                    log.debug(f"Used {used_local_bind_address} as the local binding address")
                    log.log(42, "CTRL+C to close port forwarding when you are done.")
                    with server:
                        # opens in existing window if any default program exists
                        webbrowser.open(f"http://localhost:{used_local_bind_address}", new=2)
                        while True:
                            time.sleep(5)
            except sshtunnel.HandlerSSHTunnelForwarderError:
                used_local_bind_address += random.randint(1, MAX_JUMP)
                log.info("Attempt: %s. Port in use... Trying new port %s", attempts, used_local_bind_address)
    if not master_ip:
        log.warning("Cluster id %s doesn't match an existing cluster with a master.", cluster_id)
    if not ssh_user:
        log.warning("No ssh user has been specified in the first configuration.")
    if not used_private_key:
        log.warning("No matching sshPublicKeyFiles can be found in the first configuration or in .bibigrid")
    return 1

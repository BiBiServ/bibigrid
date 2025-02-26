"""
Integration test module that assumes that one cluster is started at a time.
For multiple clusters the .bibigrid.mem structure needs to be updated TODO
This is not ready yet.
"""

import os
import subprocess

import yaml

# Define common configuration paths
CONFIG_DIR = os.path.expanduser("~/.config/bibigrid")
MEM_FILE = os.path.join(CONFIG_DIR, ".bibigrid.mem")
KEYFILE_PATH_TEMPLATE = os.path.join(CONFIG_DIR, "keys", "tempKey_bibi-{cluster_id}")
BIBIGRID_SCRIPT = os.path.abspath("../../bibigrid.sh")


def start_cluster():
    """Start the cluster by calling bibigrid.sh."""
    print("Starting the cluster...")
    result = subprocess.run([BIBIGRID_SCRIPT, "-c", "-vv", "-i", "bibigrid.yaml"], capture_output=True, text=True,
                            check=False)
    if result.returncode == 0:
        print("Cluster started successfully.")
        # print(result.stdout)
    else:
        print("Failed to start the cluster.")
        # print(result.stderr)
        raise Exception("Cluster start failed")


def read_cluster_info():
    """Read last cluster information from bibigrid.mem file."""
    with open(MEM_FILE, "r", encoding="UTF-8") as f:
        cluster_data = yaml.safe_load(f)
    return cluster_data["cluster_id"], cluster_data["floating_ip"], cluster_data["ssh_user"]


def build_keyfile_path(cluster_id):
    """Construct the keyfile path using cluster ID."""
    return KEYFILE_PATH_TEMPLATE.format(cluster_id=cluster_id)


def ssh_command(master_ip, keyfile, command, ssh_user):
    """Execute a command on the master node via SSH."""
    ssh_cmd = [
        "ssh",
        "-i", keyfile,
        "-o", "StrictHostKeyChecking=no",
        f"{ssh_user}@{master_ip}",
        command
    ]
    return subprocess.run(ssh_cmd, capture_output=True, text=True, check=False)


def terminate_cluster():
    """Terminate the cluster by calling bibigrid.sh."""
    print("Terminating the cluster...")
    result = subprocess.run([BIBIGRID_SCRIPT, "-i", "bibigrid.yaml", "-t", "-vv"], capture_output=True, text=True,
                            check=False)
    if result.returncode == 0:
        print("Cluster terminated successfully.")
        print(result.stdout)
    else:
        print("Failed to terminate the cluster.")
        print(result.stderr)
        raise Exception("Cluster termination failed")


def main():
    # Step 0: Create the cluster
    start_cluster()
    # Step 1: Read cluster info
    cluster_id, master_ip, ssh_user = read_cluster_info()
    print(f"Cluster ID: {cluster_id}, Master IP: {master_ip}")

    # Step 2: Build keyfile path
    keyfile = build_keyfile_path(cluster_id)
    print(f"Using keyfile: {keyfile}")

    # Step 3: Check worker nodes by running srun from master node
    check_command = "srun -N2 hostname>"
    print(f"Running on master: {check_command}")

    # Run the command on the master instance
    result = ssh_command(master_ip, keyfile, check_command, ssh_user)

    if result.returncode == 0:
        print("Worker nodes are up and responding:")
        print(result.stdout)
    else:
        print("Failed to run command on worker nodes.")
        print(result.stderr)

    # Step N: Terminate the cluster
    terminate_cluster()


if __name__ == "__main__":
    main()

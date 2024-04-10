"""
Module containing a bunch of useful commands to be used by sshHandler.py for cluster setup
"""

import os
import bibigrid.core.utility.paths.ansible_resources_path as a_rp

# TO_LOG = "| sudo tee -a /var/log/ansible.log"
# AIY = "apt-get -y install"
# SAU = "sudo apt-get update"
# NO_KEY_CHECK = "export ANSIBLE_HOST_KEY_CHECKING=False"
NO_UPDATE = ("""sudo sed -i 's/APT::Periodic::Unattended-Upgrade "1";/APT::Periodic::Unattended-Upgrade "0";/g' """
             """/etc/apt/apt.conf.d/20auto-upgrades""", "Disable apt auto update.")
# Setup (Python for everyone)
# UPDATE = f"sudo {AU} {TO_LOG}"
# PIP = f"sudo pip3 install --upgrade pip {TO_LOG}"
# SETUPTOOLS = "sudo pip3 install setuptools"
# LOG = "export ANSIBLE_LOG_PATH=~/ansible.log"
WAIT_READY = ('while sudo lsof /var/lib/dpkg/lock 2> null; do echo "/var/lib/dpkg/lock locked - wait for 10 seconds"; '
              'sleep 10; done', "Wait for dpkg lock removed.")
# SLEEP_10 = "sleep 10s"
# RANDOM = "sudo DEBIAN_FRONTEND=noninteractive apt-get --yes  install apt-transport-https ca-certificates " \
#         "software-properties-common python3 python3-pip libffi-dev libssl-dev"
# PYTHON_WORKERS = f'ansible workers -i "{aRP.HOSTS_CONFIG_FILE_REMOTE}" --become -m raw -a "{SAU} && {AIY} python3' \
#                 f'"'

# Test Ansible
# PING = (f'ansible -i "{aRP.HOSTS_CONFIG_FILE_REMOTE}" all -m ping',"Ping all hosts using ansible.")
# OK = ('if [ $? -eq 0 ]; then echo "Ansible configuration seems to work properly."; '
#     'else echo"Ansible hosts not reachable. There seems to be a misconfiguration."; fi',"Check for ")

# Run ansible-galaxy to install ansible-galaxy roles from galaxy, git or url (.tar.gz)
# GALAXY = f"ansible-galaxy install --roles-path {aRP.ADDITIONAL_ROLES_ROOT_PATH_REMOTE} -r {aRP.REQUIREMENTS_YML}"

# Extract ansible roles from files (.tar.gz, .tgz)
# EXTRACT = f"for f in $(find /tmp/roles -type f -regex '.*\\.t\\(ar\\.\\)?gz'); " \
#          f"do tar -xzf $f -C {aRP.ADDITIONAL_ROLES_ROOT_PATH_REMOTE}; done"

# Fix line endings for all text based ansible file to ensure windows files being used correctly
# GET_ASCII_FILES = "files=$(for f in $( find ~/playbook -type f); do  file ${f} | grep ASCII | cut -f 1 -d ':'; done;)"
# REPLACE_ENDINGS = "for file in ${file}; do sed -i 's/\\r$//' \"${file}\"; done"

# Utility
ADD_PLAYBOOK_TO_LINUX_HOME = ("ln -s /opt/playbook ~/playbook", "Link /opt/playbook to ~/playbook.")

# Execute
PLAYBOOK_HOME = ("sudo mkdir -p /opt/playbook", "Create playbook home.")
PLAYBOOK_HOME_RIGHTS = ("uid=$(id -u); gid=$(id -g); sudo chown ${uid}:${gid} /opt/playbook",
                        "Adjust playbook home permission.")
MV_ANSIBLE_CONFIG = (
    "sudo install -D /opt/playbook/ansible.cfg /etc/ansible/ansible.cfg", "Move ansible configuration.")
EXECUTE = (f"ansible-playbook {os.path.join(a_rp.PLAYBOOK_PATH_REMOTE, a_rp.SITE_YML)} -i "
           f"{os.path.join(a_rp.PLAYBOOK_PATH_REMOTE, a_rp.ANSIBLE_HOSTS)} -l {{}}",
           "Execute ansible playbook. Be patient.")

# ansible setup
UPDATE = ("sudo apt-get update", "Update apt repository lists.")
PYTHON3_PIP = "sudo apt-get install -y python3-pip", "Install python3 pip using apt."
ANSIBLE_PASSLIB = ("sudo pip install ansible==6.6 passlib", "Install Ansible and Passlib using pip.")

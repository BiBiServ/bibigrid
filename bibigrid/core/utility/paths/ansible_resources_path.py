"""
Paths that are used by Ansible. Especially playbook, vars files and Co.
"""

import os

import bibigrid.core.utility.paths.basic_path as b_p

# UNIVERSAL
ANSIBLE_HOSTS: str = "ansible_hosts"
COMMON_YML: str = "common.yml"
SITE_YML: str = "site.yml"
REQUIREMENTS_YML: str = "requirements.yml"
UPLOAD_PATH: str = "/tmp/roles/"
VARS_PATH: str = "vars/"
GROUP_VARS_PATH: str = "group_vars/"
HOST_VARS_PATH: str = "host_vars/"
ROLES_PATH: str = "roles/"
LOGIN_YML: str = VARS_PATH + "login.yml"
CONFIG_YML: str = VARS_PATH + "common_configuration.yml"
HOSTS_YML: str = VARS_PATH + "hosts.yml"
WORKER_SPECIFICATION_YML: str = VARS_PATH + "worker_specification.yml"
ADDITIONAL_ROLES_PATH: str = ROLES_PATH + "additional/"
DEFAULT_IP_FILE = VARS_PATH + "{{ ansible_default_ipv4.address }}.yml"
ANSIBLE_CFG = "ansible.cfg"

# LOCAL
PLAYBOOK = "playbook/"
PLAYBOOK_PATH: str = os.path.join(b_p.RESOURCES_PATH, PLAYBOOK)
ANSIBLE_CFG_PATH = os.path.join(PLAYBOOK_PATH, ANSIBLE_CFG)
HOSTS_FILE = os.path.join(PLAYBOOK_PATH, HOSTS_YML)
HOSTS_CONFIG_FILE: str = os.path.join(PLAYBOOK_PATH, ANSIBLE_HOSTS)
CONFIG_ROOT_PATH: str = os.path.join(PLAYBOOK_PATH, VARS_PATH)
ROLES_ROOT_PATH: str = os.path.join(PLAYBOOK_PATH, ROLES_PATH)
COMMONS_LOGIN_FILE: str = os.path.join(PLAYBOOK_PATH, LOGIN_YML)
COMMONS_CONFIG_FILE: str = os.path.join(PLAYBOOK_PATH, CONFIG_YML)
SITE_CONFIG_FILE: str = os.path.join(PLAYBOOK_PATH, SITE_YML)
WORKER_SPECIFICATION_FILE: str = os.path.join(PLAYBOOK_PATH, WORKER_SPECIFICATION_YML)
ADDITIONAL_ROLES_ROOT_PATH: str = ROLES_ROOT_PATH + ADDITIONAL_ROLES_PATH
VARS_FOLDER = os.path.join(PLAYBOOK_PATH, VARS_PATH)
GROUP_VARS_FOLDER = os.path.join(PLAYBOOK_PATH, GROUP_VARS_PATH)
HOST_VARS_FOLDER = os.path.join(PLAYBOOK_PATH, HOST_VARS_PATH)
## DEFAULTS
ANSIBLE_CFG_DEFAULT_PATH = os.path.join(b_p.RESOURCES_PATH, "defaults", "ansible", ANSIBLE_CFG)


# REMOTE
ROOT_PATH_REMOTE = "~"
PLAYBOOK_PATH_REMOTE: str = os.path.join("/opt/", PLAYBOOK)
# PLAYBOOK_PATH_REMOTE: str = os.path.join(ROOT_PATH_REMOTE, PLAYBOOK)
# PLAYBOOK_PATH_REMOTE_SLURM: str = os.path.join("/opt/slurm/", PLAYBOOK)
HOSTS_CONFIG_FILE_REMOTE: str = PLAYBOOK_PATH_REMOTE + ANSIBLE_HOSTS
CONFIG_ROOT_PATH_REMOTE: str = PLAYBOOK_PATH_REMOTE + VARS_PATH
ROLES_ROOT_PATH_REMOTE: str = PLAYBOOK_PATH_REMOTE + ROLES_PATH
COMMONS_LOGIN_FILE_REMOTE: str = PLAYBOOK_PATH_REMOTE + LOGIN_YML
COMMONS_CONFIG_FILE_REMOTE: str = PLAYBOOK_PATH_REMOTE + CONFIG_YML
SITE_CONFIG_FILE_REMOTE: str = PLAYBOOK_PATH_REMOTE + SITE_YML
WORKER_SPECIFICATION_FILE_REMOTE: str = PLAYBOOK_PATH_REMOTE + WORKER_SPECIFICATION_YML
ADDITIONAL_ROLES_ROOT_PATH_REMOTE: str = ROLES_ROOT_PATH + ADDITIONAL_ROLES_PATH
REQUIREMENTS_CONFIG_FILE_REMOTE: str = ADDITIONAL_ROLES_ROOT_PATH_REMOTE + REQUIREMENTS_YML

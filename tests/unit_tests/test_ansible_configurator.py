"""
Tests for ansible_configurator
"""
import os
from unittest import TestCase
from unittest.mock import MagicMock, Mock, patch, call, mock_open, ANY

import bibigrid.core.utility.paths.ansible_resources_path as aRP
from bibigrid.core import startup
from bibigrid.core.actions import version
from bibigrid.core.utility import ansible_configurator
from bibigrid.core.utility.yaml_dumper import NoAliasSafeDumper


class TestAnsibleConfigurator(TestCase):
    """
    Test ansible configurator test class
    """

    # pylint: disable=R0904
    def test_generate_site_file_yaml_empty(self):
        site_yaml = [{'become': 'yes', 'hosts': 'master',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-master']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml']},
                     {'become': 'yes', 'hosts': 'vpngtw',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-vpngtw']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml']},
                     {'become': 'yes', 'hosts': 'workers',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-worker']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml']}]
        self.assertEqual(site_yaml, ansible_configurator.generate_site_file_yaml([]))

    def test_generate_site_file_yaml_master_role(self):
        user_roles = [{'hosts': ['master'], 'roles': [{'name': 'resistance_nextflow', 'tags': ['rn']}]}]
        # vars_files = ['vars/login.yaml', 'vars/common_configuration.yaml', 'varsFile']
        site_yaml = [{'become': 'yes', 'hosts': 'master',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-master']},
                                {'role': 'resistance_nextflow', 'tags': ['rn']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml']},
                     {'become': 'yes', 'hosts': 'vpngtw',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-vpngtw']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml']},
                     {'become': 'yes', 'hosts': 'workers',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-worker']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml']}]
        self.assertEqual(site_yaml, ansible_configurator.generate_site_file_yaml(user_roles))

    def test_generate_site_file_yaml_vpngtw_role(self):
        user_roles = [{'hosts': ['vpngtw'], 'roles': [{'name': 'resistance_nextflow'}], 'varsFiles': ['vars/rn']}]
        # vars_files = ['vars/login.yaml', 'vars/common_configuration.yaml', 'varsFile']
        site_yaml = [{'become': 'yes', 'hosts': 'master',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-master']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml']},
                     {'become': 'yes', 'hosts': 'vpngtw',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-vpngtw']},
                                {'role': 'resistance_nextflow', 'tags': []}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml', 'vars/rn']},
                     {'become': 'yes', 'hosts': 'workers',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-worker']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml']}]
        self.assertEqual(site_yaml, ansible_configurator.generate_site_file_yaml(user_roles))

    def test_generate_site_file_yaml_workers_role(self):
        user_roles = [{'hosts': ['workers'], 'roles': [{'name': 'resistance_nextflow'}]}]
        # vars_files = ['vars/login.yaml', 'vars/common_configuration.yaml', 'varsFile']
        site_yaml = [{'become': 'yes', 'hosts': 'master',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-master']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml']},
                     {'become': 'yes', 'hosts': 'vpngtw',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-vpngtw']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml']},
                     {'become': 'yes', 'hosts': 'workers',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-worker']},
                                {'role': 'resistance_nextflow', 'tags': []}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml']}]
        self.assertEqual(site_yaml, ansible_configurator.generate_site_file_yaml(user_roles))

    def test_generate_site_file_yaml_all_role(self):
        user_roles = [
            {'hosts': ['master', 'vpngtw', 'workers'], 'roles': [{'name': 'resistance_nextflow', 'tags': ['rn']}],
             'varsFiles': ['vars/rn']}]
        # vars_files = ['vars/login.yaml', 'vars/common_configuration.yaml', 'varsFile']
        site_yaml = [{'become': 'yes', 'hosts': 'master',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-master']},
                                {'role': 'resistance_nextflow', 'tags': ['rn']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml', 'vars/rn']},
                     {'become': 'yes', 'hosts': 'vpngtw',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-vpngtw']},
                                {'role': 'resistance_nextflow', 'tags': ['rn']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml', 'vars/rn']},
                     {'become': 'yes', 'hosts': 'workers',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-worker']},
                                {'role': 'resistance_nextflow', 'tags': ['rn']}],
                      'vars_files': ['vars/common_configuration.yaml', 'vars/hosts.yaml', 'vars/rn']}]
        self.assertEqual(site_yaml, ansible_configurator.generate_site_file_yaml(user_roles))

    def test_generate_common_configuration_false(self):
        cidrs = "42"
        cluster_id = "21"
        default_user = "ubuntu"
        ssh_user = "test"
        configuration = [{}]
        common_configuration_yaml = {'bibigrid_version': version.__version__, 'cloud_scheduling': {'sshTimeout': 5},
                                     'cluster_cidrs': cidrs, 'cluster_id': cluster_id, 'default_user': default_user,
                                     'dns_server_list': ['8.8.8.8'], 'enable_ide': False, 'enable_nfs': False,
                                     'enable_slurm': False, 'enable_zabbix': False, 'local_dns_lookup': False,
                                     'local_fs': False, 'slurm': True,
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 1200, 'SuspendTime': 3600,
                                                                           'SuspendTimeout': 60, 'TreeWidth': 128},
                                                    'munge_key': 'TO_BE_FILLED'}, 'ssh_user': ssh_user,
                                     'use_master_as_compute': True}
        generated_common_configuration = ansible_configurator.generate_common_configuration_yaml(cidrs, configuration,
                                                                                                 cluster_id, ssh_user,
                                                                                                 default_user,
                                                                                                 startup.LOG)
        # munge key is randomly generated
        common_configuration_yaml["slurm_conf"]["munge_key"] = generated_common_configuration["slurm_conf"]["munge_key"]
        self.assertEqual(common_configuration_yaml, generated_common_configuration)

    def test_generate_common_configuration_true(self):
        cidrs = "42"
        cluster_id = "21"
        default_user = "ubuntu"
        ssh_user = "test"
        configuration = [
            {elem: "True" for elem in ["localFS", "localDNSlookup", "useMasterAsCompute", "slurm", "zabbix", "ide"]}]
        common_configuration_yaml = {'bibigrid_version': version.__version__, 'cloud_scheduling': {'sshTimeout': 5},
                                     'cluster_cidrs': cidrs, 'cluster_id': cluster_id, 'default_user': default_user,
                                     'dns_server_list': ['8.8.8.8'], 'enable_ide': 'True', 'enable_nfs': False,
                                     'enable_slurm': 'True', 'enable_zabbix': 'True',
                                     'ide_conf': {'build': False, 'ide': False, 'port_end': 8383, 'port_start': 8181,
                                                  'workspace': '${HOME}'}, 'local_dns_lookup': 'True',
                                     'local_fs': 'True', 'slurm': 'True',
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 1200, 'SuspendTime': 3600,
                                                                           'SuspendTimeout': 60, 'TreeWidth': 128},
                                                    'munge_key': 'TO_BE_FILLED'}, 'ssh_user': ssh_user,
                                     'use_master_as_compute': 'True',
                                     'zabbix_conf': {'admin_password': 'bibigrid', 'db': 'zabbix',
                                                     'db_password': 'zabbix', 'db_user': 'zabbix',
                                                     'server_name': 'bibigrid', 'timezone': 'Europe/Berlin'}}
        generated_common_configuration = ansible_configurator.generate_common_configuration_yaml(cidrs, configuration,
                                                                                                 cluster_id, ssh_user,
                                                                                                 default_user,
                                                                                                 startup.LOG)
        common_configuration_yaml["slurm_conf"]["munge_key"] = generated_common_configuration["slurm_conf"]["munge_key"]
        self.assertEqual(common_configuration_yaml, generated_common_configuration)

    def test_generate_common_configuration_nfs_shares(self):
        configuration = [{"nfs": "True", "nfsShares": ["/vil/mil"]}]
        cidrs = "42"
        cluster_id = "21"
        default_user = "ubuntu"
        ssh_user = "test"
        common_configuration_yaml = {'bibigrid_version': version.__version__, 'cloud_scheduling': {'sshTimeout': 5},
                                     'cluster_cidrs': cidrs, 'cluster_id': cluster_id, 'default_user': default_user,
                                     'dns_server_list': ['8.8.8.8'], 'enable_ide': False, 'enable_nfs': 'True',
                                     'enable_slurm': False, 'enable_zabbix': False, 'ext_nfs_mounts': [],
                                     'local_dns_lookup': False, 'local_fs': False,
                                     'nfs_shares': [{'dst': '/vil/mil', 'src': '/vil/mil'},
                                                    {'dst': '/vol/spool', 'src': '/vol/spool'}], 'slurm': True,
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 1200, 'SuspendTime': 3600,
                                                                           'SuspendTimeout': 60, 'TreeWidth': 128},
                                                    'munge_key': 'TO_BE_FILLED'}, 'ssh_user': ssh_user,
                                     'use_master_as_compute': True}
        generated_common_configuration = ansible_configurator.generate_common_configuration_yaml(cidrs, configuration,
                                                                                                 cluster_id, ssh_user,
                                                                                                 default_user,
                                                                                                 startup.LOG)
        common_configuration_yaml["slurm_conf"]["munge_key"] = generated_common_configuration["slurm_conf"]["munge_key"]
        self.assertEqual(common_configuration_yaml, generated_common_configuration)

    def test_generate_common_configuration_nfs(self):
        configuration = [{"nfs": "True"}]
        cidrs = "42"
        cluster_id = "21"
        default_user = "ubuntu"
        ssh_user = "test"
        common_configuration_yaml = {'bibigrid_version': version.__version__, 'cloud_scheduling': {'sshTimeout': 5},
                                     'cluster_cidrs': cidrs, 'cluster_id': cluster_id, 'default_user': default_user,
                                     'dns_server_list': ['8.8.8.8'], 'enable_ide': False, 'enable_nfs': 'True',
                                     'enable_slurm': False, 'enable_zabbix': False, 'ext_nfs_mounts': [],
                                     'local_dns_lookup': False, 'local_fs': False,
                                     'nfs_shares': [{'dst': '/vol/spool', 'src': '/vol/spool'}], 'slurm': True,
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 1200, 'SuspendTime': 3600,
                                                                           'SuspendTimeout': 60, 'TreeWidth': 128},
                                                    'munge_key': 'TO_BE_FILLED'}, 'ssh_user': ssh_user,
                                     'use_master_as_compute': True}
        generated_common_configuration = ansible_configurator.generate_common_configuration_yaml(cidrs, configuration,
                                                                                                 cluster_id, ssh_user,
                                                                                                 default_user,
                                                                                                 startup.LOG)
        common_configuration_yaml["slurm_conf"]["munge_key"] = generated_common_configuration["slurm_conf"]["munge_key"]
        self.assertEqual(common_configuration_yaml, generated_common_configuration)

    def test_generate_common_configuration_ext_nfs_shares(self):
        configuration = [{"nfs": "True", "extNfsShares": ["/vil/mil"]}]
        cidrs = "42"
        cluster_id = "21"
        default_user = "ubuntu"
        ssh_user = "test"
        common_configuration_yaml = {'bibigrid_version': version.__version__, 'cloud_scheduling': {'sshTimeout': 5},
                                     'cluster_cidrs': cidrs, 'cluster_id': cluster_id, 'default_user': default_user,
                                     'dns_server_list': ['8.8.8.8'], 'enable_ide': False, 'enable_nfs': 'True',
                                     'enable_slurm': False, 'enable_zabbix': False,
                                     'ext_nfs_mounts': [{'dst': '/vil/mil', 'src': '/vil/mil'}],
                                     'local_dns_lookup': False, 'local_fs': False,
                                     'nfs_shares': [{'dst': '/vol/spool', 'src': '/vol/spool'}], 'slurm': True,
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 1200, 'SuspendTime': 3600,
                                                                           'SuspendTimeout': 60, 'TreeWidth': 128},
                                                    'munge_key': 'YryJVnqgg24Ksf8zXQtbct3nuXrMSi9N'},
                                     'ssh_user': ssh_user, 'use_master_as_compute': True}
        generated_common_configuration = ansible_configurator.generate_common_configuration_yaml(cidrs, configuration,
                                                                                                 cluster_id, ssh_user,
                                                                                                 default_user,
                                                                                                 startup.LOG)
        common_configuration_yaml["slurm_conf"]["munge_key"] = generated_common_configuration["slurm_conf"]["munge_key"]
        self.assertEqual(common_configuration_yaml, generated_common_configuration)

    def test_generate_common_configuration_ide_and_wireguard(self):
        configuration = [{"ide": "Some1", "ideConf": {"key1": "Some2"}, "wireguard_peer": 21}, {"wireguard_peer": 42}]
        cidrs = "42"
        cluster_id = "21"
        default_user = "ubuntu"
        ssh_user = "test"
        common_configuration_yaml = {'bibigrid_version': '0.4.0', 'cloud_scheduling': {'sshTimeout': 5},
                                     'cluster_cidrs': '42', 'cluster_id': '21', 'default_user': 'ubuntu',
                                     'dns_server_list': ['8.8.8.8'], 'enable_ide': 'Some1', 'enable_nfs': False,
                                     'enable_slurm': False, 'enable_zabbix': False,
                                     'ide_conf': {'build': False, 'ide': False, 'key1': 'Some2', 'port_end': 8383,
                                                  'port_start': 8181, 'workspace': '${HOME}'},
                                     'local_dns_lookup': False, 'local_fs': False, 'slurm': True,
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 1200, 'SuspendTime': 3600,
                                                                           'SuspendTimeout': 60, 'TreeWidth': 128},
                                                    'munge_key': 'b4pPb7bQTHzTDtGsqo2pYkGdpa87TtPD'},
                                     'ssh_user': 'test', 'use_master_as_compute': True,
                                     'wireguard_common': {'listen_port': 51820, 'mask_bits': 24, 'peers': [21, 42]}}
        generated_common_configuration = ansible_configurator.generate_common_configuration_yaml(cidrs, configuration,
                                                                                                 cluster_id, ssh_user,
                                                                                                 default_user,
                                                                                                 startup.LOG)
        common_configuration_yaml["slurm_conf"]["munge_key"] = generated_common_configuration["slurm_conf"]["munge_key"]
        self.assertEqual(common_configuration_yaml, generated_common_configuration)

    @patch("bibigrid.core.utility.ansible_configurator.to_instance_host_dict")
    def test_generate_ansible_hosts(self, mock_instance_host_dict):
        cluster_id = "21"
        mock_instance_host_dict.side_effect = [0, 1, 2, 4, 5, {}]
        configuration = [{'masterInstance': {'type': 'mini', 'image': 'Ubuntu'},
                          'workerInstances': [{'type': 'tiny', 'image': 'Ubuntu', 'count': 2},
                                              {'type': 'default', 'image': 'Ubuntu', 'count': 1}]},
                         {'vpnInstance': {'type': 'mini', 'image': 'Ubuntu'}, 'workerInstances': [
                             {'type': 'tiny', 'image': 'Ubuntu', 'count': 2, 'features': ['holdsinformation']},
                             {'type': 'small', 'image': 'Ubuntu', 'count': 2}], 'floating_ip': "42"}]
        expected = {'vpn': {'children': {'master': {'hosts': {'bibigrid-master-21': 0}},
                                         'vpngtw': {'hosts': {'bibigrid-vpngtw-21-0': {'ansible_host': '42'}}}},
                            'hosts': {}}, 'workers': {
            'children': {'bibigrid_worker_21_0_1': {'hosts': {'bibigrid-worker-21-[0:1]': 1}},
                         'bibigrid_worker_21_2_2': {'hosts': {'bibigrid-worker-21-[2:2]': 2}},
                         'bibigrid_worker_21_3_4': {'hosts': {'bibigrid-worker-21-[3:4]': 4}},
                         'bibigrid_worker_21_5_6': {'hosts': {'bibigrid-worker-21-[5:6]': 5}}}, 'hosts': {}}}
        self.assertEqual(expected,
                         ansible_configurator.generate_ansible_hosts_yaml(42, configuration, cluster_id, startup.LOG))
        call_list = mock_instance_host_dict.call_args_list
        self.assertEqual(call(42), call_list[0])
        for call_happened in call_list[1:]:
            self.assertEqual(call(42, ip=""), call_happened)

    def test_to_instance_host_local(self):
        ip_address = 42
        ssh_user = 21
        local = {"ip": ip_address, "ansible_connection": "ssh",
                 "ansible_python_interpreter": ansible_configurator.PYTHON_INTERPRETER, "ansible_user": ssh_user}
        self.assertEqual(local, ansible_configurator.to_instance_host_dict(21, 42))

    def test_to_instance_host_ssh(self):
        ip_address = 42
        ssh_user = 21
        ssh = {"ip": ip_address, "ansible_connection": "ssh",
               "ansible_python_interpreter": ansible_configurator.PYTHON_INTERPRETER, "ansible_user": ssh_user}
        self.assertEqual(ssh, ansible_configurator.to_instance_host_dict(21, 42))

    def test_get_cidrs(self):
        provider = Mock()
        provider.get_subnet_by_id_or_name.return_value = {"cidr": 42}
        configuration = [{"subnet_cidrs": [21], "cloud_identifier": 13}]
        expected = [{'cloud_identifier': 13, 'provider_cidrs': [21]}]
        self.assertEqual(expected, ansible_configurator.get_cidrs(configuration))

    def test_get_ansible_galaxy_roles_empty(self):
        self.assertEqual([], ansible_configurator.get_ansible_galaxy_roles([], startup.LOG))

    def test_get_ansible_galaxy_roles(self):
        galaxy_roles = [{elem: elem for elem in ["hosts", "name", "galaxy", "git", "url", "vars", "vars_file"]}]
        self.assertEqual(galaxy_roles, ansible_configurator.get_ansible_galaxy_roles(galaxy_roles, startup.LOG))

    def test_get_ansible_galaxy_roles_add(self):
        galaxy_roles = [{elem: elem for elem in ["hosts", "name", "galaxy", "git", "url", "vars", "vars_file"]}]
        galaxy_roles_add = [
            {elem: elem for elem in ["hosts", "name", "galaxy", "git", "url", "vars", "vars_file", "additional"]}]
        self.assertEqual(galaxy_roles, ansible_configurator.get_ansible_galaxy_roles(galaxy_roles_add, startup.LOG))

    def test_get_ansible_galaxy_roles_minus(self):
        galaxy_roles = [{elem: elem for elem in ["hosts", "name", "galaxy", "git", "vars", "vars_file"]}]
        self.assertEqual(galaxy_roles, ansible_configurator.get_ansible_galaxy_roles(galaxy_roles, startup.LOG))

    def test_get_ansible_galaxy_roles_mismatch(self):
        galaxy_roles = [{elem: elem for elem in ["hosts", "name", "vars", "vars_file"]}]
        self.assertEqual([], ansible_configurator.get_ansible_galaxy_roles(galaxy_roles, startup.LOG))

    def test_generate_worker_specification_file_yaml(self):
        configuration = [{"workerInstances": [{elem: elem for elem in ["type", "image"]}], "network": [32]}]
        expected = [{'IMAGE': 'image', 'NETWORK': [32], 'TYPE': 'type'}]
        self.assertEqual(expected,
                         ansible_configurator.generate_worker_specification_file_yaml(configuration, startup.LOG))

    def test_generate_worker_specification_file_yaml_empty(self):
        configuration = [{}]
        expected = []
        self.assertEqual(expected,
                         ansible_configurator.generate_worker_specification_file_yaml(configuration, startup.LOG))

    @patch("yaml.dump")
    def test_write_yaml_no_alias(self, mock_yaml):
        with patch('builtins.open', mock_open()) as output_mock:
            ansible_configurator.write_yaml("here", {"some": "yaml"}, startup.LOG, False)
            output_mock.assert_called_once_with("here", mode="w+", encoding="UTF-8")
            mock_yaml.assert_called_with(data={"some": "yaml"}, stream=ANY, Dumper=NoAliasSafeDumper)

    @patch("yaml.safe_dump")
    def test_write_yaml_alias(self, mock_yaml):
        with patch('builtins.open', mock_open()) as output_mock:
            ansible_configurator.write_yaml("here", {"some": "yaml"}, startup.LOG, True)
            output_mock.assert_called_once_with("here", mode="w+", encoding="UTF-8")
            mock_yaml.assert_called_with(data={"some": "yaml"}, stream=ANY)

    @patch("bibigrid.core.utility.ansible_configurator.write_host_and_group_vars")
    @patch("bibigrid.core.utility.ansible_configurator.generate_worker_specification_file_yaml")
    @patch("bibigrid.core.utility.ansible_configurator.generate_common_configuration_yaml")
    @patch("bibigrid.core.actions.list_clusters.dict_clusters")
    @patch("bibigrid.core.utility.ansible_configurator.generate_ansible_hosts_yaml")
    @patch("bibigrid.core.utility.ansible_configurator.generate_site_file_yaml")
    @patch("bibigrid.core.utility.ansible_configurator.write_yaml")
    @patch("bibigrid.core.utility.ansible_configurator.get_cidrs")
    def test_configure_ansible_yaml(self, mock_cidrs, mock_yaml, mock_site, mock_hosts, mock_list, mock_common,
                                    mock_worker, mock_write):
        mock_cidrs.return_value = 421
        mock_list.return_value = {2: 422}
        provider = MagicMock()
        provider.cloud_specification = {"auth": {"username": "Default"}}
        configuration = [{"sshUser": 42, "userRoles": 21}]
        cluster_id = 2
        ansible_configurator.configure_ansible_yaml([provider], configuration, cluster_id, startup.LOG)
        mock_worker.assert_called_with(configuration, startup.LOG)
        mock_common.assert_called_with(cidrs=421, configurations=configuration, cluster_id=cluster_id, ssh_user=42,
                                       default_user="Default", log=startup.LOG)
        mock_hosts.assert_called_with(42, configuration, cluster_id, startup.LOG)
        mock_site.assert_called_with(21)
        mock_cidrs.assert_called_with(configuration)
        mock_write.assert_called()
        expected = [call(aRP.WORKER_SPECIFICATION_FILE, mock_worker(), startup.LOG, False),
                    call(aRP.COMMONS_CONFIG_FILE, mock_common(), startup.LOG, False),
                    call(aRP.HOSTS_CONFIG_FILE, mock_hosts(), startup.LOG, False),
                    call(aRP.SITE_CONFIG_FILE, mock_site(), startup.LOG, False)]
        self.assertEqual(expected, mock_yaml.call_args_list)


    @patch('bibigrid.core.utility.ansible_configurator.write_yaml')
    @patch('bibigrid.core.utility.ansible_configurator.aRP.GROUP_VARS_FOLDER', '/mocked/path/group_vars')
    @patch('bibigrid.core.utility.ansible_configurator.aRP.HOST_VARS_FOLDER', '/mocked/path/host_vars')
    def test_write_host_and_group_vars(self, mock_write_yaml):
        mock_log = MagicMock()

        mock_provider = MagicMock()
        mock_provider.get_flavor.return_value = {"name": "flavor-name", "ram": 4096, "vcpus": 2, "disk": 40,
                                                 "ephemeral": 0}

        # Define configurations and providers for the test
        configurations = [{"features": ["feature1", "feature2"], "workerInstances": [
            {"type": "m1.small", "count": 2, "image": "worker-image", "onDemand": True, "partitions": ["partition1"],
             "features": "worker-feature"}], "masterInstance": {"type": "m1.large", "image": "master-image"},
                           "network": "private-network", "subnet_cidrs": ["10.0.0.0/24"], "floating_ip": "1.2.3.4",
                           "private_v4": "10.0.0.1", "cloud_identifier": "cloud-1", "volumes": ["volume1"],
                           "fallbackOnOtherImage": False, "wireguard_peer": "peer1"},
                          {"vpnInstance": {"type": "vpn-type", "image": "vpn-image"}, "network": "private-network",
                           "subnet_cidrs": ["10.0.0.0/24"], "floating_ip": "1.2.3.4", "private_v4": "10.0.0.1",
                           "cloud_identifier": "cloud-1", "fallbackOnOtherImage": False, "wireguard_peer": "peer1"}]

        providers = [mock_provider, mock_provider]
        cluster_id = "test-cluster"

        # Call the function under test
        ansible_configurator.write_host_and_group_vars(configurations, providers, cluster_id, mock_log)

        expected_worker_dict = {"name": "bibigrid-worker-test-cluster-[0-1]",
                                "regexp": "bibigrid-worker-test-cluster-\\d+", "image": "worker-image",
                                "network": "private-network",
                                "flavor": {"name": "flavor-name", "ram": 4096, "vcpus": 2, "disk": 40, "ephemeral": 0},
                                "gateway_ip": "10.0.0.1", "cloud_identifier": "cloud-1", "on_demand": True,
                                "state": "CLOUD", "partitions": ["partition1", "all", "cloud-1"],
                                "features": {"feature1", "feature2", "worker-feature"}}

        mock_write_yaml.assert_any_call(
            os.path.join('/mocked/path/group_vars', 'bibigrid_worker_test_cluster_0_1.yaml'), expected_worker_dict,
            mock_log)

        # Assertions for masterInstance
        expected_master_dict = {"name": "bibigrid-master-test-cluster", "image": "master-image",
                                "network": "private-network", "network_cidrs": ["10.0.0.0/24"],
                                "floating_ip": "1.2.3.4",
                                "flavor": {"name": "flavor-name", "ram": 4096, "vcpus": 2, "disk": 40, "ephemeral": 0},
                                "private_v4": "10.0.0.1", "cloud_identifier": "cloud-1", "volumes": ["volume1"],
                                "fallback_on_other_image": False, "state": "UNKNOWN", "on_demand": False,
                                "partitions": ["all", "cloud-1"], "wireguard": {"ip": "10.0.0.1", "peer": "peer1"}}
        mock_write_yaml.assert_any_call(os.path.join('/mocked/path/group_vars', 'master.yaml'), expected_master_dict,
                                        mock_log)

        expected_vpn_dict = {"name": "bibigrid-vpngtw-test-cluster-0", "regexp": "bibigrid-worker-test-cluster-\\d+",
                             "image": "vpn-image", "network": "private-network", "network_cidrs": ["10.0.0.0/24"],
                             "floating_ip": "1.2.3.4", "private_v4": "10.0.0.1",
                             "flavor": {"name": "flavor-name", "ram": 4096, "vcpus": 2, "disk": 40, "ephemeral": 0},
                             "wireguard_ip": "10.0.0.2", "cloud_identifier": "cloud-1",
                             "fallback_on_other_image": False, "on_demand": False,
                             "wireguard": {"ip": "10.0.0.2", "peer": "peer1"}}
        mock_write_yaml.assert_any_call(os.path.join('/mocked/path/host_vars', 'bibigrid-vpngtw-test-cluster-0.yaml'),
                                        expected_vpn_dict, mock_log)


    def test_key_present_with_key_to(self):
        dict_from = {'source_key': 'value1'}
        dict_to = {}
        ansible_configurator.pass_through(dict_from, dict_to, 'source_key', 'destination_key')
        self.assertEqual(dict_to, {'destination_key': 'value1'})

    def test_key_present_without_key_to(self):
        dict_from = {'source_key': 'value2'}
        dict_to = {}
        ansible_configurator.pass_through(dict_from, dict_to, 'source_key')
        self.assertEqual(dict_to, {'source_key': 'value2'})

    def test_key_not_present(self):
        dict_from = {}
        dict_to = {}
        ansible_configurator.pass_through(dict_from, dict_to, 'source_key', 'destination_key')
        self.assertEqual(dict_to, {})

    def test_key_to_not_specified(self):
        dict_from = {'source_key': 'value3'}
        dict_to = {'existing_key': 'existing_value'}
        ansible_configurator.pass_through(dict_from, dict_to, 'source_key')
        self.assertEqual(dict_to, {'existing_key': 'existing_value', 'source_key': 'value3'})

    def test_key_from_not_in_dict_from(self):
        dict_from = {'another_key': 'value4'}
        dict_to = {'existing_key': 'existing_value'}
        ansible_configurator.pass_through(dict_from, dict_to, 'source_key', 'destination_key')
        self.assertEqual(dict_to, {'existing_key': 'existing_value'})

    @patch('bibigrid.core.utility.wireguard.wireguard_keys.generate')
    def test_add_wireguard_peers_multiple_configurations(self, mock_generate):
        # Set up the mock to return specific keys
        mock_generate.return_value = ('private_key_example', 'public_key_example')

        configurations = [{"cloud_identifier": "cloud-1", "floating_ip": "10.0.0.1", "subnet_cidrs": ["10.0.0.0/24"]},
            {"cloud_identifier": "cloud-2", "floating_ip": "10.0.0.2", "subnet_cidrs": ["10.0.1.0/24"]}]

        # Call the function
        ansible_configurator.add_wireguard_peers(configurations)

        # Assert that the wireguard_peer field is added correctly to each configuration
        for config in configurations:
            self.assertIn("wireguard_peer", config)
            self.assertEqual(config["wireguard_peer"]["name"], config["cloud_identifier"])
            self.assertEqual(config["wireguard_peer"]["private_key"], 'private_key_example')
            self.assertEqual(config["wireguard_peer"]["public_key"], 'public_key_example')
            self.assertEqual(config["wireguard_peer"]["ip"], config["floating_ip"])
            self.assertEqual(config["wireguard_peer"]["subnets"], config["subnet_cidrs"])

    def test_add_wireguard_peers_single_configuration(self):
        # Test with only one configuration
        configurations = [{"cloud_identifier": "cloud-1", "floating_ip": "10.0.0.1", "subnet_cidrs": ["10.0.0.0/24"]}]

        # Call the function
        ansible_configurator.add_wireguard_peers(configurations)

        # Assert that no wireguard_peer field is added
        self.assertNotIn("wireguard_peer", configurations[0])

    def test_add_wireguard_peers_empty_list(self):
        # Test with an empty list
        configurations = []

        # Call the function
        ansible_configurator.add_wireguard_peers(configurations)

        # Assert that the configurations list remains empty
        self.assertEqual(configurations, [])

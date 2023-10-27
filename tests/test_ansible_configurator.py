"""
Tests for ansible_configurator
"""
from unittest import TestCase
from unittest.mock import MagicMock, Mock, patch, call, mock_open, ANY

import bibigrid.core.utility.paths.ansible_resources_path as aRP
from bibigrid.core import startup
from bibigrid.core.utility import ansible_configurator


class TestAnsibleConfigurator(TestCase):
    """
    Test ansible configurator test class
    """

    # pylint: disable=R0904
    def test_generate_site_file_yaml_empty(self):
        site_yaml = [{'become': 'yes', 'hosts': 'master',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-master']}],
                      'vars_files': ['vars/common_configuration.yml', 'vars/hosts.yml']},
                     {'become': 'yes', 'hosts': 'vpngtw',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-vpngtw']}],
                      'vars_files': ['vars/common_configuration.yml', 'vars/hosts.yml']},
                     {'become': 'yes', 'hosts': 'workers',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-worker']}],
                      'vars_files': ['vars/common_configuration.yml', 'vars/hosts.yml']}]
        self.assertEqual(site_yaml, ansible_configurator.generate_site_file_yaml([]))

    def test_generate_site_file_yaml_role(self):
        custom_roles = [{"file": "file", "hosts": "hosts", "name": "name", "vars": "vars", "vars_file": "varsFile"}]
        # vars_files = ['vars/login.yml', 'vars/common_configuration.yml', 'varsFile']
        site_yaml = [{'become': 'yes', 'hosts': 'master',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-master']}, 'additional/name'],
                      'vars_files': ['vars/common_configuration.yml', 'vars/hosts.yml', 'varsFile']},
                     {'become': 'yes', 'hosts': 'vpngtw',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-vpngtw']}, 'additional/name'],
                      'vars_files': ['vars/common_configuration.yml', 'vars/hosts.yml', 'varsFile']},
                     {'become': 'yes', 'hosts': 'workers',
                      'roles': [{'role': 'bibigrid', 'tags': ['bibigrid', 'bibigrid-worker']}, 'additional/name'],
                      'vars_files': ['vars/common_configuration.yml', 'vars/hosts.yml', 'varsFile']}]
        self.assertEqual(site_yaml, ansible_configurator.generate_site_file_yaml(custom_roles))

    def test_generate_common_configuration_false(self):
        cidrs = "42"
        cluster_id = "21"
        default_user = "ubuntu"
        ssh_user = "test"
        configuration = [{}]
        common_configuration_yaml = {'auto_mount': False, 'cluster_cidrs': cidrs, 'cluster_id': cluster_id,
                                     'default_user': default_user, 'dns_server_list': ['8.8.8.8'], 'enable_ide': False,
                                     'enable_nfs': False, 'enable_slurm': False, 'enable_zabbix': False,
                                     'local_dns_lookup': False, 'local_fs': False, 'slurm': True,
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 900, 'SuspendTime': 3600,
                                                                           'TreeWidth': 128},
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
        common_configuration_yaml = {'auto_mount': False, 'cluster_cidrs': cidrs, 'cluster_id': cluster_id,
                                     'default_user': default_user, 'dns_server_list': ['8.8.8.8'], 'enable_ide': 'True',
                                     'enable_nfs': False, 'enable_slurm': 'True', 'enable_zabbix': 'True',
                                     'ide_conf': {'build': False, 'ide': False, 'port_end': 8383, 'port_start': 8181,
                                                  'workspace': '${HOME}'}, 'local_dns_lookup': 'True',
                                     'local_fs': 'True', 'slurm': 'True',
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 900, 'SuspendTime': 3600,
                                                                           'TreeWidth': 128},
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
        common_configuration_yaml = {'auto_mount': False, 'cluster_cidrs': cidrs, 'cluster_id': cluster_id,
                                     'default_user': default_user, 'dns_server_list': ['8.8.8.8'], 'enable_ide': False,
                                     'enable_nfs': 'True', 'enable_slurm': False, 'enable_zabbix': False,
                                     'ext_nfs_mounts': [], 'local_dns_lookup': False, 'local_fs': False,
                                     'nfs_mounts': [{'dst': '//vil/mil', 'src': '//vil/mil'},
                                                    {'dst': '//vol/spool', 'src': '//vol/spool'}], 'slurm': True,
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 900, 'SuspendTime': 3600,
                                                                           'TreeWidth': 128},
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
        common_configuration_yaml = {'auto_mount': False, 'cluster_cidrs': cidrs, 'cluster_id': cluster_id,
                                     'default_user': default_user, 'dns_server_list': ['8.8.8.8'], 'enable_ide': False,
                                     'enable_nfs': 'True', 'enable_slurm': False, 'enable_zabbix': False,
                                     'ext_nfs_mounts': [], 'local_dns_lookup': False, 'local_fs': False,
                                     'nfs_mounts': [{'dst': '//vol/spool', 'src': '//vol/spool'}], 'slurm': True,
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 900, 'SuspendTime': 3600,
                                                                           'TreeWidth': 128},
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
        common_configuration_yaml = {'auto_mount': False, 'cluster_cidrs': cidrs, 'cluster_id': cluster_id,
                                     'default_user': default_user, 'dns_server_list': ['8.8.8.8'], 'enable_ide': False,
                                     'enable_nfs': 'True', 'enable_slurm': False, 'enable_zabbix': False,
                                     'ext_nfs_mounts': [{'dst': '/vil/mil', 'src': '/vil/mil'}],
                                     'local_dns_lookup': False, 'local_fs': False,
                                     'nfs_mounts': [{'dst': '//vol/spool', 'src': '//vol/spool'}], 'slurm': True,
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 900, 'SuspendTime': 3600,
                                                                           'TreeWidth': 128},
                                                    'munge_key': 'YryJVnqgg24Ksf8zXQtbct3nuXrMSi9N'},
                                     'ssh_user': ssh_user, 'use_master_as_compute': True}
        generated_common_configuration = ansible_configurator.generate_common_configuration_yaml(cidrs, configuration,
                                                                                                 cluster_id, ssh_user,
                                                                                                 default_user,
                                                                                                 startup.LOG)
        common_configuration_yaml["slurm_conf"]["munge_key"] = generated_common_configuration["slurm_conf"]["munge_key"]
        self.assertEqual(common_configuration_yaml, generated_common_configuration)

    def test_generate_common_configuration_ide(self):
        configuration = [{"ide": "Some1", "ideConf": {"key1": "Some2"}}]
        cidrs = "42"
        cluster_id = "21"
        default_user = "ubuntu"
        ssh_user = "test"
        common_configuration_yaml = {'auto_mount': False, 'cluster_cidrs': cidrs, 'cluster_id': cluster_id,
                                     'default_user': default_user, 'dns_server_list': ['8.8.8.8'],
                                     'enable_ide': 'Some1', 'enable_nfs': False, 'enable_slurm': False,
                                     'enable_zabbix': False,
                                     'ide_conf': {'build': False, 'ide': False, 'key1': 'Some2', 'port_end': 8383,
                                                  'port_start': 8181, 'workspace': '${HOME}'},
                                     'local_dns_lookup': False, 'local_fs': False, 'slurm': True,
                                     'slurm_conf': {'db': 'slurm', 'db_password': 'changeme', 'db_user': 'slurm',
                                                    'elastic_scheduling': {'ResumeTimeout': 900, 'SuspendTime': 3600,
                                                                           'TreeWidth': 128},
                                                    'munge_key': 'b7nks3Ur3kanyPAEBxfSC9ypfSHFnWJL'},
                                     'ssh_user': ssh_user, 'use_master_as_compute': True}
        generated_common_configuration = ansible_configurator.generate_common_configuration_yaml(cidrs, configuration,
                                                                                                 cluster_id, ssh_user,
                                                                                                 default_user,
                                                                                                 startup.LOG)
        common_configuration_yaml["slurm_conf"]["munge_key"] = generated_common_configuration["slurm_conf"]["munge_key"]
        self.assertEqual(common_configuration_yaml, generated_common_configuration)

    def test_generate_common_configuration_ansible_roles_mock(self):
        cidrs = "42"
        ansible_roles = [{elem: elem for elem in ["file", "hosts", "name", "vars", "vars_file"]}]
        cluster_id = "21"
        default_user = "ubuntu"
        ssh_user = "test"
        configuration = [{"ansibleRoles": ansible_roles}]
        generated_common_configuration = ansible_configurator.generate_common_configuration_yaml(cidrs, configuration,
                                                                                                 cluster_id, ssh_user,
                                                                                                 default_user,
                                                                                                 startup.LOG)
        self.assertEqual(ansible_roles, generated_common_configuration["ansible_roles"])

    def test_generate_common_configuration_ansible_galaxy_roles(self):
        cidrs = "42"
        cluster_id = "21"
        default_user = "ubuntu"
        ssh_user = "test"
        galaxy_roles = [{elem: elem for elem in ["hosts", "name", "galaxy", "git", "url", "vars", "vars_file"]}]
        configuration = [{"ansibleGalaxyRoles": galaxy_roles}]
        generated_common_configuration = ansible_configurator.generate_common_configuration_yaml(cidrs, configuration,
                                                                                                 cluster_id, ssh_user,
                                                                                                 default_user,
                                                                                                 startup.LOG)
        self.assertEqual(galaxy_roles, generated_common_configuration["ansible_galaxy_roles"])

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
        expected = {'vpn': {'children': {'master': {'hosts': {'localhost': 0}},
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
        ip = 42
        ssh_user = 21
        local = {"ip": ip, "ansible_connection": "ssh",
                 "ansible_python_interpreter": ansible_configurator.PYTHON_INTERPRETER, "ansible_user": ssh_user}
        self.assertEqual(local, ansible_configurator.to_instance_host_dict(21, 42))

    def test_to_instance_host_ssh(self):
        ip = 42
        ssh_user = 21
        ssh = {"ip": ip, "ansible_connection": "ssh",
               "ansible_python_interpreter": ansible_configurator.PYTHON_INTERPRETER, "ansible_user": ssh_user}
        self.assertEqual(ssh, ansible_configurator.to_instance_host_dict(21, 42))

    def test_get_cidrs(self):
        provider = Mock()
        provider.get_subnet_by_id_or_name.return_value = {"cidr": 42}
        configuration = [{"subnet_cidrs": [21], "cloud_identifier": 13}]
        expected = [{'cloud_identifier': 13, 'provider_cidrs': [21]}]
        self.assertEqual(expected, ansible_configurator.get_cidrs(configuration))

    def test_get_ansible_roles_empty(self):
        self.assertEqual([], ansible_configurator.get_ansible_roles([], startup.LOG))

    def test_get_ansible_roles(self):
        ansible_roles = [{elem: elem for elem in ["file", "hosts", "name", "vars", "vars_file"]}]
        self.assertEqual(ansible_roles, ansible_configurator.get_ansible_roles(ansible_roles, startup.LOG))

    def test_get_ansible_roles_add(self):
        ansible_roles = [{elem: elem for elem in ["file", "hosts", "name", "vars", "vars_file"]}]
        ansible_roles_add = [{elem: elem for elem in ["file", "hosts", "name", "vars", "vars_file", "additional"]}]
        self.assertEqual(ansible_roles, ansible_configurator.get_ansible_roles(ansible_roles_add, startup.LOG))

    def test_get_ansible_roles_minus(self):
        ansible_roles = [{elem: elem for elem in ["file", "hosts"]}]
        self.assertEqual(ansible_roles, ansible_configurator.get_ansible_roles(ansible_roles, startup.LOG))

    def test_get_ansible_roles_mismatch_hosts(self):
        ansible_roles = [{"file": "file"}]
        self.assertEqual([], ansible_configurator.get_ansible_roles(ansible_roles, startup.LOG))

    def test_get_ansible_roles_mismatch_file(self):
        ansible_roles = [{"hosts": "hosts"}]
        self.assertEqual([], ansible_configurator.get_ansible_roles(ansible_roles, startup.LOG))

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

    @patch("logging.warning")
    def test_get_ansible_galaxy_roles_mismatch(self, mock_log):
        galaxy_roles = [{elem: elem for elem in ["hosts", "name", "vars", "vars_file"]}]
        self.assertEqual([], ansible_configurator.get_ansible_galaxy_roles(galaxy_roles, startup.LOG))
        mock_log.assert_called()

    def test_generate_worker_specification_file_yaml(self):
        configuration = [{"workerInstances": [{elem: elem for elem in ["type", "image"]}], "network": [32]}]
        expected = [{'IMAGE': 'image', 'NETWORK': [32], 'TYPE': 'type'}]
        self.assertEqual(expected, ansible_configurator.generate_worker_specification_file_yaml(configuration, startup.LOG))

    def test_generate_worker_specification_file_yaml_empty(self):
        configuration = [{}]
        expected = []
        self.assertEqual(expected, ansible_configurator.generate_worker_specification_file_yaml(configuration, startup.LOG))

    @patch("yaml.dump")
    def test_write_yaml_no_alias(self, mock_yaml):
        with patch('builtins.open', mock_open()) as output_mock:
            ansible_configurator.write_yaml("here", {"some": "yaml"}, False)
            output_mock.assert_called_once_with("here", "w+")
            mock_yaml.assert_called_with(data={"some": "yaml"}, stream=ANY, Dumper=yamlDumper.NoAliasSafeDumper)

    @patch("yaml.safe_dump")
    def test_write_yaml_alias(self, mock_yaml):
        with patch('builtins.open', mock_open()) as output_mock:
            ansible_configurator.write_yaml("here", {"some": "yaml"}, startup.LOG, True)
            output_mock.assert_called_once_with("here", mode="w+", encoding="UTF-8")
            mock_yaml.assert_called_with(data={"some": "yaml"}, stream=ANY)

    @patch("bibigrid.core.utility.id_generation.generate_munge_key")
    @patch("bibigrid.core.utility.ansible_configurator.generate_worker_specification_file_yaml")
    @patch("bibigrid.core.utility.ansible_configurator.generate_common_configuration_yaml")
    @patch("bibigrid.core.actions.list_clusters.dict_clusters")
    @patch("bibigrid.core.utility.ansible_configurator.generate_ansible_hosts_yaml")
    @patch("bibigrid.core.utility.ansible_configurator.get_ansible_roles")
    @patch("bibigrid.core.utility.ansible_configurator.generate_site_file_yaml")
    @patch("bibigrid.core.utility.ansible_configurator.write_yaml")
    @patch("bibigrid.core.utility.ansible_configurator.get_cidrs")
    def test_configure_ansible_yaml(self, mock_cidrs, mock_yaml, mock_site, mock_roles, mock_hosts,
                                    mock_list, mock_common, mock_worker, mock_munge):
        mock_munge.return_value = 420
        mock_cidrs.return_value = 421
        mock_list.return_value = {2: 422}
        mock_roles.return_value = 423
        provider = MagicMock()
        provider.cloud_specification = {"auth": {"username": "Tom"}}
        ansible_configurator.configure_ansible_yaml([provider], [{"sshUser": 42, "ansibleRoles": 21}], 2, startup.LOG)
        mock_munge.assert_called()
        mock_worker.assert_called_with([{"sshUser": 42, "ansibleRoles": 21}])
        mock_common.assert_called_with(421, configuration={"sshUser": 42, "ansibleRoles": 21})
        mock_list.assert_called_with([provider])
        mock_hosts.assert_called_with(42, 422)
        mock_site.assert_called_with(423)
        mock_roles.assert_called_with(21)
        mock_cidrs.assert_called_with([{'sshUser': 42, 'ansibleRoles': 21}], [provider])
        expected = [call(aRP.WORKER_SPECIFICATION_FILE, mock_worker(), False),
                    call(aRP.COMMONS_CONFIG_FILE, mock_common(), False),
                    call(aRP.HOSTS_CONFIG_FILE, mock_hosts(), False), call(aRP.SITE_CONFIG_FILE, mock_site(), False)]
        self.assertEqual(expected, mock_yaml.call_args_list)

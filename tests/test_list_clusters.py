"""
Module test list
"""
from unittest import TestCase
from unittest.mock import Mock

from bibigrid.core import startup
from bibigrid.core.actions import create
from bibigrid.core.actions import list_clusters


class TestList(TestCase):
    """
    Class test list
    """
    def test_setup(self):
        for identifier in [create.WORKER_IDENTIFIER, create.VPN_WORKER_IDENTIFIER, create.MASTER_IDENTIFIER]:
            cluster_id = 42
            provider = Mock()
            provider.name = "name"
            provider.cloud_specification = {"identifier": 21}
            cluster_dict = {}
            server = {"name": identifier(cluster_id=str(cluster_id))}
            self.assertEqual(str(cluster_id), list_clusters.setup(server, identifier, cluster_dict, provider))
            self.assertEqual({str(cluster_id): {'worker': [], 'vpngtw': []}}, cluster_dict)
            self.assertEqual(provider, server["provider"])

    def test_setup_already(self):
        for identifier in [create.WORKER_IDENTIFIER, create.VPN_WORKER_IDENTIFIER, create.MASTER_IDENTIFIER]:
            cluster_id = 42
            test_provider = Mock()
            test_provider.name = "name"
            cluster_dict = {str(cluster_id): {'worker': ["some"], 'vpngtw': ["some"]}}
            server = {"name": identifier(cluster_id=str(cluster_id))}
            self.assertEqual(str(cluster_id), list_clusters.setup(server, identifier, cluster_dict, test_provider))
            self.assertEqual({str(cluster_id): {'worker': ["some"], 'vpngtw': ["some"]}}, cluster_dict)
            self.assertEqual(test_provider, server["provider"])

    def test_dict_clusters(self):
        cluster_id = 42
        expected = {str(cluster_id): {'workers': [{'name': f'bibigrid-worker-{str(cluster_id)}', 'provider': 'Mock'}],
                                      'vpngtws': [{'name': f'bibigrid-vpngtw-{str(cluster_id)}', 'provider': 'Mock'}],
                                      'master': {'name': f'bibigrid-master-{str(cluster_id)}', 'provider': 'Mock'}}}
        provider = Mock()
        provider.list_servers.return_value = [{'name': identifier(cluster_id=str(cluster_id))} for identifier in
                                              [create.WORKER_IDENTIFIER, create.VPN_WORKER_IDENTIFIER,
                                               create.MASTER_IDENTIFIER]]
        self.assertEqual(expected, list_clusters.dict_clusters([provider], startup.LOG))

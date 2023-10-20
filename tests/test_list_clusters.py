"""
Module to test list
"""
from unittest import TestCase
from unittest.mock import Mock

from bibigrid.core import startup
from bibigrid.core.actions import create
from bibigrid.core.actions import list_clusters


class TestList(TestCase):
    """
    Class to test list
    """

    def test_setup(self):
        for identifier in [create.WORKER_IDENTIFIER, create.VPN_WORKER_IDENTIFIER, create.MASTER_IDENTIFIER]:
            cluster_id = "42"
            provider = Mock()
            provider.name = "name"
            provider.cloud_specification = {"identifier": "21"}
            cluster_dict = {}
            server = {"name": identifier(cluster_id=cluster_id)}
            list_clusters.setup(cluster_dict, str(cluster_id), server, provider)

            self.assertEqual({cluster_id: {'workers': [], 'vpngtws': []}}, cluster_dict)
            self.assertEqual(provider.NAME, server["provider"])
            self.assertEqual("21", server["cloud_specification"])

    def test_setup_already(self):
        for identifier in [create.WORKER_IDENTIFIER, create.VPN_WORKER_IDENTIFIER, create.MASTER_IDENTIFIER]:
            cluster_id = "42"
            provider = Mock()
            provider.name = "name"
            provider.cloud_specification = {"identifier": "21"}
            cluster_dict = {cluster_id: {'workers': ["some"], 'vpngtws': ["some"]}}
            server = {"name": identifier(cluster_id=cluster_id)}
            list_clusters.setup(cluster_dict, cluster_id, server, provider)

            self.assertEqual({cluster_id: {'workers': ["some"], 'vpngtws': ["some"]}}, cluster_dict)
            self.assertEqual(provider.NAME, server["provider"])
            self.assertEqual("21", server["cloud_specification"])

    def test_dict_clusters(self):
        cluster_id = "42"
        expected = {cluster_id: {
            'workers': [{'cloud_specification': '21', 'name': f'bibigrid-worker-{cluster_id}-0', 'provider': 'Mock'}],
            'vpngtws': [{'cloud_specification': '21', 'name': f'bibigrid-vpngtw-{cluster_id}-0', 'provider': 'Mock'}],
            'master': {'cloud_specification': '21', 'name': f'bibigrid-master-{cluster_id}', 'provider': 'Mock'}}}
        provider = Mock()
        provider.NAME = "Mock"
        provider.cloud_specification = {"identifier": "21"}
        provider.list_servers.return_value = [{'name': identifier(cluster_id=cluster_id) + "-0"} for identifier in
                                              [create.WORKER_IDENTIFIER, create.VPN_WORKER_IDENTIFIER]] + [
                                                 {'name': create.MASTER_IDENTIFIER(cluster_id=cluster_id)}]
        self.assertEqual(expected, list_clusters.dict_clusters([provider], startup.LOG))

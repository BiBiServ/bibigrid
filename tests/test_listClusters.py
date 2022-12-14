from unittest import TestCase
from unittest.mock import Mock

import bibigrid2.core.actions.create as create
import bibigrid2.core.actions.list_clusters as listClusters


class TestDictClusters(TestCase):
    def test_setup(self):
        for identifier in [create.WORKER_IDENTIFIER, create.VPN_WORKER_IDENTIFIER, create.MASTER_IDENTIFIER]:
            cluster_id = 42
            test_provider = Mock()
            test_provider.name = "name"
            cluster_dict = {}
            server = {"name": identifier + create.SEPARATOR + str(cluster_id)}
            self.assertEqual(str(cluster_id),
                             listClusters.setup(server,
                                                identifier, cluster_dict, test_provider))
            self.assertEqual({str(cluster_id): {'worker': [], 'vpnwkr': []}}, cluster_dict)
            self.assertEqual(test_provider, server["provider"])

    def test_setup_already(self):
        for identifier in [create.WORKER_IDENTIFIER, create.VPN_WORKER_IDENTIFIER, create.MASTER_IDENTIFIER]:
            cluster_id = 42
            test_provider = Mock()
            test_provider.name = "name"
            cluster_dict = {str(cluster_id): {'worker': ["some"], 'vpnwkr': ["some"]}}
            server = {"name": identifier + create.SEPARATOR + str(cluster_id)}
            self.assertEqual(str(cluster_id),
                             listClusters.setup(server,
                                                identifier, cluster_dict, test_provider))
            self.assertEqual({str(cluster_id): {'worker': ["some"], 'vpnwkr': ["some"]}}, cluster_dict)
            self.assertEqual(test_provider, server["provider"])

    def test_dict_clusters(self):
        cluster_id = 42
        expected = {str(cluster_id): {'workers': [{'name': f'bibigrid-worker-{str(cluster_id)}', 'provider': 'Mock'}],
                                      'vpnwkrs': [
                                          {'name': f'bibigrid-vpnwkr-{str(cluster_id)}', 'provider': 'Mock'}],
                                      'master': {'name': f'bibigrid-master-{str(cluster_id)}', 'provider': 'Mock'}}}
        provider = Mock()
        provider.list_servers.return_value = [{'name': identifier + create.SEPARATOR + str(cluster_id)} for identifier
                                              in
                                              [create.WORKER_IDENTIFIER, create.VPN_WORKER_IDENTIFIER,
                                               create.MASTER_IDENTIFIER]]
        self.assertEqual(expected, listClusters.dict_clusters([provider]))

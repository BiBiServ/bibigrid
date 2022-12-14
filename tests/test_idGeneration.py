from unittest import TestCase
from unittest.mock import Mock, MagicMock, patch

import bibigrid2.core.actions.create as create
import bibigrid2.core.utility.id_generation as idGeneration


class Test(TestCase):

    def test_generate_cluster_id(self):
        """
        This test is not ideal, but prevents worst changes within a reasonable runtime
        :return:
        """
        test_list = []
        for x in range(10000):
            test_list.append(idGeneration.generate_cluster_id())
        self.assertTrue(len(set(test_list)) == len(test_list))

    @patch("bibigrid2.core.utility.idGeneration.generate_cluster_id")
    def test_generate_safe_cluster_id(self, mock_generate_cluster_id):
        mock_generate_cluster_id.return_value = 21
        with patch("bibigrid2.core.utility.idGeneration.is_unique_cluster_id") as mock_is_unique:
            mock_is_unique.side_effect = [True]
            self.assertTrue(idGeneration.generate_safe_cluster_id([42]))
            mock_is_unique.assert_called_with(21, [42])

    def test_is_unique_cluster_id_duplicate(self):
        cluster_id = 42
        provider = Mock()
        provider.list_servers = MagicMock(
            return_value=[{"name": create.MASTER_IDENTIFIER + create.SEPARATOR + str(cluster_id)}])
        self.assertFalse(idGeneration.is_unique_cluster_id(str(cluster_id), [provider]))
        provider.list_servers.assert_called()

    def test_is_unique_cluster_id_unique(self):
        cluster_id = 42
        provider = Mock()
        provider.list_servers = MagicMock(
            return_value=[{"name": create.MASTER_IDENTIFIER + create.SEPARATOR + str(cluster_id + 1)}])
        self.assertTrue(idGeneration.is_unique_cluster_id(str(cluster_id), [provider]))
        provider.list_servers.assert_called()

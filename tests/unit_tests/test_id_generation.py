"""
Module to test id_generation
"""
from unittest import TestCase
from unittest.mock import Mock, MagicMock, patch

from bibigrid.core.actions import create
from bibigrid.core.utility import id_generation


class TestIDGeneration(TestCase):
    """
    Class to test id_generation
    """

    def test_generate_cluster_id(self):
        """
        This test is not ideal, but prevents worst within a reasonable runtime
        @return:
        """
        test_list = []
        for _ in range(10000):
            test_list.append(id_generation.generate_cluster_id())
        self.assertTrue(len(set(test_list)) == len(test_list))

    @patch("bibigrid.core.utility.id_generation.generate_cluster_id")
    def test_generate_safe_cluster_id(self, mock_generate_cluster_id):
        mock_generate_cluster_id.return_value = 21
        with patch("bibigrid.core.utility.id_generation.is_unique_cluster_id") as mock_is_unique:
            mock_is_unique.side_effect = [True]
            self.assertTrue(id_generation.generate_safe_cluster_id([42]))
            mock_is_unique.assert_called_with(21, [42])

    def test_is_unique_cluster_id_duplicate(self):
        cluster_id = "42"
        provider = Mock()
        provider.list_servers = MagicMock(
            return_value=[{"name": create.MASTER_IDENTIFIER(cluster_id=cluster_id)}])
        self.assertFalse(id_generation.is_unique_cluster_id(str(cluster_id), [provider]))
        provider.list_servers.assert_called()

    def test_is_unique_cluster_id_unique(self):
        cluster_id = 42
        provider = Mock()
        provider.list_servers = MagicMock(
            return_value=[{"name": create.MASTER_IDENTIFIER(cluster_id=str(cluster_id + 1))}])
        self.assertTrue(id_generation.is_unique_cluster_id(str(cluster_id), [provider]))
        provider.list_servers.assert_called()

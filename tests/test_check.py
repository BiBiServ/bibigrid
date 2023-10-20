"""
Module to test check
"""
from unittest import TestCase
from unittest.mock import patch

from bibigrid.core import startup
from bibigrid.core.actions import check


class TestCheck(TestCase):
    """
    Class to test check
    """
    @patch("bibigrid.core.utility.validate_configuration.ValidateConfiguration")
    def test_check_true(self, mock_validator):
        providers = [42]
        configurations = [32]
        self.assertFalse(check.check(configurations, providers, startup.LOG))
        mock_validator.assert_called_once_with(configurations, providers, startup.LOG)

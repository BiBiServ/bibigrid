from unittest import TestCase
from unittest.mock import patch

import bibigrid2.core.actions.check as check
import bibigrid2.core.utility.validate_configuration as validateConfiguration


class TestCheck(TestCase):

    @patch("logging.info")
    def test_check_true(self, mock_log):
        providers = [42]
        configurations = [32]
        with patch.object(validateConfiguration.ValidateConfiguration, "validate", return_value=True) as mock_validate:
            self.assertFalse(check.check(configurations, providers))
            mock_validate.assert_called()
            mock_log.assert_called_with("Total check returned True.")

    @patch("logging.info")
    def test_check_false(self, mock_log):
        providers = [42]
        configurations = [32]
        with patch.object(validateConfiguration.ValidateConfiguration, "validate", return_value=False) as mock_validate:
            self.assertFalse(check.check(configurations, providers))
            mock_validate.assert_called()
            mock_log.assert_called_with("Total check returned False.")

    @patch("bibigrid2.core.utility.validateConfiguration.ValidateConfiguration")
    def test_check_init(self, mock_validator):
        providers = [42]
        configurations = [32]
        self.assertFalse(check.check(configurations, providers))
        mock_validator.assert_called_with(configurations, providers)

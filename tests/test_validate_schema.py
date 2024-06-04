"""
Tests for validate schema
"""

import glob
import os
from unittest import TestCase

import yaml

from bibigrid.core import startup
from bibigrid.core.utility.validate_schema import validate_configurations

TEST_CONFIGURATION_DIRECTORY = "../resources/tests/schema"


class TestValidateSchema(TestCase):
    """
    Validate Schema
    """

    def test_validate_configurations(self):
        for bibigrid_configuration_name in glob.iglob(f'{TEST_CONFIGURATION_DIRECTORY}/*.yml'):
            with open(bibigrid_configuration_name, mode="r", encoding="utf-8") as config_file:
                config_yaml = yaml.safe_load(config_file)
                for cloud_config in config_yaml:
                    cloud_config["cloud_identifier"] = "some"
                result = validate_configurations(config_yaml, startup.LOG)
                self.assertEqual(not os.path.basename(bibigrid_configuration_name).startswith("error"), result)

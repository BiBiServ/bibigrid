"""
Tests for validate schema
"""

import glob
import os
from unittest import TestCase

import yaml

from bibigrid.core import startup
from bibigrid.core.utility.validate_schema import validate_configurations, str_dict_or_none, SchemaError

TEST_CONFIGURATION_DIRECTORY = "../resources/tests/schema"


class TestValidateSchema(TestCase):
    """
    Validate Schema
    """

    def test_validate_configurations(self):
        for bibigrid_configuration_name in glob.iglob(f'{TEST_CONFIGURATION_DIRECTORY}/*.yaml'):
            with open(bibigrid_configuration_name, mode="r", encoding="UTF-8") as config_file:
                config_yaml = yaml.safe_load(config_file)
                for cloud_config in config_yaml:
                    cloud_config["cloud_identifier"] = "some"
                result = validate_configurations(config_yaml, startup.LOG)
                self.assertEqual(not os.path.basename(bibigrid_configuration_name).startswith("error"), result)

    def test_none_input(self):
        self.assertIsNone(str_dict_or_none(None))

    def test_valid_dict(self):
        data = {"key": "value"}
        self.assertEqual(str_dict_or_none(data), data)

    def test_invalid_type(self):
        with self.assertRaises(SchemaError) as cm:
            str_dict_or_none("not a dict")
        self.assertIn("must be a dict or None", str(cm.exception))

    def test_non_string_key(self):
        with self.assertRaises(SchemaError) as cm:
            str_dict_or_none({123: "value"})
        self.assertIn("keys and values must be strings", str(cm.exception))

    def test_non_string_value(self):
        with self.assertRaises(SchemaError) as cm:
            str_dict_or_none({"key": 456})
        self.assertIn("keys and values must be strings", str(cm.exception))

    def test_key_too_long(self):
        long_key = "A" * 256
        with self.assertRaises(SchemaError) as cm:
            str_dict_or_none({long_key: "value"})
        self.assertIn("length <= 255", str(cm.exception))

    def test_value_too_long(self):
        long_value = "B" * 256
        with self.assertRaises(SchemaError) as cm:
            str_dict_or_none({"key": long_value})
        self.assertIn("length <= 255", str(cm.exception))

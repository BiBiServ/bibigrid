import unittest

from pydantic import ValidationError

from bibigrid.models.configuration import (
    ConfigurationsModel,
    Volume,
)


class TestConfigurationsModel(unittest.TestCase):
    """
    Tests for ConfigurationsModel and its validators.
    """

    # ---------- helpers ----------

    @staticmethod
    def minimal_instance():
        return {
            "type": "m1.small",
            "image": "ubuntu-22.04",
        }

    @classmethod
    def minimal_base_config(cls):
        return {
            "infrastructure": "openstack",
            "sshUser": "ubuntu",
            "subnet": "subnet-1",
            "workerInstances": [cls.minimal_instance()],
        }

    @classmethod
    def master_config(cls):
        cfg = cls.minimal_base_config()
        cfg.update(
            {
                "masterInstance": cls.minimal_instance(),
            }
        )
        return cfg

    @classmethod
    def other_config(cls):
        cfg = cls.minimal_base_config()
        cfg.update(
            {
                "vpnInstance": cls.minimal_instance(),
            }
        )
        return cfg

    # ---------- happy paths ----------

    def test_accepts_list_input(self):
        model = ConfigurationsModel.model_validate(
            [self.master_config(), self.other_config()]
        )

        self.assertEqual(model.master.sshUser, "ubuntu")
        self.assertEqual(len(model.others), 1)
        self.assertEqual(
            model.others[0].vpnInstance.type,
            "m1.small",
        )

    def test_accepts_configurations_key(self):
        model = ConfigurationsModel.model_validate(
            {
                "configurations": [
                    self.master_config(),
                    self.other_config(),
                ]
            }
        )

        self.assertIsNotNone(model.master)
        self.assertEqual(len(model.others), 1)

    def test_single_configuration(self):
        model = ConfigurationsModel.model_validate(
            [self.master_config()]
        )

        self.assertIsNotNone(model.master)
        self.assertEqual(model.others, [])

    # ---------- validation errors ----------

    def test_empty_list_raises(self):
        with self.assertRaises(ValueError):
            ConfigurationsModel.model_validate([])

    def test_extra_fields_forbidden(self):
        cfg = self.master_config()
        cfg["unexpected"] = 123

        with self.assertRaises(ValidationError):
            ConfigurationsModel.model_validate([cfg])

    def test_subnet_network_xor_violation(self):
        cfg = self.master_config()
        cfg["network"] = "net-1"  # subnet already set

        with self.assertRaises(ValidationError):
            ConfigurationsModel.model_validate([cfg])

    # ---------- custom dump ----------

    def test_model_custom_dump(self):
        model = ConfigurationsModel.model_validate(
            [self.master_config(), self.other_config()]
        )

        dumped = model.model_custom_dump()

        self.assertIsInstance(dumped, list)
        self.assertEqual(dumped[0]["sshUser"], "ubuntu")
        self.assertEqual(
            dumped[1]["vpnInstance"]["type"],
            "m1.small",
        )

    def test_model_custom_dump_roundtrip(self):
        model = ConfigurationsModel.model_validate(
            [self.master_config(), self.other_config()]
        )

        dumped = model.model_custom_dump()
        model2 = ConfigurationsModel.model_validate(dumped)

        self.assertEqual(
            model2.master.sshUser,
            model.master.sshUser,
        )


class TestVolumeValidators(unittest.TestCase):
    """
    Tests for Volume-specific validators.
    """

    def test_only_one_flag_allowed(self):
        Volume(permanent=True)

        with self.assertRaises(ValidationError):
            Volume(permanent=True, exists=True)

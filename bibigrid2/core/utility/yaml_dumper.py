"""
Alternative version of yaml.SafeDumper that ignores aliases.
"""

import yaml


class NoAliasSafeDumper(yaml.SafeDumper):
    """
    Only difference to the regular yaml.SafeDumper class is that ignore_aliases is true
    and therefore aliases are ignored.
    """

    def ignore_aliases(self, data):
        return True

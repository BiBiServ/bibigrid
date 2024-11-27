"""
You need a network with a subnet in order to integration test the providers
Additionally you need to set:
OS_IMAGE=[any available image];
OS_FLAVOR=[any available flavor];
OS_NETWORK=[existing network in your project connected to an external network via a router];
OS_KEY_NAME=[your keyname];
OS_SNAPSHOT=[a snapshot you created]
The integration tests will create a volume that is not deleted.
"""

import logging
import os
import sys
import unittest
from contextlib import contextmanager


@contextmanager
def suppress_stdout():
    """
    Suppresses stdout within it
    @return:
    """
    with open(os.devnull, "w", encoding="utf-8") as devnull:
        old_stdout = sys.stdout
        sys.stdout = devnull
        try:
            yield
        finally:
            sys.stdout = old_stdout


logging.basicConfig(level=logging.ERROR)
if __name__ == '__main__':
    # Unittests
    # Configuration at resources/tests/bibigrid_test.yaml
    # Needs to contain providers and infrastructures for the provider tests
    # You can use bibigrid_test_exmaple.yaml as the basis
    print(os.path.abspath("."))
    suite = unittest.TestLoader().discover(start_dir=".", pattern='test_*.py')
    with suppress_stdout():
        unittest.TextTestRunner(verbosity=2).run(suite)

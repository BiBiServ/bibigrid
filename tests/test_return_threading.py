"""
Module to test return thread
"""
from unittest import TestCase

import bibigrid.models.return_threading as returnThreading


def test_method(elem):
    return 42, elem


class TestReturnThread(TestCase):
    """
    Class to test return thread
    """

    def test_return_thread(self):
        return_thread = returnThreading.ReturnThread(target=test_method, args=[42])
        return_thread.start()
        return_value = return_thread.join()
        self.assertTrue(return_value == (42, 42))

from unittest import TestCase

import bibigrid2.models.return_threading as returnThreading


def test_method(x):
    return (42, x)


class TestReturnThread(TestCase):

    def test_ReturnThread(self):
        return_thread = returnThreading.ReturnThread(target=test_method,
                                                     args=[42])
        return_thread.start()
        return_value = return_thread.join()
        self.assertTrue(return_value == (42, 42))

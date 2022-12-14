"""
Expands threading.
"""

import threading


class ReturnThread(threading.Thread):
    """
    Extends the Thread functionality:
    - Return value of called function is returned by join()
    - An exception occurred within the called function is raised by join()
    """

    def __init__(self, group=None, target=None, name=None, args=(), kwargs={}):  # pylint: disable=dangerous-default-value
        threading.Thread.__init__(self, group, target, name, args, kwargs)
        self._return = None
        self._exc = None

    def run(self):
        if self._target is not None:
            try:
                self._return = self._target(*self._args, **self._kwargs)
            except Exception as exc: # pylint: disable=broad-except
                self._exc = exc

    def join(self, *args):
        threading.Thread.join(self, *args)
        if self._exc:
            raise self._exc
        return self._return

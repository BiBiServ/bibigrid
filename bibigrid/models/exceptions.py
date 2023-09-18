""" module for additional exceptions """


class ConnectionException(Exception):
    """ Connection exception. """


class ExecutionException(Exception):
    """ Execution exception. """


class ConfigurationException(Exception):
    """ Configuration exception"""


class ConflictException(Exception):
    """ Conflict exception"""


class ImageDeactivatedException(Exception):
    """ Image deactivated exception"""


class ImageNotFoundException(Exception):
    """ Image not found exception"""

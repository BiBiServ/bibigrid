""" module for additional exceptions """


class ConnectionException(Exception):
    """ Connection exception. """


class ExecutionException(Exception):
    """ Execution exception. """


class ConfigurationException(Exception):
    """ Configuration exception"""


class ConflictException(Exception):
    """ Conflict exception"""


class ImageNotActiveException(Exception):
    """ Image deactivated exception"""


class ImageDeactivatedException(ImageNotActiveException):
    """ Image deactivated exception"""


class ImageNotFoundException(ImageNotActiveException):
    """ Image not found exception"""

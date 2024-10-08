#!/usr/bin/env python3
"""
Module for wireguard conforming base64 key creation
"""
import codecs

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.x25519 import X25519PrivateKey


def generate():
    """
    Generates private and public key for wireguard
    @return: tuple (private_key_str, publickey_str)
    """
    # generate private key
    private_key = X25519PrivateKey.generate()
    bytes_ = private_key.private_bytes(
        encoding=serialization.Encoding.Raw,
        format=serialization.PrivateFormat.Raw,
        encryption_algorithm=serialization.NoEncryption()
    )
    private_key_str = codecs.encode(bytes_, 'base64').decode('utf8').strip()

    # derive public key
    publickey = private_key.public_key().public_bytes(encoding=serialization.Encoding.Raw,
                                                      format=serialization.PublicFormat.Raw)
    publickey_str = codecs.encode(publickey, 'base64').decode('utf8').strip()
    return private_key_str, publickey_str

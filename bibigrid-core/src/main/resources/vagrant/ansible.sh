#!/usr/bin/env bash

echo "Install python2"
apt-get install apt-transport-https ca-certificates software-properties-common python python-pip -qq

echo "Install ansible from pypi using pip"
pip install ansible -q
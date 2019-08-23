#!/usr/bin/env bash

echo "Install python3"
apt-get install apt-transport-https ca-certificates software-properties-common python3 python3-pip -qq

echo "Install ansible from pypi using pip3"
pip3 install ansible -q
#!/usr/bin/env bash

cat > /etc/resolv.conf << "END"
nameserver 192.168.33.10
search bibigrid.de
END
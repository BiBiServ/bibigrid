#!/usr/bin/env bash

apt-get install bind9 -qq
cp /vagrant/vagrant/bind/* /etc/bind/
chown root:bind /etc/bind/*
chmod 644 /etc/bind/*
systemctl restart bind9
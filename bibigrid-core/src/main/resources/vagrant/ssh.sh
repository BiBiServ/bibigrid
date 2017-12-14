#!/usr/bin/env
sudo -u ubuntu cp /vagrant/.vagrant/machines/default/virtualbox/private_key /home/ubuntu/.ssh/id_rsa
sudo -u ubuntu ssh-keygen -y -f /home/ubuntu/.ssh/id_rsa >> /home/ubuntu/.ssh/authorized_keys
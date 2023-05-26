# Zabbix

Zabbix is a monitoring software. It works great on a single-cloud, but currently has some issues on multi-cloud.
We are working on this.

If you activated zabbix in the [configuration](../features/configuration.md), you can connect to zabbix by creating a
port forwarding: from a port of your choice (here `9090`) to `80` on the remote machine:
```commandline
`ssh -L 127.0.0.1:9090:127.0.0.1:80 ubuntu@123.45.67.89` 
```
then visit `127.0.0.1:9090` to see the zabbix main page (apache server).
#!/usr/bin/env python3
"""
This module is used to delete hosts once they are powered off by slurm
"""
import argparse
import sys

from zabbix_api import ZabbixAPI, ZabbixAPIException

parser = argparse.ArgumentParser(description='Delete hosts from Zabbix host database.')
parser.add_argument("--url", help="Zabbix server url (default: http://127.0.0.1/zabbix )", type=str,
                    default="http://127.0.0.1/zabbix")
parser.add_argument("--user", help="Zabbix user (default: Admin)", type=str, default="Admin")
parser.add_argument("--pwd", help="Zabbix user password", type=str, required=True)
parser.add_argument("hosts", help="List of Zabbix host names", metavar='host', type=str, nargs="+")

args = parser.parse_args()

try:
    zapi = ZabbixAPI(server=args.url)
    zapi.login(args.user, args.pwd)
    hosts = zapi.host.get({"filter": {"host": args.hosts}, "output": "hostid"})

    for host in hosts:
        result = zapi.host.delete([host["hostid"]])
        print(f"Host[s] {','.join(result['hostids'])} removed from Zabbix database.")

except ZabbixAPIException as e:
    print(e)
    sys.exit(1)

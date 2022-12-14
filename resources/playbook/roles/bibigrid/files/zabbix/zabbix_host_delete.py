#!/usr/bin/env python3
from zabbix_api import ZabbixAPI, ZabbixAPIException
import argparse

parser = argparse.ArgumentParser(description='Delete hosts from Zabbix host database.')
parser.add_argument("--url",help="Zabbix server url (default: http://127.0.0.1/zabbix )", type=str, default="http://127.0.0.1/zabbix")
parser.add_argument("--user",help="Zabbix user (default: Admin)",type=str, default="Admin")
parser.add_argument("--pwd",help="Zabbix user password",type=str, required=True)
parser.add_argument("hosts",help="List of Zabbix host names", metavar='host', type=str, nargs="+")


args = parser.parse_args()

try:
    zapi = ZabbixAPI(server=args.url)
    zapi.login(args.user,args.pwd)
    hosts = zapi.host.get({
        "filter": {
            "host" : args.hosts
        },
        "output": "hostid"
    })

    for host in hosts:
        result = zapi.host.delete([host["hostid"]])
        print (f"Host[s] {','.join(result['hostids'])} removed from Zabbix database.")

except ZabbixAPIException as e:
    print(e)
    exit(1)
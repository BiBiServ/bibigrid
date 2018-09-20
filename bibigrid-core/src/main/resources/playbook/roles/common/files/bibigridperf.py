#!/usr/bin/env python

import subprocess
import os
import io
import json


def execute_cmd(params):
    proc = subprocess.Popen(params, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    result = proc.communicate()
    if result[0] is not None and len(result[0]) > 0:
        return result[0]
    return result[1]


passes = 3

mem_text = execute_cmd(['cat', '/proc/meminfo'])
mem_info = {x[0].lower().strip(): x[1].strip() for x in [y.split(': ') for y in mem_text.split('\n') if ': ' in y]}
mem_total = int(mem_info['memtotal'].split(' ')[0]) / 1024
# TODO: we currently can't get the RAM type or speed
print('%s MiB Memory' % (mem_total))

cpu_text = execute_cmd(['lscpu'])
cpu_info = {x[0].lower().strip(): x[1].strip() for x in [y.split(': ') for y in cpu_text.split('\n') if ': ' in y]}
cpu_count = int(cpu_info['cpu(s)'])
cpu_speed = float(cpu_info['cpu mhz'])
threads_per_core = int(cpu_info['thread(s) per core'])
# TODO: we currently may only get a very simplified CPU type
print('%s CPU cores with %s threads and %s MHz' % (cpu_count, threads_per_core, cpu_speed))

write_speed = 0.0
write_speed_text = execute_cmd(['dd', 'if=/dev/zero', 'of=/tmp/test1.img', 'bs=256M', 'count=%s' % passes, 'oflag=dsync'])
write_speed_text = write_speed_text.split(', ')[-1]
write_speed_text = write_speed_text.split(' ')[0]
write_speed = float(write_speed_text)
print('Average write speed of %.2f MB/sec over %s passes' % (write_speed, passes))

read_speed = 0.0
for i in range(0, passes):
    read_speed_text = execute_cmd(['hdparm', '-t', '/dev/vda1'])
    read_speed_text = read_speed_text.split(' =')[1]
    read_speed_text = read_speed_text.strip().split(' ')[0]
    read_speed += float(read_speed_text)
read_speed = read_speed / passes
print('Average read speed of %.2f MB/sec over %s passes' % (read_speed, passes))

with io.open('bibigridperf.json', 'w', encoding='utf-8') as f:
    f.write(unicode(json.dumps({
        'cpu': {
            'count': cpu_count,
            'threads': threads_per_core,
            'speed': cpu_speed
        },
        'mem': {
            'size': mem_total
        },
        'disk': {
            'read': read_speed,
            'write': write_speed
        }
    }, ensure_ascii=False, indent=2)))

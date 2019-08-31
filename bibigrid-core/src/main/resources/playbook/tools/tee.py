#!/usr/bin/env python3
from argparse import ArgumentParser
from subprocess import Popen,PIPE
import sys
from queue import Queue
from threading import Thread
from pathlib import Path

parser = ArgumentParser(
    description='Tee like program, that executes given cmd and branch stdout and stderr\n.'
                'Exits with the exit code of called program.')
parser.add_argument ("--cmd", help="A cmd executed by tee.py, e.g. --cmd \"ansible-playbook -i hosts site.yml\"",required=True)
parser.add_argument ("--outfile", help="Path to stdoutfile",required=True)
args = parser.parse_args()

cmds = args.cmd.split(" ")
cmd = Path(cmds[0])
if not(cmd.is_file()) :
    sys.exit("{} is not an file".format(cmd))

def reader(pipe, queue):
    try:
        with pipe:
            for line in iter(pipe.readline, b''):
                queue.put(line.decode('utf-8'))
    finally:
        queue.put(None)

p = Popen(cmds,shell=False,stdout=PIPE,stderr=PIPE,bufsize=1)

q = Queue()
Thread(target=reader, args=[p.stdout, q]).start()
Thread(target=reader, args=[p.stderr, q]).start()

f = open(args.outfile,"w")
for line in iter(q.get, None):
    # print to stdout
    sys.stdout.write(line)
    # print to file
    f.write(line)
f.close()

# wait until process is finished ...
rt = p.wait()
# and returns it returncode
sys.exit(rt)





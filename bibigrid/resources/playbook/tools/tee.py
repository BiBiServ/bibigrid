#!/usr/bin/env python3
"""
Tee like program, that executes given cmd and branch stdout and stderr\n. Exits with the exit code of called program.
"""
from argparse import ArgumentParser
from subprocess import Popen, PIPE
import sys
from queue import Queue
from threading import Thread
from pathlib import Path

parser = ArgumentParser(description='Tee like program, that executes given cmd and branch stdout and stderr\n.'
                                    'Exits with the exit code of called program.')
parser.add_argument("--cmd", help="A cmd executed by tee.py, e.g. --cmd \"ansible-playbook -i hosts site.yaml\"",
                    required=True)
parser.add_argument("--outfile", help="Path to stdoutfile", required=True)
args = parser.parse_args()

cmds = args.cmd.split(" ")
cmd = Path(cmds[0])
if not cmd.is_file():
    sys.exit(f"{cmd} is not an file")


def reader(pipe, queue):
    try:
        with pipe:
            for read_line in iter(pipe.readline, b''):
                queue.put(read_line.decode('utf-8'))
    finally:
        queue.put(None)


with Popen(cmds, shell=False, stdout=PIPE, stderr=PIPE, bufsize=1) as p:
    q = Queue()
    Thread(target=reader, args=[p.stdout, q]).start()
    Thread(target=reader, args=[p.stderr, q]).start()

    with open(args.outfile, "w", encoding="UTF-8") as outfile:
        for line in iter(q.get, None):
            # print to stdout
            sys.stdout.write(line)
            sys.stdout.flush()
            # print to file
            outfile.write(line)
            outfile.flush()

    # wait until process is finished ...
    RETURN_CODE = p.wait()
# and returns its return code
sys.exit(RETURN_CODE)

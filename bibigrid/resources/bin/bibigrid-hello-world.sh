#!/bin/bash
exe() { echo "\$" "$@" ; "$@" ; }

echo "Hello, World! This program will show very basic slurm scheduling."
echo "I) Only execute this just after logging in and without any prior changes"
echo "II) You need to have at least one worker in your configuration or this program will hang at some point."
echo "III) The master should be configured to work as well or this program will hang at some point."
read -n 1 -r -s -p $'Press enter to continue...\n'
echo "Let's see which servers are up using sinfo (slurm info)!"
exe sinfo
echo -e "\nOnly the master is up, since all other workers are configured, but not powered up ('~' is used for nodes that are powered down)."
echo "See here for more info about node states: https://slurm.schedmd.com/sinfo.html#SECTION_NODE-STATE-CODES"
read -n 1 -r -s -p $'Press enter to continue...\n'
echo -e "\nLet's execute the 'hostname' command:"
exe srun hostname
echo -e "\nAnd see if a server started"
exe sinfo
echo -e "\nSince the master is a worker, too, no need to start new workers."
read -n 1 -r -s -p $'Press enter to continue...\n'
echo -e "\nWhat if we need another server? Let's exclude $(hostname) for now using (-x node-name-to-exclude), so slurm has to power up a worker node."
echo "While it starts, open another terminal and execute 'squeue'. That will show you the running job."
echo "Also execute 'sinfo' that will show you the node is powering up ('#' is used for nodes that are powering up). But now let's start another node:"
start_time=$(date +%T)
exe srun -x "$(hostname)" hostname
echo "We triggered the power up at: $(date +%T). Now it's $start_time."
echo -e "\nLet's see what changed."
exe sinfo
echo "Now a worker powered up as we can see looking at 'sinfo'"
read -n 1 -r -s -p $'Press enter to continue...\n'
echo -e "\nWorkers that are not used will be shut down after a while."

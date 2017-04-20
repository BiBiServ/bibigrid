#! /bin/bash


#This script is for installing all mandatory and optional ansible
#playbooks which have been copied to the master machine in the cloud.
#Author: Alex Walender <awalende@cebitec.uni-bielefeld.de>
#Usage: playbookinstall.sh [PATH TO TMP FOLDER] [RETRYS]
FOLDER_PATH=$1
MAX_RETRY=$2

mkdir -p $FOLDER_PATH/log

echo "Installing mandatory playbook files."
now=$(date +"%d_%m_%Y_%Hh_%Mm_%Ss")
for f in $FOLDER_PATH/mandatory/*.yaml
do
	COUNTER=0
	while [ $COUNTER -lt $MAX_RETRY ]
	do
		ansible-playbook $f >> $FOLDER_PATH/log/mand_$now.log
		rm $FOLDER_PATH/mandatory/*.retry
		if [ $? -eq 0 ]
		then
			break
		fi
		COUNTER=$(($COUNTER+1))
		sleep 10
	done
	echo "Could not install $f"
done

echo "_______________________________________"

echo "Installing additional playbook files."
now=$(date +"%d_%m_%Y_%Hh_%Mm_%Ss")
for f in $FOLDER_PATH/additional/*.yaml
do
	COUNTER=0
	while [ $COUNTER -lt $MAX_RETRY ]
	do
		ansible-playbook $f >> $FOLDER_PATH/log/add_$now.log
		rm $FOLDER_PATH/additional/*.retry
		if [ $? -eq 0 ]
		then
			break
		fi
		COUNTER=$(($COUNTER+1))
		sleep 10
	done
	echo "Could not install $f"
done


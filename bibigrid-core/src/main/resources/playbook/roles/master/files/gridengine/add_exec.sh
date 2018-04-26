#!/bin/bash

if [ $# -ne 2 ] ; then
    echo "usage:: $0 <hostname> $1 <cores>";
    exit;
fi;

echo "add '$1' as exec host"

# create simple configuration file
echo -e "hostname              $1
load_scaling          NONE
complex_values        NONE
user_lists            NONE
xuser_lists           NONE
projects              NONE
xprojects             NONE
usage_scaling         NONE
report_variables      NONE" > /tmp/$1.cfg

# add $1 as execution host using previous configuration
qconf -Ae /tmp/$1.cfg

# add $1 to hostgroup @allhosts
qconf -aattr hostgroup hostlist $1 @allhosts
qconf -aattr queue slots '['$1'='$2']' main.q
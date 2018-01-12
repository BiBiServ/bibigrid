#!/bin/bash


if [ ${0} = "bash" -o ${0} = "-bash" ] ; then
 alias bibigrid="java -jar `pwd`/`ls bibigrid-main/target/BiBiGrid-*.jar`"
else 
 java -jar `dirname ${0}`/`ls bibigrid-main/target/BiBiGrid-*.jar` $@
fi


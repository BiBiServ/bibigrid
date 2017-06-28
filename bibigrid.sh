#!/bin/bash


if [ ${0} = "bash" -o ${0} = "-bash" ] ; then
 alias bibigrid="java -jar `pwd`/`ls target/BiBiGrid-*.jar`"
else 
 java -jar `dirname ${0}`/`ls target/BiBiGrid-*.jar` $@
fi


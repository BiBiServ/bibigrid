#!/bin/bash
cd ..
mvn -P openstack  clean install -DskipTests
cd bibigrid-light-rest-4j/ || exit
mvn clean install -Prelease -DskipTests

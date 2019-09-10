#!/bin/bash
###
# Instantiate and run containers.
###
set -e
#
cwd=$(pwd)
#
VTAG="v0.0.1-SNAPSHOT"
#
###
# Tomcat
INAME="biocomp"
#
DOCKERPORT=9091
APPPORT=8080
#
sudo docker run -dit \
	--name "${INAME}_container" \
	-p ${DOCKERPORT}:${APPPORT} \
	${INAME}:${VTAG}
#
sudo docker container logs "${INAME}_container"
#
###
sudo docker container ls -a
#
printf "Tomcat Web Application Manager: http://localhost:%s/manager/html\n" "${DOCKERPORT}"
printf "BIOCOMP Web Application: http://localhost:%s/${INAME}\n" "${DOCKERPORT}"
#

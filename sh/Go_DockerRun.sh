#!/bin/bash
###
# Instantiate and run containers.
###
set -e
#
cwd=$(pwd)
#
VTAG="v0.9.0"
#
###
# Tomcat
INAME="biocomp"
#
DOCKERPORT=9095
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
printf "Tomcat Web Application Manager: http://localhost:${DOCKERPORT}/manager/html\n"
printf "BIOCOMP Web Application: http://localhost:${DOCKERPORT}/${INAME}\n"
#

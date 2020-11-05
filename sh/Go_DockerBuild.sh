#!/bin/sh
###
# Prerequisites:
#  unzip $HOME/archives/JSME_2013-10-13.zip -d biocomp_war/src/main/webapp
#  mv biocomp_war/src/main/webapp/JSME_2013-10-13 biocomp_war/src/main/webapp/jsme 
#  mvn clean install
###
#
set -e
#
cwd=$(pwd)
#
if [ $(whoami) != "root" ]; then
	echo "${0} should be run as root or via sudo."
	exit
fi
#
docker version
#
INAME="biocomp"
TAG="latest"
#TAG="v0.0.1"
#
T0=$(date +%s)
#
###
# Build image from Dockerfile.
dockerfile="${cwd}/Dockerfile"
docker build -f ${dockerfile} -t ${INAME}:${TAG} .
#
printf "Elapsed time: %ds\n" "$[$(date +%s) - ${T0}]"
#
docker images
#

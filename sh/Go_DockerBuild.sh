#!/bin/sh
###
cwd=$(pwd)
#
sudo docker version
#
INAME="biocomp"
TAG="v0.0.1-SNAPSHOT"
#
T0=$(date +%s)
#
###
# Build image from Dockerfile.
dockerfile="${cwd}/Dockerfile"
sudo docker build -f ${dockerfile} -t ${INAME}:${TAG} .
#
printf "Elapsed time: %ds\n" "$[$(date +%s) - ${T0}]"
#
sudo docker images
#

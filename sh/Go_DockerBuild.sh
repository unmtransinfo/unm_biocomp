#!/bin/sh
###
#
set -e
#
cwd=$(pwd)
#
sudo docker version
#
INAME="biocomp"
TAG="v0.0.1-SNAPSHOT"
#
T0=$(date +%s)
#
if [ ! -d "${cwd}/conf/chemaxon" ]; then
	mkdir -p "${cwd}/conf/chemaxon"
fi
#
if [ -f ~/.chemaxon/license.cxl ]; then
	cp ~/.chemaxon/license.cxl ${cwd}/conf/chemaxon
else
	echo "No ChemAxon license found at ~/.chemaxon/license.cxl"
fi
#
###
# Build image from Dockerfile.
dockerfile="${cwd}/Dockerfile"
sudo docker build -f ${dockerfile} -t ${INAME}:${TAG} .
#
rm ${cwd}/conf/chemaxon/license.cxl
#
printf "Elapsed time: %ds\n" "$[$(date +%s) - ${T0}]"
#
sudo docker images
#

#!/bin/bash
###
#
cwd=$(pwd)
#
T0=$(date +%s)
#
if [ ! -d "${cwd}/conf/chemaxon" ]; then
	mkdir -p "${cwd}/conf/chemaxon"
fi
#
if [ -f $HOME/.chemaxon/license.cxl ]; then
	cp $HOME/.chemaxon/license.cxl ${cwd}/conf/chemaxon
else
	echo "No ChemAxon license found at $HOME/.chemaxon/license.cxl"
fi
#
sudo docker version
#
INAME="biocomp"
TAG="latest"
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

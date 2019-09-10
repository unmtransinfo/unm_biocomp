#!/bin/sh
###
#
INAME="biocomp"
CNAME="${INAME}_container"
#
TAG=v0.0.1-SNAPSHOT
#
###
# Stop and clean up.
sudo docker stop ${CNAME}
sudo docker ps -a
sudo docker rm ${CNAME}
sudo docker rmi ${INAME}:${TAG}
#
IIDS=$(sudo docker images -f dangling=true \
	|sed -e '1d' \
	|awk -e '{print $3}')
for iid in $IIDS ; do
	sudo docker rmi ${iid}
done
#
#
sudo docker container ls -a
#

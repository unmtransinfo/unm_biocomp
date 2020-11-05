#!/bin/bash
###
# Instantiate and run containers.
# -dit = --detached --interactive --tty
###
cwd=$(pwd)
#
if [ $(whoami) != "root" ]; then
	echo "${0} should be run as root or via sudo."
	exit
fi
#
VTAG="latest"
#VTAG="v0.0.1"
#
###
INAME="biocomp"
#
DOCKERPORT=9080
APPPORT=8080
#
#IMG_UI="unmtransinfo/${INAME}:${VTAG}"
IMG_UI="${INAME}:${VTAG}"
docker run -dit --name "${INAME}_container" -p ${DOCKERPORT}:${APPPORT} ${IMG_UI}
#
###
# Install ChemAxon license.cxl
docker exec ${INAME}_container mkdir -p /usr/share/tomcat9/webapps/biocomp/.chemaxon
LICFILE="~tomcat/.chemaxon/license.cxl"
if [ -e "${LICFILE}" ]; then
	docker cp ${LICFILE} ${INAME}_container:/usr/share/tomcat9/webapps/biocomp/.chemaxon
	docker exec chown -R tomcat /usr/share/tomcat9/webapps/biocomp/.chemaxon
else
	printf "ERROR: ChemAxon license file not found: "${LICFILE}"\n"
	printf "ChemAxon license file must be installed manually.\n"
fi
#
docker container logs "${INAME}_container"
#
###
docker container ls -a
#
printf "Tomcat Web Application Manager: http://localhost:${DOCKERPORT}/manager/html\n"
printf "BIOCOMP Web Application: http://localhost:${DOCKERPORT}/${INAME}\n"
#

#!/bin/sh
#
if [ "`uname -s`" = "Darwin" ]; then
	APPDIR="/Users/app"
elif [ "`uname -s`" = "Linux" ]; then
	APPDIR="/home/app"
else
	APPDIR="/home/app"
fi
#
LIBDIR=$APPDIR/lib
CLASSPATH=$LIBDIR/unm_biocomp_db.jar
CLASSPATH=$CLASSPATH:$LIBDIR/unm_biocomp_util.jar
#
CLASSPATH=$CLASSPATH:$LIBDIR/mysql-connector-java.jar
#CLASSPATH=$CLASSPATH:$LIBDIR/mysql-connector-java-5.1.6.jar
#
#JAVA_OPTS="-Dderby.stream.error.field=System.err"
#JAVA_OPTS="-Dderby.stream.error.file=data/derby_utils.log"
#
java $JAVA_OPTS -classpath $CLASSPATH edu.unm.health.biocomp.db.mysql_utils $*
#

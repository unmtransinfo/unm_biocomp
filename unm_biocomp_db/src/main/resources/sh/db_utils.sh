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
CLASSPATH="$CLASSPATH:$LIBDIR/mysql-connector-java.jar"
CLASSPATH="$CLASSPATH:$LIBDIR/ojdbc5.jar"
CLASSPATH="$CLASSPATH:$LIBDIR/postgresql.jdbc3.jar"
CLASSPATH="$CLASSPATH:$LIBDIR/berkeleydb-3.3.75.jar"
CLASSPATH="$CLASSPATH:$LIBDIR/derby-10.10.1.1.jar"
CLASSPATH="$CLASSPATH:$LIBDIR/jtds-1.3.1.jar"
#
#
java $JAVA_OPTS -classpath $CLASSPATH edu.unm.health.biocomp.db.db_utils $*
#

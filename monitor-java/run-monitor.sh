#!/usr/bin/env bash
# set -x

##########################################################################################
# PARENT DIRECTORY
# code copied from Tomcat's `catalina.sh`
##########################################################################################
# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# LOAD ENVIRONMENT VARIABLES
if [ -r "$PRGDIR/setenv.sh" ]; then
  . "$PRGDIR/setenv.sh"
elif [ -r "$PRGDIR/../setenv.sh" ]; then
  . "$PRGDIR/../setenv.sh"
else
  . "$PRGDIR/../setenv.default.sh"
fi

$PRGDIR/../mvnw -DskipTests package


echo "##################"
echo "# START MONITOR #"
echo "##################"



java -classpath target/classes/ FrontendMonitor


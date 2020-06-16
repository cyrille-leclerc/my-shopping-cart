#!/usr/bin/env bash
set -x

export OPEN_TELEMETRY_AGENT_VERSION=0.3.0
export OPEN_TELEMETRY_EXPORTER_PROTOCOL="otlp"

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

$PRGDIR/../mvnw -DskipTests package exec:java


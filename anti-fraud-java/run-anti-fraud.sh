#!/usr/bin/env bash
set -x

export ELASTIC_AGENT_VERSION=1.19.0

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

##########################################################################################
# DOWNLOAD ELASTIC_AGENT AGENT
##########################################################################################
mkdir -p $PRGDIR/target/agent
cp  $PRGDIR/etc/elastic-agent/elasticapm.properties $PRGDIR/target/agent
$PRGDIR/../mvnw dependency:copy \
      -Dartifact=co.elastic.apm:elastic-apm-agent:$ELASTIC_AGENT_VERSION \
      -DoutputDirectory=$PRGDIR/target/agent/

$PRGDIR/../mvnw -DskipTests package

java -javaagent:target/agent/elastic-apm-agent-$ELASTIC_AGENT_VERSION.jar \
    -Dserver.port=8081 \
     -jar target/anti-fraud-1.0-SNAPSHOT.jar

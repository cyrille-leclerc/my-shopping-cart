#!/usr/bin/env bash
set -x

mvn package

export OPEN_TELEMETRY_AGENT_HOME=/opt/opentelemetry-java-agent
export OPEN_TELEMETRY_AGENT_VERSION=0.3.0
export OPEN_TELEMETRY_AGENT_JAR=$OPEN_TELEMETRY_AGENT_HOME/opentelemetry-auto-$OPEN_TELEMETRY_AGENT_VERSION.jar
# export OPEN_TELEMETRY_AGENT_EXPORTER_JAR=$OPEN_TELEMETRY_AGENT_HOME/opentelemetry-auto-exporters-logging-$OPEN_TELEMETRY_AGENT_VERSION.jar

# JAEGER
# export OPEN_TELEMETRY_AGENT_EXPORTER_JAR=$OPEN_TELEMETRY_AGENT_HOME/opentelemetry-auto-exporters-jaeger-$OPEN_TELEMETRY_AGENT_VERSION.jar
# export OTEL_RESOURCE_ATTRIBUTES=service.name=monitor,service.namespace=com-shoppingcart,service.version=1.0-SNAPSHOT

# OTEL COLLECTOR
export OPEN_TELEMETRY_AGENT_EXPORTER_JAR=$OPEN_TELEMETRY_AGENT_HOME/opentelemetry-auto-exporters-otlp-$OPEN_TELEMETRY_AGENT_VERSION.jar
export OTEL_RESOURCE_ATTRIBUTES=service.name=monitor,service.namespace=com-shoppingcart,service.version=1.0-otlp-SNAPSHOT

java -javaagent:$OPEN_TELEMETRY_AGENT_JAR \
     -Dota.exporter.jar=$OPEN_TELEMETRY_AGENT_EXPORTER_JAR \
     -Dota.exporter.otlp.endpoint=localhost:55680 \
     -Dota.exporter.jaeger.endpoint=localhost:14250 \
     -Dota.exporter.jaeger.service.name=monitor \
     -Dio.opentelemetry.auto.slf4j.simpleLogger.defaultLogLevel=info \
     -classpath target/classes/ Injector
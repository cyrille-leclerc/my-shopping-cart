FROM eclipse-temurin:21-jdk as builder

WORKDIR /usr/src/app/

COPY ./mvnw pom.xml ./
COPY ./.mvn ./.mvn
COPY ./shipping ./shipping
# remove all module lines except for shipping - keep lines that are not modules
RUN sed -i -r '/<module>shipping/! s/<module>.*<\/module>//' pom.xml
RUN --mount=type=cache,target=/root/.m2 ./mvnw install -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /usr/src/app/

COPY --from=builder /usr/src/app/shipping/target/shipping-1.1-SNAPSHOT.jar ./app.jar

ADD --chmod=644 https://github.com/DataDog/dd-trace-java/releases/latest/download/dd-java-agent.jar /usr/src/app/dd-java-agent.jar

# https://docs.datadoghq.com/tracing/troubleshooting/tracer_debug_logs/

EXPOSE 8080
ENTRYPOINT [ \
    "java", \
    "-javaagent:/usr/src/app/dd-java-agent.jar", \
#    "-Ddd.service=shipping", \
#    "-Ddd.env=dev", \
#    "-Ddd.version=1.1", \
#    "-Ddd.trace.agent.url=http://otel-collector:8126", \
    "-Ddd.trace.debug=true", \
    "-Ddatadog.slf4j.simpleLogger.defaultLogLevel=INFO", \
    "-Ddatadog.slf4j.simpleLogger.log.datadog.trace.agent.core.DDSpan=DEBUG", \
    "-Ddatadog.slf4j.simpleLogger.log.datadog.trace.agent.common.writer.RemoteWriter=DEBUG", \
    "-jar", "./app.jar" ]

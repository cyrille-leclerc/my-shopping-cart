FROM eclipse-temurin:21-jdk as builder

WORKDIR /usr/src/app/

# install node and npm
RUN apt-get update && apt-get install -y nodejs npm
RUN npm install copyfiles -g

COPY ./mvnw pom.xml ./
COPY ./.mvn ./.mvn
COPY ./frontend ./frontend

# remove all module lines except for frontend - keep lines that are not modules
RUN sed -i -r '/<module>frontend/! s/<module>.*<\/module>//' pom.xml
RUN --mount=type=cache,target=/root/.m2 ./mvnw install -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /usr/src/app/

COPY --from=builder /usr/src/app/frontend/target/frontend-1.1-SNAPSHOT.jar ./app.jar

ADD --chmod=644 https://github.com/DataDog/dd-trace-java/releases/latest/download/dd-java-agent.jar /usr/src/app/dd-java-agent.jar

# https://docs.datadoghq.com/tracing/troubleshooting/tracer_debug_logs/

EXPOSE 8080
ENTRYPOINT [ \
    "java", \
    "-javaagent:/usr/src/app/dd-java-agent.jar", \
    "-Ddd.trace.debug=true", \
    "-Ddatadog.slf4j.simpleLogger.defaultLogLevel=INFO", \
    "-Ddatadog.slf4j.simpleLogger.log.datadog.trace.agent.core.DDSpan=DEBUG", \
    "-Ddatadog.slf4j.simpleLogger.log.datadog.trace.agent.common.writer.RemoteWriter=DEBUG", \
    "-jar", "./app.jar" ]

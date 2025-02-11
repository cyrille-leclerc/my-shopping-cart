FROM eclipse-temurin:21-jdk AS builder

WORKDIR /usr/src/app/

COPY ./mvnw pom.xml ./
COPY ./.mvn ./.mvn
COPY ./warehouse ./warehouse
# remove all module lines except for warehouse - keep lines that are not modules
RUN sed -i -r '/<module>warehouse/! s/<module>.*<\/module>//' pom.xml
RUN --mount=type=cache,target=/root/.m2 ./mvnw install -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /usr/src/app/

COPY --from=builder /usr/src/app/warehouse/target/warehouse-1.1-SNAPSHOT.jar ./app.jar
# we ignore the version (which is from upstream) and use the latest version of the grafana distribution
ADD --chmod=644 https://github.com/grafana/grafana-opentelemetry-java/releases/latest/download/grafana-opentelemetry-java.jar /usr/src/app/opentelemetry-javaagent.jar
ENV JAVA_TOOL_OPTIONS=-javaagent:/usr/src/app/opentelemetry-javaagent.jar

EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "./app.jar" ]

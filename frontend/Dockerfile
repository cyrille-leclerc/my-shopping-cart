FROM eclipse-temurin:21-jdk AS builder

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
# we ignore the version (which is from upstream) and use the latest version of the grafana distribution
ADD --chmod=644 https://github.com/grafana/grafana-opentelemetry-java/releases/latest/download/grafana-opentelemetry-java.jar /usr/src/app/opentelemetry-javaagent.jar
ENV JAVA_TOOL_OPTIONS=-javaagent:/usr/src/app/opentelemetry-javaagent.jar

EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "./app.jar" ]

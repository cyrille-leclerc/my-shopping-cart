FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /usr/src/app/

# install node and npm
RUN apt-get update && apt-get install -y nodejs npm
RUN npm install copyfiles -g

COPY pom.xml ./
COPY ./src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /usr/src/app/

COPY --from=builder /usr/src/app/target/frontend-1.1-SNAPSHOT.jar ./app.jar

ADD --chmod=644 https://github.com/grafana/pyroscope-java/releases/latest/download/pyroscope.jar /usr/src/app/pyroscope.jar

EXPOSE 8080
ENTRYPOINT [ "java", "-javaagent:/usr/src/app/pyroscope.jar", "-jar", "./app.jar" ]

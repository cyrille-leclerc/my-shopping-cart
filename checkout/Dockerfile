FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /usr/src/app/

COPY pom.xml ./
COPY ./src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /usr/src/app/

COPY --from=builder /usr/src/app/target/checkout-1.1-SNAPSHOT.jar ./app.jar
# load dependencies
COPY --from=builder /usr/src/app/target/lib ./lib

EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "./app.jar" ]

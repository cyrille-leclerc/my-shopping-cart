FROM eclipse-temurin:21-jdk as builder

WORKDIR /usr/src/app/

COPY ./mvnw pom.xml ./
COPY ./.mvn ./.mvn
COPY ./checkout ./checkout
COPY ./checkout-protobuff ./checkout-protobuff
# remove all module lines except for checkout - keep lines that are not modules
RUN sed -i -r '/<module>checkout/! s/<module>.*<\/module>//' pom.xml
RUN --mount=type=cache,target=/root/.m2 ./mvnw install -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /usr/src/app/

COPY --from=builder /usr/src/app/checkout/target/checkout-1.0-SNAPSHOT.jar ./app.jar
COPY --from=builder /usr/src/app/checkout/target/lib ./lib
# we ignore the version (which is from upstream) and use the latest version of the grafana distribution
ADD --chmod=644 https://github.com/grafana/grafana-opentelemetry-java/releases/latest/download/grafana-opentelemetry-java.jar /usr/src/app/opentelemetry-javaagent.jar
ENV JAVA_TOOL_OPTIONS=-javaagent:/usr/src/app/opentelemetry-javaagent.jar

EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "./app.jar" ]

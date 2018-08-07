FROM maven:3-jdk-10-slim as build

COPY . /build/
WORKDIR /build
RUN mvn package -q

FROM openjdk:10-slim
COPY --from=build /build/target/*.jar /app/
COPY --from=build /build/config/LogSettings.xml /app/
COPY --from=build /build/config/service.properties.docker /app/service.properties
COPY --from=build /build/target/lib /app/lib

WORKDIR /app
EXPOSE 80
CMD sh -c "java -Dlog4j.configurationFile=LogSettings.xml -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -jar *.jar --env"

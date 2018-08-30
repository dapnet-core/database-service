FROM maven:3-jdk-10-slim as build

COPY . /build/
WORKDIR /build
RUN mvn package -q

FROM openjdk:10-slim
COPY --from=build /build/target/*.jar /app/

WORKDIR /app
EXPOSE 80
CMD sh -c "java -jar *.jar --spring.profiles.active=docker"

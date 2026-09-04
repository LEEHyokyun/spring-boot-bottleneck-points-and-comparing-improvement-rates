## BUILD

FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY . .
RUN ./gradlew clean bootJar

## DEPLOY

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/build/libs/spring-boot-bottleneck-points-and-comparing-improvement-rates.jar spring-boot-bottleneck-points-and-comparing-improvement-rates.jar

VOLUME /tmp

ENTRYPOINT ["java","-jar","spring-boot-bottleneck-points-and-comparing-improvement-rates.jar"]
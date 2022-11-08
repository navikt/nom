FROM eclipse-temurin:17-jre as builder
ARG JAR_PATH

WORKDIR application
COPY ${JAR_PATH} app.jar

RUN java -Djarmode=layertools -jar app.jar extract

FROM ghcr.io/navikt/baseimages/temurin:17
LABEL maintainer="Team Org"

COPY /docker_run-java.sh /run-java.sh

COPY --from=builder --chown=apprunner:apprunner application/dependencies/ ./
COPY --from=builder --chown=apprunner:apprunner application/spring-boot-loader/ ./
COPY --from=builder --chown=apprunner:apprunner application/snapshot-dependencies/ ./
COPY --from=builder --chown=apprunner:apprunner application/application/ ./
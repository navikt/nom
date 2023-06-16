FROM eclipse-temurin:17-jre as builder
ARG JAR_PATH

WORKDIR application
COPY ${JAR_PATH} app.jar

RUN java -Djarmode=layertools -jar app.jar extract

FROM ghcr.io/navikt/baseimages/temurin:17
LABEL maintainer="Team Org"

# Temporary until temurin base image include updated run-script
USER root
COPY /docker_run-java.sh /run-java.sh
RUN chown apprunner:root /run-java.sh && \
    chmod +x /run-java.sh && \
    chmod 0755 /run-java.sh

RUN mkdir /secure-logs
RUN chown -R apprunner /secure-logs

USER apprunner

COPY --from=builder --chown=apprunner:apprunner application/dependencies/ ./
COPY --from=builder --chown=apprunner:apprunner application/spring-boot-loader/ ./
COPY --from=builder --chown=apprunner:apprunner application/snapshot-dependencies/ ./
COPY --from=builder --chown=apprunner:apprunner application/application/ ./
FROM navikt/java:14-appdynamics
LABEL maintainer="Team Registre"

ENV APPD_ENABLED=true
COPY appdynamics.sh /init-scripts/

ARG JAR_PATH

ADD $JAR_PATH /app/app.jar

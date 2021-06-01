FROM navikt/java:16
LABEL maintainer="Team Registre"

ARG JAR_PATH

ADD $JAR_PATH /app/app.jar

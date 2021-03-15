FROM navikt/java:15
LABEL maintainer="Team Registre"

ARG JAR_PATH

ADD $JAR_PATH /app/app.jar

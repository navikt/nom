FROM navikt/java:17
LABEL maintainer="Team Org"

ARG JAR_PATH

ADD $JAR_PATH /app/app.jar

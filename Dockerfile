FROM navikt/java:11
LABEL maintainer="Team Registre"

ARG SCRIPT_PATH
ARG JAR_PATH

#COPY $SCRIPT_PATH /init-scripts/init.sh
ADD $JAR_PATH /app/app.jar

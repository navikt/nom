#!/usr/bin/env bash

if [ -z ${APPDYNAMICS+x} ]; then
    JAVA_OPTS="${JAVA_OPTS} -javaagent:/opt/appdynamics/javaagent.jar "
    JAVA_OPTS="${JAVA_OPTS} -Dappdynamics.agent.applicationName=${NAIS_APP_NAME}_${NAIS_NAMESPACE}"
    JAVA_OPTS="${JAVA_OPTS} -Dappdynamics.agent.tierName=${NAIS_NAMESPACE}_${NAIS_APP_NAME}"
    JAVA_OPTS="${JAVA_OPTS} -Dappdynamics.agent.reuse.nodeName=true"
    JAVA_OPTS="${JAVA_OPTS} -Dappdynamics.agent.reuse.nodeName.prefix=${NAIS_NAMESPACE}_${NAIS_APP_NAME}_"
    JAVA_OPTS="${JAVA_OPTS} -Dappdynamics.jvm.shutdown.mark.node.as.historical=true"
    export JAVA_OPTS
fi
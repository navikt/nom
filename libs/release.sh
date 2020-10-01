#!/bin/bash
set -e

cd libs/contract-nom-dto
mvn -B --settings ../maven-settings.xml deploy -Dmaven.wagon.http.pool=false --also-make


#!/bin/sh

echo "Building service images ..."

cd pstatus-graphql-ktor && ./gradlew jibDockerBuild
cd ../pstatus-report-sink-ktor && ./gradlew jibDockerBuild
cd ../pstatus-notifications-workflow-ktor && ./gradlew jibDockerBuild
cd ../pstatus-notifications-rules-engine-ktor && ./gradlew jibDockerBuild


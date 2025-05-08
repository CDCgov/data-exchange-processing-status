#!/bin/sh

echo "Building images ..."

cd pstatus-graphql-ktor && ./gradlew jibDockerBuild
cd ../pstatus-report-sink-ktor && ./gradlew jibDockerBuild
cd ../pstatus-notifications-workflow-ktor && ./gradlew jibDockerBuild
cd ../pstatus-notifications-rules-engine-ktor && ./gradlew jibDockerBuild
cd ../

echo "Starting services ..."
docker compose -f docker-compose.local.yml --env-file mock-email.env up -d
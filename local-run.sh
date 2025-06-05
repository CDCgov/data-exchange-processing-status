#!/bin/sh

echo "Building images ..."

cd pstatus-graphql-ktor && ./gradlew jibDockerBuild -q
cd ../pstatus-report-sink-ktor && ./gradlew jibDockerBuild -q
cd ../pstatus-notifications-workflow-ktor && ./gradlew jibDockerBuild -q
cd ../pstatus-notifications-rules-engine-ktor && ./gradlew jibDockerBuild -q
cd ../

echo "Starting services ..."
docker compose -f docker-compose.local.yml --env-file mock-email.env up -d --quiet-pull
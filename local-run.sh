#!/bin/sh

# This script builds the Docker images for the PS API services with gradle jib and 
# starts them using docker compose. It is primmarily used for the GitHub CI workflow 
# but can also be used for local development.

echo "Building images ..."

cd pstatus-graphql-ktor && ./gradlew jibDockerBuild -q
cd ../pstatus-report-sink-ktor && ./gradlew jibDockerBuild -q
cd ../pstatus-notifications-workflow-ktor && ./gradlew jibDockerBuild -q
cd ../pstatus-notifications-rules-engine-ktor && ./gradlew jibDockerBuild -q
cd ../

echo "Starting services ..."
docker compose -f docker-compose.ci.yml --env-file mock-email.env up -d --quiet-pull
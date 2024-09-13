#!/bin/bash

# Define variables
KEYCLOAK_URL="http://localhost:8080"
REALM="test-realm-a"
CLIENT_ID="test-client-a"
CLIENT_SECRET="ApcvPgFU0l1ouysX4nZ8v954Af06XSL2"
USERNAME="test-user-a"
PASSWORD="TestUserA"

# Execute curl request to get the token
curl --location --request POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "client_id=$CLIENT_ID" \
--data-urlencode "client_secret=$CLIENT_SECRET" \
--data-urlencode 'grant_type=password' \
--data-urlencode "username=$USERNAME" \
--data-urlencode "password=$PASSWORD"

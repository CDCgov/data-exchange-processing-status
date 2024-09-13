#!/bin/bash

# Define variables
KEYCLOAK_URL="http://localhost:8080"
REALM="test-realm-jwt"
CLIENT_ID="test-client-jwt"
CLIENT_SECRET="JZ9eteOtRvrxQo8j97cvILzwPMwq28bp"
USERNAME="test-user-jwt"
PASSWORD="TestUser"

# Execute curl request to get the token
curl --location --request POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "client_id=$CLIENT_ID" \
--data-urlencode "client_secret=$CLIENT_SECRET" \
--data-urlencode 'grant_type=password' \
--data-urlencode "username=$USERNAME" \
--data-urlencode "password=$PASSWORD"

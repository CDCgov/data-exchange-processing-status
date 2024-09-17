#!/bin/bash

# Define variables
KEYCLOAK_URL="http://localhost:8080"
REALM="test-realm-jwt"
CLIENT_ID="test-client-jwt"
CLIENT_SECRET="AvkgJNTDhfQECsnnCZp9uAmt8lbZOhx1"
USERNAME="test-user"
PASSWORD="test"

# Execute curl request to get the token
curl --location --request POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "client_id=$CLIENT_ID" \
--data-urlencode "client_secret=$CLIENT_SECRET" \
--data-urlencode 'grant_type=password' \
--data-urlencode "scope=email profile" \
--data-urlencode "username=$USERNAME" \
--data-urlencode "password=$PASSWORD"

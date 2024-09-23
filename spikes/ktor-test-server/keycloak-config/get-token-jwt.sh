#!/bin/bash

KEYCLOAK_URL="http://localhost:9080"
REALM="test-realm-jwt"
CLIENT_ID="test-client-jwt"
CLIENT_SECRET="AvkgJNTDhfQECsnnCZp9uAmt8lbZOhx1"
USERNAME="test-user"
PASSWORD="test"

# KEYCLOAK_URL="http://localhost:9080"
# REALM="test-realm-alternate"
# CLIENT_ID="test-client-jwt"
# CLIENT_SECRET="N1MGAKDaSoRsA0tyJEC6LGgb9UbLIKGn"
# USERNAME="test-user"
# PASSWORD="test"

# Execute curl request to get the token
curl --location --request POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode "client_id=$CLIENT_ID" \
--data-urlencode "client_secret=$CLIENT_SECRET" \
--data-urlencode 'grant_type=password' \
--data-urlencode "scope=email profile" \
--data-urlencode "username=$USERNAME" \
--data-urlencode "password=$PASSWORD"

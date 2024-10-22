#!/usr/bin/bash

AZURITE_KEY_DEFAULT="Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==" # Default key used by Azurite

# Wait for azurite startup
until curl -s http://azurite:10000/ > /dev/null; do
    echo "Waiting for Azurite to be ready..."
    sleep 5
done

# Create a blob container 'test-container'
az storage container create \
    --name test-container \
    --account-name devstoreaccount1 \
    --account-key ${AZURITE_KEY_DEFAULT} \
    --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=${AZURITE_KEY_DEFAULT};BlobEndpoint=http://azurite:10000/devstoreaccount1;"

echo "Blob container 'test-container' created in Azurite."


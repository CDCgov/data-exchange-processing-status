# Configuring and Testing the Event Reader Sink with Azurite and RabbitMQ

These steps help to configure and test locally using Docker Compose to simulate Azure Blob Storage with **Azurite** and an AMQP message broker with **RabbitMQ**.

## Prerequisites
- Docker and Docker Compose installed.
- Azure CLI installed (for managing Azurite).
- CURL command installed.
- Ensure you have a `.env` file in the root of the project.

## 1. Add Azure and RabbitMQ Configurations to the `.env` File
In your `.env` file, set the needed environment variables for both Azure Blob Storage and RabbitMQ:

```bash
# General settings
export CLOUD_PROVIDER="azure"

# Azure Blob settings
export AZURE_BLOB_STORAGE_ACCOUNT_NAME="devstoreaccount1"
export AZURE_BLOB_STORAGE_ACCOUNT_KEY="Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==" # Default Azurite Key
export AZURE_BLOB_CONTAINER_NAME="test-container"
export AZURE_BLOB_ENDPOINT="http://localhost:10000/devstoreaccount1"

# Azure Service Bus settings (for RabbitMQ)
export SERVICE_BUS_NAMESPACE="localhost"
export SERVICE_BUS_CONNECTION_STRING="amqp://localhost:5672"
export SERVICE_BUS_SHARED_ACCESS_KEY_NAME="user"
export SERVICE_BUS_SHARED_ACCESS_KEY="password"
export SERVICE_BUS_TOPIC_NAME="test-queue"
export SERVICE_BUS_SUBSCRIPTION_NAME=""  # Optional, can be ignored empty for RabbitMQ
```

## 2. Start Azurite and RabbitMQ with Docker Compose

Start the Azurite and RabbitMQ Docker containers by running:

```bash
docker-compose -f docker-compose.rabbitmq.yml -f docker-compose.azurite.yml up
```

- **Azurite**: Simulates Azure Blob Storage locally.
  - The `docker-compose/azurite-bootstrap.sh` script is used to automatically create a `test-container` when Azurite starts.

- **RabbitMQ**: Message broker for handling AMQP messages.
  - The `docker-compose/rabbitmq-definitions.json` file is used to automatically create a queue named `test-queue` when RabbitMQ starts.

## 3. Run the Application
To start the event reader sink application after configuring Azurite and RabbitMQ:

```bash
# Load environment variables
source ./.env

# Run the application (using Gradle as an example)
./gradlew run
```

## 4. Send a Test Message to RabbitMQ using CURL
To test the RabbitMQ integration, send a message to the queue using CURL.

```bash
curl -u user:password -H "content-type:application/json" -X POST -d '{
  "properties": {},
  "routing_key": "test-queue",
  "payload": "This is a test message",
  "payload_encoding": "string"
}' http://localhost:15672/api/exchanges/%2F/amq.default/publish
```


## 5. Verify Blob Storage
To verify that messages have been processed and stored in Azure Blob Storage, list the contents of the blob container:

```bash
ACCOUNT_KEY_DEFAULT="Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw=="
az storage blob list \
    --container-name test-container \
    --account-name devstoreaccount1 \
    --account-key ${ACCOUNT_KEY_DEFAULT} \
    --connection-string "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=${ACCOUNT_KEY_DEFAULT};BlobEndpoint=http://localhost:10000/devstoreaccount1;" \
    --query "[].name"
```

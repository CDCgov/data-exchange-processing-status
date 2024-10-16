# Configuring and Testing the Event Reader Sink with LocalStack

These steps help to configure and test locally using LocalStack to simulate AWS SQS and S3 services.

## Prerequisites
- Docker and Docker Compose installed.
- AWS CLI installed.
- Ensure you have a `.env` file in the root of the project.

## 1. Create a LocalStack AWS Profile
To simulate AWS credentials locally, create a LocalStack profile with dummy values:

```bash
aws configure --profile localstack
```

When prompted, enter the following:
```
AWS Access Key ID [None]: dummyAccessKey
AWS Secret Access Key [None]: dummySecretKey
Default region name [None]: us-east-1
Default output format [None]: json
```

## 2. Add AWS Configurations to the `.env` File

In your `.env` file, set the needed environment variables for both SQS and S3:

```bash
# General settings
export CLOUD_PROVIDER="aws"
export AWS_S3_REGION="us-east-1"

# SQS settings
export AWS_QUEUE_NAME="test-queue"
export AWS_QUEUE_URL="http://localhost:4566/000000000000/test-queue"
export AWS_SQS_REGION="us-east-1"

# S3 settings
export AWS_S3_ENDPOINT="http://localhost:4566"
export AWS_S3_BUCKET_NAME="test-bucket"
export AWS_ACCESS_KEY_ID="dummyAccessKey"
export AWS_SECRET_ACCESS_KEY="dummyAccessKey"
```

## 3. Start LocalStack with Docker Compose

Start the LocalStack Docker container by running:

```bash
docker-compose -f docker-compose.localstack.yml up
```

## 4. Automatic Resource Creation

LocalStack will automatically create the SQS queue and S3 bucket when it starts, as specified in the `localstack-bootstrap.sh` script within the Docker Compose setup.

- SQS Queue Name: `test-queue`
- S3 Bucket Name: `test-bucket`

## 5. Run the Application

To start the event reader sink application after configuring LocalStack:

```bash
# Load environment variables
source ./.env

# Run the application (using Gradle as an example)
./gradlew run
```

## 6. Send a Test Message to the Queue

To test the SQS integration, send a message to the queue:

```bash
aws --profile localstack --endpoint-url=http://localhost:4566 sqs send-message \
  --queue-url http://localhost:4566/000000000000/test-queue \
  --message-body "Hello, this is a test message!"
```

## 7. Check Files in S3

To verify the S3 bucket is working, list the contents:

```bash
aws --profile localstack --endpoint-url=http://localhost:4566 s3 ls s3://test-bucket/
```

---

## Additional LocalStack Commands

### Working with SQS

#### List Queues

```bash
aws --profile localstack --endpoint-url=http://localhost:4566 sqs list-queues
```

#### Send Another Test Message

```bash
aws --profile localstack --endpoint-url=http://localhost:4566 sqs send-message \
  --queue-url http://localhost:4566/000000000000/test-queue \
  --message-body "Another test message"
```

#### Get Queue Attributes

```bash
aws --profile localstack --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/test-queue \
  --attribute-names ApproximateNumberOfMessages
```

#### Receive a Message

```bash
aws --profile localstack --endpoint-url=http://localhost:4566 sqs receive-message \
  --queue-url http://localhost:4566/000000000000/test-queue
```

#### Delete a Message

To delete a message, use the `ReceiptHandle` from the received message:

```bash
aws --profile localstack --endpoint-url=http://localhost:4566 sqs delete-message \
  --queue-url http://localhost:4566/000000000000/test-queue \
  --receipt-handle <YourReceiptHandle>
```

### Working with S3

#### Upload a File to S3

```bash
aws --profile localstack --endpoint-url=http://localhost:4566 s3 cp ./localstack-bootstrap.sh s3://test-bucket/file.txt
```

#### List Bucket Contents

```bash
aws --profile localstack --endpoint-url=http://localhost:4566 s3 ls s3://test-bucket/
```

#### Download a File from S3

```bash
aws --profile localstack --endpoint-url=http://localhost:4566 s3 cp s3://test-bucket/file.txt ./local-file.txt
```

---

## Notes
- **Automatic Resource Creation:** You don't need to manually create the SQS queue or S3 bucket; they are set up by the `localstack-bootstrap.sh` script when LocalStack starts.
- **Dummy Credentials:** LocalStack does not require valid AWS credentials, so dummy values are used for local development.

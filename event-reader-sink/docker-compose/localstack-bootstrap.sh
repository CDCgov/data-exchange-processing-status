#!/usr/bin/bash

# Wait for localstack startup
until curl -s http://localhost:4566/ > /dev/null; do
    echo "Waiting for LocalStack to be ready..."
    sleep 5
done

# Create an SQS queue
awslocal sqs create-queue --queue-name test-queue --region us-east-1

# Create an S3 bucket
awslocal s3api create-bucket --bucket test-bucket --region us-east-1

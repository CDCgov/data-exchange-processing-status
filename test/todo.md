# Processing Status API Testing TODO list
This file outlines some of the known test coverage gaps and functionality that needs to be tested with thoughts and recommendations on moving forward to improve test coverage.

## RabbitMQ Integration Test for Report-Sink
### Overview
The report-sink component currently lacks integration test coverage for RabbitMQ message processing functionality. While Playwright tests cover GraphQL endpoints extensively, we need integration tests that can send messages to RabbitMQ to ensure the report-sink can properly consume, validate, and persist reports from message queues.

### Required Integration Test Scenarios
#### Basic Message Processing
- **Objective**: Verify report-sink can consume valid reports from RabbitMQ
- **Test Steps**:
  - Start RabbitMQ server (using existing docker-compose setup)
  - Send valid report message to `PSAPIQueue` via `PSAPIExchange`
  - Verify report-sink processes message and persists to database
  - Validate report appears in GraphQL queries

#### Message Validation and Dead Letter Handling
- **Objective**: Test report-sink validation logic and dead letter queue behavior
- **Test Steps**:
  - Send malformed/invalid report messages
  - Verify messages are rejected and sent to dead letter queue
  - Validate dead letter messages are persisted to dead letter container
  - Check error logging and monitoring

#### Message Schema Validation
- **Objective**: Ensure report-sink validates messages against expected schemas
- **Test Steps**:
  - Send reports with missing required fields
  - Send reports with invalid field types/values
  - Verify schema validation errors are properly handled
  - Test with different report types (upload-started, upload-completed, etc.)

#### Volume and Performance
- **Objective**: Test report-sink under load conditions
- **Test Steps**:
  - Send multiple reports in rapid succession
  - Monitor processing performance and resource usage
  - Verify no message loss under load
  - Test concurrent message processing

### Success Criteria
- All valid messages are processed and persisted correctly
- Invalid messages are properly rejected and sent to dead letter queue
- No message loss during normal operation or connection issues
- Performance meets expected throughput requirements
- Error handling and logging work as expected

### Implementation Requirements
#### Test Infrastructure
- **RabbitMQ Setup**: Use existing `docker-compose.yml` with RabbitMQ definitions
- **Database**: Use test database (Couchbase/CosmosDB) for persistence validation
- **Test Framework**: Multiple options available for RabbitMQ integration testing

#### Test Framework Options
##### Option 1: Playwright (Current Framework)
- **Pros**: Already established in project, good GraphQL integration, familiar to team
- **Cons**: Primarily designed for web testing, limited RabbitMQ client libraries
- **Implementation**: Extend existing Playwright setup with RabbitMQ client libraries
- **Best For**: Teams wanting to maintain consistency with existing test framework

##### Option 2: Jest + amqplib
- **Pros**: Native Node.js testing, excellent RabbitMQ support via amqplib, fast execution
- **Cons**: Separate from existing Playwright tests, requires additional setup
- **Implementation**: 
  ```javascript
  const amqp = require('amqplib');
  const { sendReportMessage, verifyMessageProcessed } = require('./rabbitmq-helper');
  ```
- **Best For**: Teams preferring Node.js ecosystem and direct RabbitMQ integration

##### Option 3: Python + pytest + pika
- **Pros**: Excellent RabbitMQ support via pika, rich testing ecosystem, easy data manipulation
- **Cons**: Different language from main application (Kotlin/Java)
- **Implementation**:
  ```python
  import pika
  import pytest
  from rabbitmq_helper import RabbitMQTestHelper
  ```
- **Best For**: Teams with Python expertise or needing complex data transformations

##### Option 4: Kotlin + JUnit + RabbitMQ Java Client
- **Pros**: Same language as report-sink service, native integration, type safety
- **Cons**: More complex setup, separate from existing Playwright tests
- **Implementation**:
  ```kotlin
  import com.rabbitmq.client.ConnectionFactory
  import org.junit.jupiter.api.Test
  ```
- **Best For**: Teams wanting language consistency and deep integration with service code

##### Option 5: Go + testify + amqp091-go
- **Pros**: Excellent performance, simple setup, good RabbitMQ support
- **Cons**: Different language ecosystem, separate test infrastructure
- **Implementation**:
  ```go
  import (
      "github.com/streadway/amqp"
      "github.com/stretchr/testify/assert"
  )
  ```
- **Best For**: Teams with Go expertise or needing high-performance test execution



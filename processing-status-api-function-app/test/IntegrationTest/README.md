# Integration Test For Processing Status API

## Overview
The Integration Test exists to:

- **Create Reports :** Simulates the process of creating reports sent by the Upload, Routing and HL7V2 Validation services to Processing Status API.
- **Azure Service Bus:** Automates the process of sending the reports to Processing Status API through Service Bus.
- **Processing StatusAPI:** Automates the process of sending the reports directly to Processing Status API.
- **Querying CosmosDB:** Automates the process of querying the CosmosDB by the reportId.

## How It works

1. **The 'ReportFactory' class**: It employs the Abstract Factory design pattern to simulate creation of reports sent from Upload, Routing and HL7v2 services to Processing Status API.This component is still being developed and will undergo changes.
2. **Reports Sent via Azure Service Bus**: Reports are sent to the Processing Status API through the service bus.
3. **Reports Sent Directly to the API**: Reports are sent directly to the Processing Status API through HTTP endpoints.
4. **CosmoDB Querying**: Utilizing Cosmos Client library from lib-dex-commons to query the  container for each report. To validate the correctness of report processing and persistence to Cosmos DB.

The integration test suite for the Processing Status API use two primary methods of sending report:
![SendingReportsThroughServiceBus](https://github.com/CDCgov/data-exchange-processing-status/assets/137535421/7da1c249-91e3-4734-821b-6490cbac9173)

![SendingReportsToProcessingStatusAPI](https://github.com/CDCgov/data-exchange-processing-status/assets/137535421/bc2776af-551b-44c2-8978-3999e680a039)

## Required Configuration Parameters
- **COSMOS_DB_KEY**: Access key of the Cosmos DB instance.
- **COSMOS_DB_ENDPOINT**: The URL endpoint of your Cosmos DB instance.
- **COSMOS_DB_PARTITION_KEY**: The path of the partition key.
- **COSMOS_DB_CONNECTION_STRING**: The connection key for Cosmos DB.
- **COSMOS_DB_CONTAINER_NAME**: The name of the Cosmos DB container.
- **PROCESSING_STATUS_API_BASE_URL**: The Processing Status API base URL.
- **SERVICE_BUS_CONNECTION_STRING**: The Service Bus connection string.
- **SERVICE_BUS_FULLY_QUALIFIED_NAMESPACE**: The fully qualified name space for service bus.
- **SERVICE_BUS_QUEUE_NAME**: The queue name for service bus.

## Usage
To build and run the Integration test, follow these steps:
1. Clone this repository to your local machine:
```
git clone https://github.com/CDCgov/data-exchange-processing-status.git
configure environment variables
cd test/IntegrationTest
mvn clean install
mvn integration-test
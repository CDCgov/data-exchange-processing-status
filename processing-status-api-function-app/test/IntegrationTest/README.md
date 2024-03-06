# Integration Test For Processing Status API

## Overview
The Integration Test exists to:

- **Create Reports :** Simulates the process of creating reports sent by the Upload, Routing and HL7V2 Validation services to Processing Status API.

- **Azure Service Bus:** Automates the process of sending the reports to Processing Status API through Service Bus.

- **Processing StatusAPI:** Automates the process of sending the reports directly to Processing Status API.

- **Querying CosmosDB:** Automates the process of querying the CosmosDB by the reportId.
- 
## How It works

1. **The 'ReportFactory' class**: It employs the Abstract Factory design pattern to simulate creation of reports sent from Upload, Routing and HL7v2 services to Processing Status API.This component is still being developed and will undergo changes.
2. **Reports Submitted via Azure Service Bus**: Reports are sent to the Processing Status API through the service bus.
3. **Reports Submitted Directly to the API**: Reports are submitted directly to the Processing Status API through HTTP endpoints.
4. **CosmoDB Querying**: Utilizing Cosmos Client library from lib-dex-commons to query the  container for each report. To validate the correctness of report processing and persistence to Cosmos DB.

The integration test suite for the Processing Status API use two primary methods of sending report:
![Diagrams-private - Sending Reports to Processing Status .png](..%2F..%2F..%2F..%2F..%2FDownloads%2FDiagrams-private%20-%20Sending%20Reports%20to%20Processing%20Status%20.png)
![Diagrams-private - SendingReportWithServiceBus.png](..%2F..%2F..%2F..%2F..%2FDownloads%2FDiagrams-private%20-%20SendingReportWithServiceBus.png)


## Required Configuration Parameters
- **COSMOS_DB_KEY**: Access key of the Cosmos DB instance.
- **COSMOS_DB_ENDPOINT**: The URL endpoint of your Cosmos DB instance.
- **COSMOS_DB_PARTITION_KEY**: The path of the partition key.
- **COSMOS_DB_CONNECTION_STRING**: The connection key for Cosmos DB.
- **COSMOS_DB_CONTAINER_NAME**: The name of the Cosmos DB container.
- **PROCESSING_STATUS_API_BASE_URL**: The Processing Status API base URL.
- **SERVICE_BUS_CONNECTION_STRING**: The Service Bus connection string.

## Usage
To build and run the Integration test, follow these steps:
1. Clone this repository to your local machine:
```
git clone https://github.com/CDCgov/data-exchange-processing-status.git
cd 
mvn clean install
mvn integration-test
# GraphQL use in Processing Status API

## Overview

### Test platforms

#### Data API builder

Run test using Data API builder.   Move more sophisticated queries into stored procedures accessible as attributes in GraphQL.  Use policies to control authZ.

#### GraphQL Kotlin

#### Apollo Server

## Data API Builder

### Configuration generation commands
Step 1:
```bash
dab init --database-type cosmosdb_nosql --graphql-schema schema.gql --cosmosdb_nosql-database ProcessingStatus --connection-string "{{place here}}"
--host-mode "Development"
```

Step 2:
```bash
dab add Report --source Reports --permissions "anonymous:*"
```

Step 3:
Add the stored procedure to the model.
```bash
dab add GetMetadataReports --source dbo.test2 --source.type "stored-procedure" source.params "searchType:s" --permissions "anonymous:execute" --rest.methods "get" --graphql.operation "query"

dab update GetMetadataReports --graphql.operation "mutation"
```

Step 4:
Start the Data API builder
```bash
dab start
```

### Testing with Postman
**POST** `https://localhost:5001/graphql`.
Change request body type to GraphQL and paste the following:
```graphql
{
    reports(first: 5) {
        items {
            id
            uploadId
            reportId
            dataStreamId
            dataStreamRoute
            stageName
            timestamp
            content {
                schema_name
                schema_version
            }
        }
    }
}
```

### GraphQL Schema
**schema.gql**:
```graphql
type Report @model {
  id: ID
  uploadId: String
  reportId: String
  dataStreamId: String
  dataStreamRoute: String
  stageName: String
  timestamp: Long
  contentType: String
  content: JsonContent
}
 
type JsonContent {
  schema_name: String
  schema_version: String
}

type GetMetadataReports {
  counter: Int
}
```

### Deployment of Data API Builder
See [Data API Builder on Azure Container Instances](https://www.linkedin.com/pulse/data-api-builder-azur-container-instances-chand-abdul-salam/).  In particular, see the section, *Run Data API builder in Azure Container Instances*.

Also see https://learn.microsoft.com/en-us/azure/data-api-builder/how-to-run-container
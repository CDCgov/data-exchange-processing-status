# GraphQL and the Processing Status API

## Overview
*"GraphQL is a query language for APIs and a runtime for fulfilling those queries with your existing data. GraphQL provides a complete and understandable description of the data in your API, gives clients the power to ask for exactly what they need and nothing more, makes it easier to evolve APIs over time, and enables powerful developer tools."* -[graphql.org](https://graphql.org/)

There are a considerable number of queries that are needed for the PS API.  Each user of PS API has unqiue queries they'd like to have.  Currently, the PS API provides traditional HTTP endpoints.  As the number of users of PS API, so too will the number of unique queries needed to fulfill the needs of those users.  Rather than continue to add new queries to PS API as needs arise, GraphQL would allow users to create their own queries.

There are at least two approaches that can be used to implement GraphQL for the Processing Status API.  **Approach 1** is to have an independent and dedicated GraphQL server that interacts directly with the data source, namely CosmosDB.  **Approach 2** is to use a GraphQL library that is embedded in the PS API.

![PS API GraphQL Approaches](./resources/PSAPI_GraphQL_Approaches.png)

The advantages of each approach is as follows:

| Independent GraphQL Server (Approach 1) | Embedded GraphQL Server (Approach 2) |
| --------------------------------------- | ------------------------------------ |
| 1. Mature rich products available       | 1. Fewer resources to manage         |
| 2. Decouples queries from ingestion     | 2. Shared resources; e.g. security management |

### GraphQL Requirements
- Must be able to provide fine access control through JWT tokens passed in the HTTP header.
- The GraphQL implementation must be able to expose stored procedures, not just tables and views.
- GraphQL queries must be performant and introduce little to no additional latency when compared to HTTP queries through the existing REST PS API.

## Test Platforms
One or more of each of the two approaches will be examined.

### Data API Builder
The [Data API Builder (DAB)](https://learn.microsoft.com/en-us/azure/data-api-builder/overview) is a Mirosoft product.  The Data API builder "generates modern REST and GraphQL endpoints for your Azure Databases. Use Data API builder to securely expose API endpoints so that your data can be accessed using modern techniques from any platform, developer language, or device."

The Data API Builder is a fairly new product and is only on release 0.10.23 at the time of this writing.  The github page for it can be found [here](https://github.com/Azure/data-api-builder).

#### Configuration generation commands
**Step 1**: Initialize the DAB configuration file.
```bash
dab init --database-type cosmosdb_nosql --graphql-schema schema.gql --cosmosdb_nosql-database ProcessingStatus --connection-string "{{place here}}"
--host-mode "Development"
```

**Step 2**: Add the Reports container to the config.
```bash
dab add Report --source Reports --permissions "anonymous:*"
```

**Step 3**: Attempt to add a stored procedure.
```bash
dab add GetMetadataReports --source dbo.test2 --source.type "stored-procedure" source.params "searchType:s" --permissions "anonymous:execute" --rest.methods "get" --graphql.operation "query"
```

**Step 4**: Start the Data API builder.
```bash
dab start
```

#### Testing with Postman
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

#### GraphQL Schema
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

#### Deployment of Data API Builder
See [Data API Builder on Azure Container Instances](https://www.linkedin.com/pulse/data-api-builder-azur-container-instances-chand-abdul-salam/).  In particular, see the section, *Run Data API builder in Azure Container Instances*.  Also see this article, [how to run DAB in an ACI](https://learn.microsoft.com/en-us/azure/data-api-builder/how-to-run-container).

### Apollo Server
"Apollo Server is an open-source, spec-compliant GraphQL server that's compatible with any GraphQL client, including Apollo Client. It's the best way to build a production-ready, self-documenting GraphQL API that can use data from any source." -[apollographql.com](https://www.apollographql.com/docs/apollo-server/)

#### Local setup

##### Step 1: Apollo server base install
Follow the setup instructions found [here](https://www.apollographql.com/docs/apollo-server/v2/getting-started).

##### Step 2: Install Apollo datasource for Cosmos DB
See [Apollo datasource for Cosmos DB](https://github.com/andrejpk/apollo-datasource-cosmosdb)

Execute the following:
```bash
npm install apollo-server graphql
npm install apollo-datasource-cosmosdb
```

[Apollo setup with typescript](https://deepak-v.medium.com/build-a-scalable-graphql-server-using-typescript-and-apollo-server-4c116ed1425e)

##### Step 3: Execute stored procedures with GraphQL
See [Execute stored procedure with GraphQL](https://stackoverflow.com/questions/73944424/execute-stored-procedure-with-graphql), which defines how a resolver might look for Apollo.
```js
const resolvers = {
  Query: {
    executeGetMetadataReports() {  
      return cosmosdb.execute('select get_metadata_reports();');
    }
  }
}
```

For exposing stored procedures in GraphQL, see [Using TypeScript to write Cosmos DB stored procedures with async/await](https://thomaslevesque.com/2019/07/15/using-typescript-to-write-cosmos-db-stored-procedures-with-async-await/).

#### Unions and interfaces
https://www.apollographql.com/docs/apollo-server/v3/schema/unions-interfaces

#### Apollo Sandbox
Use [this link](https://studio.apollographql.com/sandbox/explorer) to bring up a very nice web app to submit GraphQL queries.

#### Postman
**GET** `http://localhost:4000`.
Change request body type to GraphQL and paste the following:
```graphql
query GetBooks {
  books {
    title
    author
  }
}
```

#### Deployment of Apollo Server
See [Deploy Apollo Server as an Azure Function App](https://www.apollographql.com/docs/apollo-server/v3/deployment/azure-functions/)

#### Generate code from GraphQL schema
https://the-guild.dev/graphql/codegen

#### Apollo License
Apollo is licensed under [Elastic License v2 (ELv2)](https://www.elastic.co/licensing/elastic-license).  See [Apollo License](https://www.apollographql.com/docs/resources/elastic-license-v2-faq/).

#### TO-DO
- [x] Get unions working for different report types
- [x] Get array of reports returned
- [x] Pass parameter to queries
- [x] Get (first:n) working
- [ ] Get pagination working
- [ ] Get sorting working
- [ ] Security example
- [ ] Create PowerPoint deck

### GraphQL Kotlin


## Next Steps
- Create an Architecture Decision Record (ADR) to justify and explain the shift to include GraphQL for PS API
- Deploy solution to Azure
- Test query performance
- Add/test DateScalars so timestamps aren't shown as epoch
- Undetermininic key/value pairs for metadata (challenging since gql strongly typed)
- 
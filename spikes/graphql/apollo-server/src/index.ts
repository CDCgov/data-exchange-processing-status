import { ApolloServer, gql } from "apollo-server";

import { CosmosClient } from "@azure/cosmos";

import { CosmosDataSource } from "apollo-datasource-cosmosdb";

export interface ReportDoc {
  id: string;
}

export class ReportDataSource extends CosmosDataSource<ReportDoc> {}

// Take custom port from ENV
const PORT = process.env.PORT || 4000;

const typeDefs = gql`
  interface ReportType {
    schema_name: String
    schema_version: String
  }

  type Tuple {
    name: String!
    value: String!
  }

  type MetadataVerifyReport implements ReportType {
    schema_name: String
    schema_version: String
    filename: String
    metadata: [Tuple]
  }

  type UploadStatusReport implements ReportType {
    schema_name: String
    schema_version: String
    offset: Float
    size: Float
    # v1 fields
    meta_destination_id: String
    meta_ext_event: String
    # v2 fields
    data_stream_id: String
    data_stream_route: String
  }

  type UnknownReport implements ReportType {
    schema_name: String
    schema_version: String
  }

  # Define the Report type
  type Report {
    id: ID
    uploadId: String
    reportId: String
    dataStreamId: String
    dataStreamRoute: String
    stageName: String
    timestamp: Float
    contentType: String
    content: ReportType
  }

  type Query {
    report(id: ID!): Report
    reports(first: Int, offset: Int): [Report]
  }

  type MetadataReport {
    counter: Float
    hasEntityName: Float
    isMetadataVerify: Float
  }
  
  # Query type for getting metadata report from stored procedure
  type Query {
    metadataReport: MetadataReport
  }
`;

const cosmosClient = new CosmosClient({
  endpoint: "https://pstatus-cosmos-dbaccount.documents.azure.com:443/",
  key: "{{place here}}",
});
const cosmosContainer = cosmosClient.database("ProcessingStatus").container("Reports");

const resolvers = {
  ReportType: {
    __resolveType(report: { schema_name: string; }) {
      switch (report.schema_name) {
        case 'dex-metadata-verify': return 'MetadataVerifyReport';
        case 'upload': return 'UploadStatusReport';
        default: return 'UnknownReport';
      }
    },
  },
  Query: {
    report: async (_: any, { id }: any, { dataSources }: any) => {
      return dataSources.reportsAPI.findOneById(id);
    },
    reports: async (_: any, { first, offset }: any, { dataSources }: any) => {
      console.log("first: " + first);
      console.log("offset: " + offset);
      var sqlQuery = "SELECT * from c";
      var offsetVal = 0;
      if (typeof offset != "undefined") {
        offsetVal = offset;
      }
      sqlQuery += " offset " + offsetVal;
      if (typeof first != "undefined") {
        sqlQuery += " limit " + first;
      }
      console.log("sqlQuery = " + sqlQuery);
      var results = await dataSources.reportsAPI.findManyByQuery(sqlQuery);
      return results.resources;
    },
    metadataReport: async (_: any, { id }: any, { dataSources }: any) => {  
      console.log("id:" + id);
      var storedProcedure = dataSources.reportsAPI.container.scripts.storedProcedure("get_metadata_reports");
      var results = await storedProcedure.execute();
      return results.resource;
    },
  },
};

// Instance of ApolloServer
const server = new ApolloServer({
  typeDefs,
  resolvers,
  dataSources: () => ({
    reportsAPI: new ReportDataSource(cosmosContainer),
  }),
});

server.listen(PORT).then(({ url }) => {
  console.log(`🚀  Server ready at ${url}`);
});

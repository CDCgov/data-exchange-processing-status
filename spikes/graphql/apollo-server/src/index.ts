import { ApolloServer } from "apollo-server";

import { CosmosClient } from "@azure/cosmos";

import { CosmosDataSource } from "apollo-datasource-cosmosdb";

export interface ReportDoc {
  id: string;
  uploadId: string;
}

export class ReportDataSource extends CosmosDataSource<ReportDoc> {}

// Take custom port from ENV
const PORT = process.env.PORT || 4000;

const typeDefs = `
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
  }

  type Query {
    reports:Report
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
  key: "{{place here]}}",
});
const cosmosContainer = cosmosClient.database("ProcessingStatus").container("Reports");

const resolvers = {
  Query: {
    reports: async (_: any, { id }: any, { dataSources }: any) => {
      console.log("id:" + id);
      return dataSources.reportAPI.findOneById("1cc09043-0ba6-4e83-97eb-7bae13b79f5f");
    },
    metadataReport: async (_: any, { id }: any, { dataSources }: any) => {  
      console.log("id:" + id);
      var storedProcedure = dataSources.reportAPI.container.scripts.storedProcedure("get_metadata_reports");
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
    reportAPI: new ReportDataSource(cosmosContainer),
  }),
});

server.listen(PORT).then(({ url }) => {
  console.log(`🚀  Server ready at ${url}`);
});

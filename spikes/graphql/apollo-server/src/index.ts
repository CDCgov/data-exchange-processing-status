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
  type MetadataVerify {
    schema_name: String
    schema_version: String
  }

  type UploadStatus {
    schema_name: String
    schema_version: String
  }

  union Content = MetadataVerify | UploadStatus

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
    content: Content
  }

  type Query {
    Content
    report(id: ID!): Report
    reports(first: Int, limit: Int): [Report]
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
    Content: {
      __resolveType(obj: { schema_name: string; }, _contextValue: any, _info: any) {
      // __resolveType(obj: { schema_name: string; }){
        if(obj.schema_name == 'xyz'){
          return 'xyz';
        }
        if(obj.schema_name == 'abc'){
          return 'abc';
        }
        return null; // GraphQLError is thrown
      },
    },
    report: async (_: any, { id }: any, { dataSources }: any) => {
      return dataSources.reportsAPI.findOneById(id);
    },
    reports: async (_: any, { first, limit }: any, { dataSources }: any) => {
      console.log("first: " + first)
      console.log("limit: " + limit)
      var sqlQuery = "SELECT * from c";
      if (typeof first != "undefined") {
        sqlQuery += " offset 0 limit " + first;
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

"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ReportDataSource = void 0;
const apollo_server_1 = require("apollo-server");
const cosmos_1 = require("@azure/cosmos");
const apollo_datasource_cosmosdb_1 = require("apollo-datasource-cosmosdb");
class ReportDataSource extends apollo_datasource_cosmosdb_1.CosmosDataSource {
}
exports.ReportDataSource = ReportDataSource;
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
const cosmosClient = new cosmos_1.CosmosClient({
    endpoint: "https://pstatus-cosmos-dbaccount.documents.azure.com:443/",
    key: "{{place here]}}",
});
const cosmosContainer = cosmosClient.database("ProcessingStatus").container("Reports");
const resolvers = {
    Query: {
        reports: (_1, _a, _b) => __awaiter(void 0, [_1, _a, _b], void 0, function* (_, { id }, { dataSources }) {
            console.log("id:" + id);
            return dataSources.reportAPI.findOneById("1cc09043-0ba6-4e83-97eb-7bae13b79f5f");
        }),
        metadataReport: (_2, _c, _d) => __awaiter(void 0, [_2, _c, _d], void 0, function* (_, { id }, { dataSources }) {
            console.log("id:" + id);
            var storedProcedure = dataSources.reportAPI.container.scripts.storedProcedure("get_metadata_reports");
            var results = yield storedProcedure.execute();
            return results.resource;
        }),
    },
};
const server = new apollo_server_1.ApolloServer({
    typeDefs,
    resolvers,
    dataSources: () => ({
        reportAPI: new ReportDataSource(cosmosContainer),
    }),
});
server.listen(PORT).then(({ url }) => {
    console.log(`🚀  Server ready at ${url}`);
});

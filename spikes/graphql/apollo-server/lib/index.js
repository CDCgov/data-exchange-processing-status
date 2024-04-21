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
const typeDefs = (0, apollo_server_1.gql) `
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
const cosmosClient = new cosmos_1.CosmosClient({
    endpoint: "https://pstatus-cosmos-dbaccount.documents.azure.com:443/",
    key: "{{place here}}",
});
const cosmosContainer = cosmosClient.database("ProcessingStatus").container("Reports");
const resolvers = {
    ReportType: {
        __resolveType(report) {
            switch (report.schema_name) {
                case 'dex-metadata-verify': return 'MetadataVerifyReport';
                case 'upload': return 'UploadStatusReport';
                default: return 'UnknownReport';
            }
        },
    },
    Query: {
        report: (_1, _a, _b) => __awaiter(void 0, [_1, _a, _b], void 0, function* (_, { id }, { dataSources }) {
            return dataSources.reportsAPI.findOneById(id);
        }),
        reports: (_2, _c, _d) => __awaiter(void 0, [_2, _c, _d], void 0, function* (_, { first, offset }, { dataSources }) {
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
            var results = yield dataSources.reportsAPI.findManyByQuery(sqlQuery);
            return results.resources;
        }),
        metadataReport: (_3, _e, _f) => __awaiter(void 0, [_3, _e, _f], void 0, function* (_, { id }, { dataSources }) {
            console.log("id:" + id);
            var storedProcedure = dataSources.reportsAPI.container.scripts.storedProcedure("get_metadata_reports");
            var results = yield storedProcedure.execute();
            return results.resource;
        }),
    },
};
const server = new apollo_server_1.ApolloServer({
    typeDefs,
    resolvers,
    dataSources: () => ({
        reportsAPI: new ReportDataSource(cosmosContainer),
    }),
});
server.listen(PORT).then(({ url }) => {
    console.log(`🚀  Server ready at ${url}`);
});

import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';
import type { KeyValueCache } from '@apollo/utils.keyvaluecache';
import { GraphQLError } from 'graphql';

import { CosmosClient, Container } from '@azure/cosmos';
import { CosmosDataSource } from 'apollo-datasource-cosmosdb';
import { IncomingMessage } from 'http';

import jsonwebtoken from 'jsonwebtoken';

export interface ReportDoc {
  id: string;
}

export class ReportDataSource extends CosmosDataSource<ReportDoc> {
  private token: string;

  constructor(options: { token: string; cache: KeyValueCache; cosmosContainer: Container }) {
    // super(options); // this sends our server's `cache` through
    super(options.cosmosContainer)
    this.token = options.token.replace("Bearer ", "");
    const verifiedToken = getUser(this.token);
    console.log(`verified token = ${JSON.stringify(verifiedToken)}`);
    if (verifiedToken == null) {
      throw new GraphQLError('You are not authorized to perform this action.', {
        extensions: {
          code: 'FORBIDDEN',
          http: {
            status: 403,
          }
        },
      });
    }
  }
}

interface ContextValue {
  token?: String;
  dataSources: {
    reportsAPI: ReportDataSource;
  };
}

const typeDefs = `
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
    count: Float
    hasSchemaName: Float
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

const JWT_SECRET = "{{place here}}";

const getUser = (token: string) => {
  try {
      if (token) {
        console.log("incoming token = " + token);
        console.log("secret = " + JWT_SECRET);
        return jsonwebtoken.verify(token, JWT_SECRET)
      }
      return null
  } catch (error) {
      return null
  }
}

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
      var sqlQuery = `SELECT * from c WHERE c.id = '${id}'`;
      // var results = await dataSources.reportsAPI.findOneById(id);
      // return results.resource;
      var results = await dataSources.reportsAPI.findManyByQuery(sqlQuery);
      return results.resources[0];
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

const server = new ApolloServer<ContextValue>({
  typeDefs,
  resolvers,
  includeStacktraceInErrorResponses: false
});

function getTokenFromRequest(req: IncomingMessage): string {
  const token = req.headers.authorization || '';
  console.log("req.headers.token = " + req.headers.token);
  console.log("req.headers.authentication = " + req.headers.authentication);
  console.log("req.headers.authorization = " + req.headers.authorization);
  return token;
}

const { url } = await startStandaloneServer<ContextValue>(server, {
  context: async ({ req }) => {
    const token = getTokenFromRequest(req);
    const { cache } = server;
    // return { user: getUser(token.replace('Bearer', ''))}
    return {
      token,
      dataSources: {
        reportsAPI: new ReportDataSource({ cache, token, cosmosContainer }),
      },
    };
  },
  listen: { port: 4000 }
});
console.log(`🚀  Server ready at ${url}`);

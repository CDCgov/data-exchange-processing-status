import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';

import { CosmosClient } from '@azure/cosmos';

import { IncomingMessage } from 'http';

import { typeDefs } from './typeDefs.js';
import { resolvers } from './resolvers.js';
import { ReportDataSource } from './dataSources.js';

import dotenv from 'dotenv';

dotenv.config();

interface ContextValue {
  token?: String;
  dataSources: {
      reportsAPI: ReportDataSource;
  };
}

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

const cosmosClient = new CosmosClient({
  endpoint: process.env.COSMOS_DB_ENDPOINT!,
  key: process.env.COSMOS_DB_KEY!,
});
const cosmosContainer = cosmosClient.database("ProcessingStatus").container("Reports");
  
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
  listen: { port: parseInt(process.env.PORT!) }
});
console.log(`🚀  Server ready at ${url}`);

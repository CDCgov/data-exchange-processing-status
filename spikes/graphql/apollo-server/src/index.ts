import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';

import { CosmosClient } from '@azure/cosmos';

import { IncomingMessage } from 'http';

import { typeDefs } from './typeDefs.js';
import { resolvers } from './resolvers.js';
import { ReportDataSource } from './dataSources.js';

import dotenv from 'dotenv';
dotenv.config();

const { PORT, COSMOS_DB_ENDPOINT, COSMOS_DB_KEY } = process.env;

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
  console.log(`req.headers.token = ${req.headers.token}`);
  console.log(`req.headers.authentication = ${req.headers.authentication}`);
  console.log(`req.headers.authorization = ${req.headers.authorization}`);
  
  const token = req.headers.authorization || '';
  return token;
}

const cosmosClient = new CosmosClient({
  endpoint: COSMOS_DB_ENDPOINT!,
  key: COSMOS_DB_KEY!,
});
const cosmosContainer = cosmosClient.database("ProcessingStatus").container("Reports");
  
const { url } = await startStandaloneServer<ContextValue>(server, {
  context: async ({ req }) => {
    const token = getTokenFromRequest(req);
    const { cache } = server;
    return {
      token,
      dataSources: {
        reportsAPI: new ReportDataSource({ cache, token, cosmosContainer }),
      },
    };
  },
  listen: { port: parseInt(PORT!) }
});
console.log(`🚀  Server ready at ${url}`);

import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';

import { CosmosClient } from '@azure/cosmos';

import { IncomingMessage } from 'http';

import { typeDefs } from './typeDefs.js';
import { resolvers } from './resolvers.js';
import { ReportDataSource } from './dataSources.js';

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
  endpoint: "https://pstatus-cosmos-dbaccount.documents.azure.com:443/",
  key: "{{place here]}}",
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
  listen: { port: 4000 }
});
console.log(`🚀  Server ready at ${url}`);

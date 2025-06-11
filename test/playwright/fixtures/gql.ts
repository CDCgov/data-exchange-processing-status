import { test as baseTest, expect, request, APIRequestContext } from '@playwright/test';
import { getClient, RequesterOptions} from '@gql';
import dotenv from 'dotenv';
import dataGenerator from './dataGenerator';
import { NotificationHelper } from './notificationHelper';
import { GraphQLError } from 'graphql';

export { expect };
export type GraphQLErrorResponse = { errors: GraphQLError[] };

dotenv.config({ path: '../.env' });
const options: RequesterOptions = {
    gqlEndpoint: '/graphql', 
};
type WorkerFixtures = {
    apiContext: APIRequestContext;
    gql: ReturnType<typeof getClient>;
    dataGenerator: typeof dataGenerator;
    notificationHelper: NotificationHelper;
};

export const test = baseTest.extend<{}, WorkerFixtures>({
    apiContext: [
        async ({ }, use) => {
            const apiContext = await request.newContext({
                baseURL: process.env.BASEURL,
                extraHTTPHeaders: {
                    'Authorization': `Bearer ${process.env.GRAPHQL_AUTH_TOKEN || ''}`
                },
            })
            await use(apiContext);
        }, { scope: 'worker' }
    ],
    gql: [
        async ({ apiContext }, use) => { // NOSONAR
            await use(getClient(apiContext, options));
        }, { auto: false, scope: 'worker' }
    ],
    dataGenerator: [
        async ({ }, use) => {
            await use(dataGenerator);
        }, { auto: false, scope: 'worker' }
    ],
    notificationHelper: [
        async ({ gql, apiContext }, use) => {
            const notificationHelper = new NotificationHelper(gql, apiContext);
            await use(notificationHelper);
        }, { auto: false, scope: 'worker' }
    ]
});

import { test as baseTest, expect as baseExpect, request, APIRequestContext } from '@playwright/test';
import { getClient, RequesterOptions} from '@gql';
import dotenv from 'dotenv';
import dataGenerator from './dataGenerator';
import { NotificationHelper } from './notificationHelper';
import { GraphQLError } from 'graphql';
import { SchemaHelper } from './schemaHelper';
import { ReportHelper } from './reportHelper';

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
    schemaHelper: SchemaHelper;
    reportHelper: ReportHelper;
};

export const test = baseTest.extend<{}, WorkerFixtures>({
    apiContext: [
        async ({ }, use) => { // NOSONAR
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
        async ({ apiContext }, use) => { 
            await use(getClient(apiContext, options));
        }, { auto: false, scope: 'worker' }
    ],
    dataGenerator: [
        async ({ }, use) => { // NOSONAR
            await use(dataGenerator);
        }, { auto: false, scope: 'worker' }
    ],
    notificationHelper: [
        async ({ gql, apiContext }, use) => {
            const notificationHelper = new NotificationHelper(gql, apiContext);
            await use(notificationHelper);
        }, { auto: false, scope: 'worker' }
    ],
    schemaHelper: [
        async ({ gql }, use) => {
            await use(new SchemaHelper(gql));
        }, { auto: false, scope: 'worker' }
    ],
    reportHelper: [
        async ({ gql }, use) => {
            await use(new ReportHelper(gql));
        }, { auto: false, scope: 'worker' }
    ]
});

export const expect = baseExpect.extend({
    toBeRecentInSeconds: (received: any, threshold: number = 30) => {
        try {
            const now = new Date()
            const receivedDate: Date = new Date(received)
            const diff = now.getTime() - receivedDate.getTime()
            const diffInSeconds = diff / (1000)
            const pass =  diffInSeconds < threshold

            if (pass) {
                return {
                    message: () => `passed`,
                    pass: true
                }
            } else {
                return {
                    message: () => `expected ${received} to be recent to now ${now} within ${threshold} seconds.  Diff: ${diffInSeconds} seconds`,
                    pass: false
                } 
            }
        } catch (error) {
            return {
                message: () => `Exception thrown`,
                matcherResult: error,
                pass: false
            }
        }
    }
})

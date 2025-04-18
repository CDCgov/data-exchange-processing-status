import { test as baseTest, expect, request, APIRequestContext } from '@playwright/test';
import { getClient, RequesterOptions} from '@gql';

export { expect };

const options: RequesterOptions = {
    gqlEndpoint: '/graphql', 
};
type WorkerFixtures = {
    apiContext: APIRequestContext;
    gql: ReturnType<typeof getClient>;
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
    ]
});

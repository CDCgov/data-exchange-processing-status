import { test as baseTest, expect, request, APIRequestContext } from '@playwright/test';
import { getSdkRequester } from 'playwright-graphql';
import { getSdk } from '@gql';

export { expect };

const getClient = (apiContext: APIRequestContext) => getSdk(getSdkRequester(apiContext, { gqlEndpoint: '/graphql' }));

type WorkerFixtures = {
    gql: ReturnType<typeof getClient>;
};

export const test = baseTest.extend<{}, WorkerFixtures>({
    gql: [
        async ({ }, use) => { // NOSONAR
            const options = {
                extraHTTPHeaders: {
                    'Authorization': `Bearer ${process.env.GRAPHQL_AUTH_TOKEN || ''}`
                }
            };
            const apiContext = await request.newContext(options);
            await use(getClient(apiContext));
        }, { auto: false, scope: 'worker' }
    ]
});

import { test, expect, GraphQLErrorResponse} from '@fixtures/gql';

let subscriptions:string[] = []

test.describe('GraphQL unsubscribeNotificationWorkflow', () => {

    test.afterEach(async ({ notificationHelper }) => { 
        await notificationHelper.subscriptionNotificationWorkflowCleanup(subscriptions);
        subscriptions = [];
    });
    
    test('unsubscribing from email subscription', async ({ gql, dataGenerator, notificationHelper }) => {
        const subscription = dataGenerator.createSubscriptionInput({
            emailAddresses: [`unsubscribeNotificationWorkflow-email@test.com`],
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();

        const subscriptionId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
        subscriptions.push(subscriptionId);

        const unsubscribeRes = await gql.unsubscribeNotificationWorkflow({ subscriptionId: subscriptionId });
        expect(unsubscribeRes.unsubscribeNotificationWorkflow).toBeDefined();
        expect(unsubscribeRes.unsubscribeNotificationWorkflow.subscriptionId).toBeDefined();
        expect(unsubscribeRes.unsubscribeNotificationWorkflow.subscriptionId).toBe(subscriptionId);

        await notificationHelper.validateWorkflowIsNotRunning(subscriptionId);
    });

    test('unsubscribing from webhook subscription', async ({ gql, dataGenerator, notificationHelper }) => {
        const subscription = dataGenerator.createSubscriptionInput({
            webhookUrl: "https://testwebook:80",
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();

        const subscriptionId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
        subscriptions.push(subscriptionId);

        const unsubscribeRes = await gql.unsubscribeNotificationWorkflow({ subscriptionId: subscriptionId });
        expect(unsubscribeRes.unsubscribeNotificationWorkflow).toBeDefined();
        expect(unsubscribeRes.unsubscribeNotificationWorkflow.subscriptionId).toBeDefined();
        expect(unsubscribeRes.unsubscribeNotificationWorkflow.subscriptionId).toBe(subscriptionId);

        await notificationHelper.validateWorkflowIsNotRunning(subscriptionId);
    });

    test('unsubscribing from datastream subscription', async ({ gql, dataGenerator, notificationHelper }) => {
        const subscription = dataGenerator.createSubscriptionInput({
            emailAddresses: [`unsubscribeNotificationWorkflow-datastream@test.com`],
            cronSchedule: "@every 10s",
            dataStreamIds: ["dextesting"],
            dataStreamRoutes: ["testevent1"],
            jurisdictions: ["jurisdiction"]
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();

        const subscriptionId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
        subscriptions.push(subscriptionId);

        const unsubscribeRes = await gql.unsubscribeNotificationWorkflow({ subscriptionId: subscriptionId });
        expect(unsubscribeRes.unsubscribeNotificationWorkflow).toBeDefined();
        expect(unsubscribeRes.unsubscribeNotificationWorkflow.subscriptionId).toBeDefined();
        expect(unsubscribeRes.unsubscribeNotificationWorkflow.subscriptionId).toBe(subscriptionId);

        await notificationHelper.validateWorkflowIsNotRunning(subscriptionId);
    });

    test.describe('unsubscribe errors', () => {
        test('with non-existing subscription id', async ({ gql }) => {
            const unsubscribeRes = await gql.unsubscribeNotificationWorkflow({ subscriptionId: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse
            expect(JSON.stringify(unsubscribeRes.errors)).toMatchSnapshot('non-existing-subscription-id');
        });

        test('with invalid subscription id', async ({ gql }) => {
            const unsubscribeRes = await gql.unsubscribeNotificationWorkflow({ subscriptionId: "INVALID" }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse
            expect(JSON.stringify(unsubscribeRes.errors)).toMatchSnapshot('invalid-subscription-id');
        });

        test('with blank subscription id', async ({ gql }) => {
            const unsubscribeRes = await gql.unsubscribeNotificationWorkflow({ subscriptionId: "" }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse
            expect(JSON.stringify(unsubscribeRes.errors)).toMatchSnapshot('blanksubscription-id');
        });
    });
});

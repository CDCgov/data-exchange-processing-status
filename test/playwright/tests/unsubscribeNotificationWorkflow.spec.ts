import { test, expect } from '@fixtures/gql';
import { GraphQLError } from 'graphql';
import { createSubscriptionInput } from '../fixtures/dataGenerator';

type GraphQLErrorResponse = { errors: GraphQLError[] };

let subscriptions:string[] = []

test.describe('GraphQL unsubscribeNotificationWorkflow', () => {

    test.afterEach(async ({ gql }) => { 
        subscriptions.forEach(async (subscriptionId) => {
            const response = await gql.unsubscribeNotificationWorkflow({ subscriptionId: subscriptionId });
            expect(response.unsubscribeNotificationWorkflow.subscriptionId).toBe(subscriptionId);
        });
        subscriptions = [];
    });
    
    test('unsubscribing from email subscription', async ({ gql }) => {
        const subscription = createSubscriptionInput({
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

        await expect(async () => {
            const workflowsResponse = await gql.getAllWorkflows();
            const runningWorkflows = workflowsResponse.getAllWorkflows.filter(workflow => 
                workflow.workflowId === subscriptionId && 
                workflow.status === "WORKFLOW_EXECUTION_STATUS_RUNNING"
            )
            await expect(runningWorkflows.length).toBe(0);
        }).toPass({
            intervals: [1000],
            timeout: 5000,
        });
    });

    test('unsubscribing from webhook subscription', async ({ gql }) => {
        const subscription = createSubscriptionInput({
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

        await expect(async () => {
            const workflowsResponse = await gql.getAllWorkflows();
            const runningWorkflows = workflowsResponse.getAllWorkflows.filter(workflow => 
                workflow.workflowId === subscriptionId && 
                workflow.status === "WORKFLOW_EXECUTION_STATUS_RUNNING"
            )
            await expect(runningWorkflows.length).toBe(0);
        }).toPass({
            intervals: [1000],
            timeout: 5000,
        });
    });

    test('unsubscribing from datastream subscription', async ({ gql }) => {
        const subscription = createSubscriptionInput({
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

        await expect(async () => {
            const workflowsResponse = await gql.getAllWorkflows();
            const runningWorkflows = workflowsResponse.getAllWorkflows.filter(workflow => 
                workflow.workflowId === subscriptionId && 
                workflow.status === "WORKFLOW_EXECUTION_STATUS_RUNNING"
            )
            await expect(runningWorkflows.length).toBe(0);
        }).toPass({
            intervals: [1000],
            timeout: 5000,
        });
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

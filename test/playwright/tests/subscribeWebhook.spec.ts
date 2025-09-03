import { test, expect, GraphQLErrorResponse } from '@fixtures/gql';

let subscriptions:string[] = []

test.describe('GraphQL subscribeWebhook', () => {
    let webhookUrl: string;
    let token: { uuid: string };
    test.beforeEach(async ({ notificationHelper }) => {
        const result = await notificationHelper.getNewWebhook();
        token = result.token;
        webhookUrl = result.webhookUrl;
    });

    test.afterEach(async ({ gql }) => { 
        subscriptions.forEach(async (subscriptionId) => {
            const response = await gql.unsubscribe({ subscriptionId: subscriptionId });
            expect(response.unsubscribe.subscriptionId).toBe(subscriptionId);
        });
        subscriptions = [];
    });

    test('create subscription to a generic rule', async ({ gql, dataGenerator }) => {
        const subscription = dataGenerator.createWebhookSubscriptionInput({
            dataStreamId: "TestDataStream",
            dataStreamRoute: "TestStreamRoute",
            jurisdiction: "TestJurisdiction",
            ruleDescription: "Generic Rule Description",
            mvelCondition: "true",
            webhookUrl: webhookUrl,
        });

        const res = await gql.subscribeWebhook(subscription);
        expect(res.subscribeWebhook).toBeDefined();
        expect(res.subscribeWebhook.subscriptionId).toBeDefined();
        
        const subscriptionId = res.subscribeWebhook.subscriptionId!.toString();
        subscriptions.push(subscriptionId);
    });

    test('data stream subscription with generic rule should trigger a webhook call', {tag: "@slow"}, async ({ gql, dataGenerator, notificationHelper }) => {   
        const report = dataGenerator.createUploadReportStarted()

        const subscription = dataGenerator.createWebhookSubscriptionInput({
            dataStreamId: report.data_stream_id,
            dataStreamRoute: report.data_stream_route,
            jurisdiction: report.jurisdiction,
            ruleDescription: "Data Stream Rule Description",
            mvelCondition: `true`,
            webhookUrl: webhookUrl,
        });

        const res = await gql.subscribeWebhook(subscription);
        expect(res.subscribeWebhook).toBeDefined();
        expect(res.subscribeWebhook.subscriptionId).toBeDefined();
        
        const subscriptionId = res.subscribeWebhook.subscriptionId!.toString();
        subscriptions.push(subscriptionId);
        
        await notificationHelper.upsertCustomReport(report);
        await notificationHelper.validateWebhookIsCalledForToken(token);
        
    });

    test('data stream subscription with specific rule should trigger a webhook call', {tag: "@slow"}, async ({ gql, dataGenerator, notificationHelper }) => {   
        const report = dataGenerator.createUploadReportStarted()

        const subscription = dataGenerator.createWebhookSubscriptionInput({
            dataStreamId: report.data_stream_id,
            dataStreamRoute: report.data_stream_route,
            jurisdiction: report.jurisdiction,
            ruleDescription: "Data Stream Rule Description",
            mvelCondition: `stageInfo.service == '${report.stage_info.service}' && stageInfo.action == '${report.stage_info.action}'`,
            webhookUrl: webhookUrl,
        });

        const res = await gql.subscribeWebhook(subscription);
        expect(res.subscribeWebhook).toBeDefined();
        expect(res.subscribeWebhook.subscriptionId).toBeDefined();
        
        const subscriptionId = res.subscribeWebhook.subscriptionId!.toString();
        subscriptions.push(subscriptionId);
        
        await notificationHelper.upsertCustomReport(report);
        await notificationHelper.validateWebhookIsCalledForToken(token);
    });

    test('custom schema and data stream subscription should trigger a webhook call', {tag: "@slow"}, async ({ gql, dataGenerator, notificationHelper }) => {   
        const schema = dataGenerator.createRandomSchema()
        const schemaRes = await gql.upsertSchema(schema);
        expect(schemaRes.upsertSchema).toBeDefined();

        const report = {
            ...dataGenerator.createMinimalReport(),
            data_stream_id: "customtestingid",
            data_stream_route: "customtestingroute", 
            jurisdiction: "customtestingjurisdiction",
            content: {
                content_schema_name: schema.schemaName,
                content_schema_version: schema.schemaVersion,
                property1: "test value",
                property2: "SUCCESS",
                status: "SUCCESS"
            }
        }

        const subscription = dataGenerator.createWebhookSubscriptionInput({
            dataStreamId: report.data_stream_id,
            dataStreamRoute: report.data_stream_route,
            jurisdiction: report.jurisdiction,
            ruleDescription: "Custom Schema Rule",
            mvelCondition: `content.property1 == 'test value' && content.property2 == 'SUCCESS'`,
            webhookUrl: webhookUrl,
        });

        const res = await gql.subscribeWebhook(subscription);
        expect(res.subscribeWebhook).toBeDefined();
        expect(res.subscribeWebhook.subscriptionId).toBeDefined();
        const subscriptionId = res.subscribeWebhook.subscriptionId!.toString();
        subscriptions.push(subscriptionId);

        await notificationHelper.upsertCustomReport(report);
        await notificationHelper.validateWebhookIsCalledForToken(token);
    });

    test.describe('subscribing errors', () => {
        test('blank mvel condition', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createWebhookSubscriptionInput({
                mvelCondition: "",
            });

            const res = await gql.subscribeWebhook( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("blank-mvel");
        });

        test('empty data stream id', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createWebhookSubscriptionInput({
                dataStreamId: "",
            });

            const res = await gql.subscribeWebhook( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-data-stream-id");
        });

        test('empty data stream route', async ({ gql, dataGenerator }) => {    
            const subscription = dataGenerator.createWebhookSubscriptionInput({
                dataStreamRoute: "",
            });

            const res = await gql.subscribeWebhook( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-data-stream-route");
        });

        test('empty webhook url', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createWebhookSubscriptionInput({
                webhookUrl: "",
            });

            const res = await gql.subscribeWebhook( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-webhook-url");
        });

    });
});


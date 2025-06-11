import { test, expect } from '@fixtures/gql';
import { GraphQLError } from 'graphql';
import { createMinimalReport, createRandomSchema, createUploadReportStarted, createWebhookSubscriptionInput, UploadReport } from '../fixtures/dataGenerator';
import { APIRequestContext, GqlAPI } from '@gql';

const WEBHOOK_SERVICE = process.env.WEBHOOKURL || "http://localhost:8084";
const WEBHOOK_SERVICE_UI = process.env.WEBHOOKAPI || "http://localhost:8084";

type GraphQLErrorResponse = { errors: GraphQLError[] };

let subscriptions:string[] = []

test.describe('GraphQL subscribeWebhook', () => {
    let webhookUrl: string;
    let token: { uuid: string };
    test.beforeEach(async ({ request }) => {
        const tokenRequest = await request.post(`${WEBHOOK_SERVICE_UI}/token`);
        token = await tokenRequest.json();
        webhookUrl = `${WEBHOOK_SERVICE}/${token.uuid}`;
    });

    test.afterEach(async ({ gql }) => { 
        subscriptions.forEach(async (subscriptionId) => {
            const response = await gql.unsubscribe({ subscriptionId: subscriptionId });
            expect(response.unsubscribe.subscriptionId).toBe(subscriptionId);
        });
        subscriptions = [];
    });

    test('create subscription to a generic rule', async ({ gql }) => {
        const subscription = createWebhookSubscriptionInput({
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

    test('data stream subscription with generic rule should trigger a webhook call', async ({ gql, request }) => {   
        const report = createUploadReportStarted()

        const subscription = createWebhookSubscriptionInput({
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
        
        await upsertCustomReport(gql, report);
        await validateWebookIsCalledForToken(request, token);
        
    });

    test('data stream subscription with specific rule should trigger a webhook call', async ({ gql, request }) => {   
        const report = createUploadReportStarted()

        const subscription = createWebhookSubscriptionInput({
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
        
        await upsertCustomReport(gql, report);
        await validateWebookIsCalledForToken(request, token);
    });

    test('custom schema and data stream subscription should trigger a webhook call', async ({ gql, request }) => {   
        const schema = createRandomSchema()
        const schemaRes = await gql.upsertSchema(schema);
        expect(schemaRes.upsertSchema).toBeDefined();

        const report = {
            ...createMinimalReport(),
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

        const subscription = createWebhookSubscriptionInput({
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

        await upsertCustomReport(gql, report);
        await validateWebookIsCalledForToken(request, token);
    });

    test.describe('subscribing errors', () => {
        test('blank mvel condition', async ({ gql }) => {
            const subscription = createWebhookSubscriptionInput({
                mvelCondition: "",
            });

            const res = await gql.subscribeWebhook( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("blank-mvel");
        });

        test('empty data stream id', async ({ gql }) => {
            const subscription = createWebhookSubscriptionInput({
                dataStreamId: "",
            });

            const res = await gql.subscribeWebhook( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-data-stream-id");
        });

        test('empty data stream route', async ({ gql }) => {    
            const subscription = createWebhookSubscriptionInput({
                dataStreamRoute: "",
            });

            const res = await gql.subscribeWebhook( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-data-stream-route");
        });

        test('empty webhook url', async ({ gql }) => {
            const subscription = createWebhookSubscriptionInput({
                webhookUrl: "",
            });

            const res = await gql.subscribeWebhook( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-webhook-url");
        });

    });
});

async function upsertCustomReport(gql: GqlAPI, report: UploadReport | any) {
    const reportRes = await gql.upsertReport({
        action: "replace",
        report: report,
    });
    expect(reportRes.upsertReport).toBeDefined();
    expect(reportRes.upsertReport.reportId).toBeDefined();
}

async function validateWebookIsCalledForToken(request: APIRequestContext, token: { uuid: string; }) {
    await expect.poll(async () => {
        const webhooksiteResponse = await request.get(`${WEBHOOK_SERVICE_UI}/token/${token.uuid}/requests`);
        const webhookRequests = await webhooksiteResponse.json();
        return webhookRequests.total;
    }, {
        message: "Webhook should be called",
        intervals: [1000, 2000, 5000],
        timeout: 30000,
    }).toBeGreaterThan(0);
}


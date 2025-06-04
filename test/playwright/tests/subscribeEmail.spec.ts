import { test, expect } from '@fixtures/gql';
import { GraphQLError } from 'graphql';
import { createEmailSubscriptionInput, createMinimalReport, createRandomSchema, createUploadReport, createUploadReportStarted } from '../fixtures/dataGenerator';

const EMAIL_SERVICE = process.env.EMAILURL || "http://localhost:8025";

type GraphQLErrorResponse = { errors: GraphQLError[] };

let subscriptions:string[] = []

test.describe('GraphQL subscribeEmail', () => {

    test.afterEach(async ({ gql }) => { 
        subscriptions.forEach(async (subscriptionId) => {
            const response = await gql.unsubscribe({ subscriptionId: subscriptionId });
            expect(response.unsubscribe.subscriptionId).toBe(subscriptionId);
        });
        subscriptions = [];
    });

    test('create subscription to a generic rule', async ({ gql }) => {
        const subscription = createEmailSubscriptionInput({
            emailAddresses: ["subscribeEmail-create@test.com"],
            dataStreamId: "TestDataStream",
            dataStreamRoute: "TestStreamRoute",
            jurisdiction: "TestJurisdiction",
            ruleDescription: "Generic Rule Description",
            mvelCondition: "true",
        });

        const res = await gql.subscribeEmail(subscription);
        expect(res.subscribeEmail).toBeDefined();
        expect(res.subscribeEmail.subscriptionId).toBeDefined();
        
        const subscriptionId = res.subscribeEmail.subscriptionId!.toString();
        subscriptions.push(subscriptionId);
    });

    test('data stream subscription with generic rule should trigger an email', async ({ gql, request }) => {   
        const subscriptionEmail = "subscribeEmail-generic-rule@test.com"
        const report = createUploadReportStarted()

        const subscription = createEmailSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            dataStreamId: report.data_stream_id,
            dataStreamRoute: report.data_stream_route,
            jurisdiction: report.jurisdiction,
            ruleDescription: "Data Stream Rule Description",
            mvelCondition: `true`,
        });

        const res = await gql.subscribeEmail(subscription);
        expect(res.subscribeEmail).toBeDefined();
        expect(res.subscribeEmail.subscriptionId).toBeDefined();
        
        const subscriptionId = res.subscribeEmail.subscriptionId!.toString();
        subscriptions.push(subscriptionId);
        
        const reportRes = await gql.upsertReport({
            action: "replace",
            report: report,
        });
        expect(reportRes.upsertReport).toBeDefined();
        expect(reportRes.upsertReport.reportId).toBeDefined();

        await expect.poll(async () => {
            const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
            const emails = await mailhogResponse.json();
            return emails.total;
        }, {
            message: 'Email should be found',
            timeout: 60000,
        }).toBeGreaterThan(0);

        const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscription.emailAddresses[0]);
        const emails = await mailhogResponse.json();
        expect(emails.items[0].Content.Headers.To[0]).toBe(subscriptionEmail);
        expect(emails.items[0].Content.Headers.Subject[0]).toContain(`Triggered: ${subscription.ruleDescription}`);
    });

    test('data stream subscription with specific rule should trigger an email', async ({ gql, request }) => {   
        const subscriptionEmail = "subscribeEmail-specific-rule@test.com"
        const report = createUploadReportStarted()

        const subscription = createEmailSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            dataStreamId: report.data_stream_id,
            dataStreamRoute: report.data_stream_route,
            jurisdiction: report.jurisdiction,
            ruleDescription: "Data Stream Rule Description",
            mvelCondition: `stageInfo.service == '${report.stage_info.service}' && stageInfo.action == '${report.stage_info.action}'`,
        });

        const res = await gql.subscribeEmail(subscription);
        expect(res.subscribeEmail).toBeDefined();
        expect(res.subscribeEmail.subscriptionId).toBeDefined();
        
        const subscriptionId = res.subscribeEmail.subscriptionId!.toString();
        subscriptions.push(subscriptionId);
        
        const reportRes = await gql.upsertReport({
            action: "replace",
            report: report,
        });
        expect(reportRes.upsertReport).toBeDefined();
        expect(reportRes.upsertReport.reportId).toBeDefined();

        await expect.poll(async () => {
            const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
            const emails = await mailhogResponse.json();
            return emails.total;
        }, {
            message: 'Email should be found',
            timeout: 60000,
        }).toBeGreaterThan(0);

        const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscription.emailAddresses[0]);
        const emails = await mailhogResponse.json();
        expect(emails.items[0].Content.Headers.To[0]).toBe(subscriptionEmail);
        expect(emails.items[0].Content.Headers.Subject[0]).toContain(`Triggered: ${subscription.ruleDescription}`);
    });

    test('custom schema and data stream subscription should trigger an email', async ({ gql, request }) => {   
        const subscriptionEmail = "subscribeEmail-custom-schema@test.com"
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
                status: "SUCCESS" // Required by minimal report content type
            }
        }

        const subscription = createEmailSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            dataStreamId: report.data_stream_id,
            dataStreamRoute: report.data_stream_route,
            jurisdiction: report.jurisdiction,
            ruleDescription: "Custom Schema Rule",
            mvelCondition: `content.property1 == 'test value' && content.property2 == 'SUCCESS'`,
        });

        const res = await gql.subscribeEmail(subscription);
        expect(res.subscribeEmail).toBeDefined();
        expect(res.subscribeEmail.subscriptionId).toBeDefined();
        const subscriptionId = res.subscribeEmail.subscriptionId!.toString();
        subscriptions.push(subscriptionId);

        const reportRes = await gql.upsertReport({
            action: "replace",
            report: report,
        });
        expect(reportRes.upsertReport).toBeDefined();


        await expect.poll(async () => {
            const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
            const emails = await mailhogResponse.json();
            return emails.total;
        }, {
            message: 'Email should be found',
            timeout: 60000,
        }).toBeGreaterThan(0);

        const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscription.emailAddresses[0]);
        const emails = await mailhogResponse.json();
        expect(emails.items[0].Content.Headers.To[0]).toBe(subscriptionEmail);
        expect(emails.items[0].Content.Headers.Subject[0]).toContain(`Triggered: ${subscription.ruleDescription}`);
    });

    test.describe('subscribing errors', () => {
        test('blank mvel condition', async ({ gql }) => {
            const subscription = createEmailSubscriptionInput({
                emailAddresses: ["subscribeEmail-invalid-mvel@test.com"],
                mvelCondition: "",
            });

            const res = await gql.subscribeEmail( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("blank-mvel");
        });

        test('empty data stream id', async ({ gql }) => {
            const subscription = createEmailSubscriptionInput({
                emailAddresses: ["subscribeEmail-empty-data-stream-id@test.com"],
                dataStreamId: "",
            });

            const res = await gql.subscribeEmail( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-data-stream-id");
        });

        test('empty data stream route', async ({ gql }) => {    
            const subscription = createEmailSubscriptionInput({
                emailAddresses: ["subscribeEmail-empty-data-stream-route@test.com"],
                dataStreamRoute: "",
            });

            const res = await gql.subscribeEmail( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-data-stream-route");
        });

        test('empty email addresses', async ({ gql }) => {
            const subscription = createEmailSubscriptionInput({
                emailAddresses: [],
            });

            const res = await gql.subscribeEmail( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-email-addresses");
        });

        test('blank email addresses', async ({ gql }) => {
            const subscription = createEmailSubscriptionInput({
                emailAddresses: [""],
            });

            const res = await gql.subscribeEmail( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("blank-email-addresses");
        });

    });
});

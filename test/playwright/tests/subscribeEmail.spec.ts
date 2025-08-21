import { test, expect, GraphQLErrorResponse } from '@fixtures/gql';

let subscriptions:string[] = []

test.describe('GraphQL subscribeEmail', () => {

    test.afterEach(async ({ notificationHelper }) => { 
        await notificationHelper.subscriptionCleanup(subscriptions);
        subscriptions = [];
    });

    test('create subscription to a generic rule', async ({ gql, dataGenerator }) => {
        const subscription = dataGenerator.createEmailSubscriptionInput({
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

    test('data stream subscription with generic rule should trigger an email', async ({ gql, dataGenerator, notificationHelper }) => {   
        const subscriptionEmail = "subscribeEmail-generic-rule@test.com"
        const report = dataGenerator.createUploadReportStarted()

        const subscription = dataGenerator.createEmailSubscriptionInput({
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
        
        await notificationHelper.upsertCustomReport(report);

        await notificationHelper.validateEmailIsSent(subscriptionEmail, `Triggered: ${subscription.ruleDescription}`);
    });

    test('data stream subscription with specific rule should trigger an email', async ({ gql, dataGenerator, notificationHelper }) => {   
        const subscriptionEmail = "subscribeEmail-specific-rule@test.com"
        const report = dataGenerator.createUploadReportStarted()

        const subscription = dataGenerator.createEmailSubscriptionInput({
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

        await notificationHelper.validateEmailIsSent(subscriptionEmail, `Triggered: ${subscription.ruleDescription}`);

    });

    test('custom schema and data stream subscription should trigger an email', async ({ gql, dataGenerator, notificationHelper }) => {   
        const subscriptionEmail = "subscribeEmail-custom-schema@test.com"
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
                status: "SUCCESS" // Required by minimal report content type
            }
        }

        const subscription = dataGenerator.createEmailSubscriptionInput({
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

        await notificationHelper.upsertCustomReport(report);

        await notificationHelper.validateEmailIsSent(subscriptionEmail, `Triggered: ${subscription.ruleDescription}`);
    });

    test.describe('subscribing errors', () => {
        test('blank mvel condition', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createEmailSubscriptionInput({
                emailAddresses: ["subscribeEmail-invalid-mvel@test.com"],
                mvelCondition: "",
            });

            const res = await gql.subscribeEmail( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("blank-mvel");
        });

        test('empty data stream id', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createEmailSubscriptionInput({
                emailAddresses: ["subscribeEmail-empty-data-stream-id@test.com"],
                dataStreamId: "",
            });

            const res = await gql.subscribeEmail( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-data-stream-id");
        });

        test('empty data stream route', async ({ gql, dataGenerator }) => {    
            const subscription = dataGenerator.createEmailSubscriptionInput({
                emailAddresses: ["subscribeEmail-empty-data-stream-route@test.com"],
                dataStreamRoute: "",
            });

            const res = await gql.subscribeEmail( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-data-stream-route");
        });

        test('empty email addresses', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createEmailSubscriptionInput({
                emailAddresses: [],
            });

            const res = await gql.subscribeEmail( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("empty-email-addresses");
        });

        test('blank email addresses', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createEmailSubscriptionInput({
                emailAddresses: [""],
            });

            const res = await gql.subscribeEmail( subscription , { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("blank-email-addresses");
        });

    });
});


// This test is flaky and only sometimes passes and sometimes fails.
// The invalid subscription MUST be looked at before the other subscriptions to demonstrate
// the issue but it is not always the first subscription to be inspected by the rules engine.
// There is no good way to guarantee the order of subscriptions in the rules engine.
// When this test runs it should be run as: test.describe.serial
test.describe.skip('GraphQL subscribeEmail Catastrophic Failures', () => {
    test('gets email even with an invalid mvel condition', async ({ gql, dataGenerator, notificationHelper }) => {
        const subscriptionEmail = "subscribeEmail-major-mvel-failure@test.com"
        const report = dataGenerator.createUploadReportStarted()

        const subscription = dataGenerator.createEmailSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            dataStreamId: report.data_stream_id,
            dataStreamRoute: report.data_stream_route,
            jurisdiction: report.jurisdiction,
            ruleDescription: "Data Stream Rule Description",
            mvelCondition: `true`,
        });

        // This subscription is a bad rule, but the email for valid subscriptions should still be sent
        const invalidSubscription = dataGenerator.createEmailSubscriptionInput({
            ...subscription,
            dataStreamId: "a",
            dataStreamRoute: "a",
            jurisdiction: "a",
            ruleDescription: "1 - Invalid Data Stream Rule Description",
            mvelCondition: `&&`,
        });

        const invalidRes = await gql.subscribeEmail(invalidSubscription);   
        expect(invalidRes.subscribeEmail).toBeDefined();
        expect(invalidRes.subscribeEmail.subscriptionId).toBeDefined();

        const invalidSubscriptionId = invalidRes.subscribeEmail.subscriptionId!.toString();
        subscriptions.push(invalidSubscriptionId);

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

        await notificationHelper.validateEmailIsSent(subscriptionEmail, `Triggered: ${subscription.ruleDescription}`);
    })
});

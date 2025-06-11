import { test, expect } from '@fixtures/gql';
import { NotificationType } from '@gql';
import { GraphQLError } from 'graphql';
import { GraphQLErrorResponse } from '@fixtures/gql';

let subscriptions:string[] = []

test.describe('GraphQL subscribeUploadDigestCounts', () => {

    test.afterEach(async ({ notificationHelper }) => { 
        await notificationHelper.subscriptionNotificationWorkflowCleanup(subscriptions);
        subscriptions = [];
    });

    test('subscribing via email with duration cron', async ({ gql, request, notificationHelper, dataGenerator }) => {
        const subscriptionEmail = `subscribeUploadDigestCounts-cron-duration@test.com`;
        const subscription = dataGenerator.createSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "@every 10s"
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();        
        subscriptions.push(res.subscribeUploadDigestCounts.subscriptionId!.toString())

        await notificationHelper.validateEmailIsSent(subscriptionEmail, "PHDO UPLOAD DIGEST NOTIFICATION");
    });

    test('subscribing via email with classic cron', async ({ gql, request, notificationHelper, dataGenerator }) => {     
        test.setTimeout(90000); 
        const subscriptionEmail = `subscribeUploadDigestCounts-cron-classic@test.com`;
        const subscription = dataGenerator.createSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "* * * * *"
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();

        subscriptions.push(res.subscribeUploadDigestCounts.subscriptionId!.toString())

        await notificationHelper.validateEmailIsSent(subscriptionEmail, "PHDO UPLOAD DIGEST NOTIFICATION");
    });

    test('subscribing via webhook with duration cron', async ({ gql, request, notificationHelper, dataGenerator }) => {
        const { token, webhookUrl } = await notificationHelper.getNewWebhook();

        const subscription = dataGenerator.createSubscriptionInput({
            webhookUrl: webhookUrl,
            cronSchedule: "@every 5s",
            notificationType: NotificationType.Webhook
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();

        subscriptions.push(res.subscribeUploadDigestCounts.subscriptionId!.toString())
        
        await notificationHelper.validateWebhookIsCalledForToken(token);
    });

    test('subscribing via webhook with classic cron', async ({ gql, request, notificationHelper, dataGenerator }) => {
        test.setTimeout(90000); 
        const { token, webhookUrl } = await notificationHelper.getNewWebhook();

        const subscription = dataGenerator.createSubscriptionInput({  
            webhookUrl: webhookUrl,
            cronSchedule: "* * * * *",
            notificationType: NotificationType.Webhook
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();

        subscriptions.push(res.subscribeUploadDigestCounts.subscriptionId!.toString())
        
        await notificationHelper.validateWebhookIsCalledForToken(token);
    });

    test('subscribing to a generic data stream via email', async ({ gql, notificationHelper, dataGenerator }) => {
        const subscriptionEmail = `subscribeUploadDigestCounts-datastream@test.com`;
        const subscription = dataGenerator.createSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "@every 10s",
            dataStreamIds: [],
            dataStreamRoutes: [],
            jurisdictions: []
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();

        subscriptions.push(res.subscribeUploadDigestCounts.subscriptionId!.toString())
        
        await notificationHelper.validateEmailIsSent(subscriptionEmail, "PHDO UPLOAD DIGEST NOTIFICATION");
    });

    test('subscribing with multiple emails', async ({ gql, request, notificationHelper, dataGenerator }) => {
        const subscriptionEmail1 = `subscribeUploadDigestCounts-multiple-emails-1@test.com`;
        const subscriptionEmail2 = `subscribeUploadDigestCounts-multiple-emails-2@test.com`;
        const subscription = dataGenerator.createSubscriptionInput({
            emailAddresses: [subscriptionEmail1, subscriptionEmail2],
            cronSchedule: "@every 10s"
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();
        
        subscriptions.push(res.subscribeUploadDigestCounts.subscriptionId!.toString())

        await notificationHelper.validateEmailIsSent(subscriptionEmail1, "PHDO UPLOAD DIGEST NOTIFICATION");
        await notificationHelper.validateEmailIsSent(subscriptionEmail2, "PHDO UPLOAD DIGEST NOTIFICATION");
    });
        

    test.describe('subscribing errors', () => {
        test('invalid chron schedule', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createSubscriptionInput({
                emailAddresses: [`subscribeUploadDigestCounts-error-cron@test.com`],
                cronSchedule: "INVALID"
            });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-cron");
        });

        test('invalid notification type', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createSubscriptionInput({
                emailAddresses: [`subscribeUploadDigestCounts-error-notification-type@test.com`],
                notificationType: "INVALID" as unknown as NotificationType
            });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-notification-type");
        });

        test.skip('invalid email format', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createSubscriptionInput({
                emailAddresses: [`subscribeUploadDigestCounts-error-invalid-email`],
                notificationType: NotificationType.Email
            });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
        });

        test.skip('invalid webhook format', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createSubscriptionInput({
                webhookUrl: "bad/webhook/url",
                notificationType: NotificationType.Webhook
            });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
        });
    });
});

import { test, expect, GraphQLErrorResponse } from '@fixtures/gql';
import { NotificationType } from '@gql';

let subscriptions:string[] = []

test.describe('GraphQL subscribeDeadlineCheck', () => {

    test.afterEach(async ({ notificationHelper }) => { 
        await notificationHelper.subscriptionNotificationWorkflowCleanup(subscriptions);
        subscriptions = [];
    });

    test('subscribing via email with duration cron', async ({ gql, notificationHelper, dataGenerator }) => {
        const subscriptionEmail = `subscribeDeadlineCheck-cron-duration@test.com`;
        const subscription = dataGenerator.createDeadlineSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "@every 10s"
        });

        const res = await gql.subscribeDeadlineCheck({ subscription });
        expect(res.subscribeDeadlineCheck).toBeDefined();
        expect(res.subscribeDeadlineCheck.subscriptionId).toBeDefined();
        
        subscriptions.push(res.subscribeDeadlineCheck.subscriptionId!.toString())

        await notificationHelper.validateEmailIsSent(subscriptionEmail, "PHDO DEADLINE MISSED NOTIFICATION");
    });

    test('subscribing via email with classic cron', async ({ gql, notificationHelper, dataGenerator }) => {   
        test.setTimeout(90000); 
        const subscriptionEmail = `subscribeDeadlineCheck-cron-classic@test.com`;
        const subscription = dataGenerator.createDeadlineSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "* * * * *"
        });

        const res = await gql.subscribeDeadlineCheck({ subscription });
        expect(res.subscribeDeadlineCheck).toBeDefined();
        expect(res.subscribeDeadlineCheck.subscriptionId).toBeDefined();

        subscriptions.push(res.subscribeDeadlineCheck.subscriptionId!.toString())
        await notificationHelper.validateEmailIsSent(subscriptionEmail, "PHDO DEADLINE MISSED NOTIFICATION");
    });

    test('subscribing via webhook with duration cron', async ({ gql, notificationHelper, dataGenerator }) => {
        const { token, webhookUrl } = await notificationHelper.getNewWebhook();

        const subscription = dataGenerator.createDeadlineSubscriptionInput({
            webhookUrl: webhookUrl,
            cronSchedule: "@every 5s",
            notificationType: NotificationType.Webhook
        });

        const res = await gql.subscribeDeadlineCheck({ subscription });
        expect(res.subscribeDeadlineCheck).toBeDefined();
        expect(res.subscribeDeadlineCheck.subscriptionId).toBeDefined();

        subscriptions.push(res.subscribeDeadlineCheck.subscriptionId!.toString())
        await notificationHelper.validateWebhookIsCalledForToken(token);
    });

    test('subscribing via webhook with classic cron', async ({ gql, notificationHelper, dataGenerator }) => {
        test.setTimeout(90000); 
        const { token, webhookUrl } = await notificationHelper.getNewWebhook();

        const subscription = dataGenerator.createDeadlineSubscriptionInput({
            webhookUrl: webhookUrl,
            cronSchedule: "* * * * *",
            notificationType: NotificationType.Webhook
        });

        const res = await gql.subscribeDeadlineCheck({ subscription });
        expect(res.subscribeDeadlineCheck).toBeDefined();
        expect(res.subscribeDeadlineCheck.subscriptionId).toBeDefined();

        subscriptions.push(res.subscribeDeadlineCheck.subscriptionId!.toString())
        await notificationHelper.validateWebhookIsCalledForToken(token);
    });

    test('subscribing to a generic data stream via email', async ({ gql, notificationHelper, dataGenerator }) => {
        const subscriptionEmail = `subscribeDeadlineCheck-datastream@test.com`;
        const subscription = dataGenerator.createDeadlineSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "@every 10s",
            dataStreamId: "",
            dataStreamRoute: "",
            expectedJurisdictions: []
        });

        const res = await gql.subscribeDeadlineCheck({ subscription });
        expect(res.subscribeDeadlineCheck).toBeDefined();
        expect(res.subscribeDeadlineCheck.subscriptionId).toBeDefined();

        subscriptions.push(res.subscribeDeadlineCheck.subscriptionId!.toString())
        await notificationHelper.validateEmailIsSent(subscriptionEmail, "PHDO DEADLINE MISSED NOTIFICATION");
    });

    test('subscribing with multiple emails', async ({ gql, notificationHelper, dataGenerator }) => {
        const subscriptionEmail1 = `subscribeDeadlineCheck-multiple-emails-1@test.com`;
        const subscriptionEmail2 = `subscribeDeadlineCheck-multiple-emails-2@test.com`;
        const subscription = dataGenerator.createDeadlineSubscriptionInput({
            emailAddresses: [subscriptionEmail1, subscriptionEmail2],
            cronSchedule: "@every 10s"
        });

        const res = await gql.subscribeDeadlineCheck({ subscription });
        expect(res.subscribeDeadlineCheck).toBeDefined();
        expect(res.subscribeDeadlineCheck.subscriptionId).toBeDefined();
        
        subscriptions.push(res.subscribeDeadlineCheck.subscriptionId!.toString())
        await notificationHelper.validateEmailIsSent(subscriptionEmail1, "PHDO DEADLINE MISSED NOTIFICATION");
        await notificationHelper.validateEmailIsSent(subscriptionEmail2, "PHDO DEADLINE MISSED NOTIFICATION");
    });

    test.describe('subscribing errors', () => {
        test('invalid chron schedule', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createDeadlineSubscriptionInput({
                emailAddresses: [`subscribeDeadlineCheck-error-invalid-cron@test.com`],
                cronSchedule: "INVALID"
            });

            const res = await gql.subscribeDeadlineCheck({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-cron");
        });

        test('invalid notification type', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createDeadlineSubscriptionInput({
                emailAddresses: [`subscribeDeadlineCheck-error-invalid-notification-type@test.com`],
                notificationType: "INVALID" as unknown as NotificationType
            });

            const res = await gql.subscribeDeadlineCheck({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-notification-type");
        });

        test.skip('invalid email format', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createDeadlineSubscriptionInput({
                emailAddresses: [`subscribeDeadlineCheck-error-invalid-email@test.com`],
                notificationType: NotificationType.Email
            });

            const res = await gql.subscribeDeadlineCheck({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
        });

        test.skip('invalid webhook format', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createDeadlineSubscriptionInput({
                webhookUrl: "bad/webhook/url",
                notificationType: NotificationType.Webhook
            });

            const res = await gql.subscribeDeadlineCheck({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
        });

        test('invalid deadline value', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createDeadlineSubscriptionInput({
                emailAddresses: [`subscribeDeadlineCheck-error-invalid-deadline-type@test.com`],
                deadlineTime: "INVALID"
            });

            const res = await gql.subscribeDeadlineCheck({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-notification-type");
        });

        test.skip('invalid webhook subscription with no url', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createDeadlineSubscriptionInput({
                notificationType: NotificationType.Webhook,
                webhookUrl: null,
                emailAddresses: ["subscribeDeadlineCheck-error-invalid-webhook-subscription-with-no-url@test.com"]
            });
            
            const res = await gql.subscribeDeadlineCheck({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-webhook-subscription-with-no-url");
        });

        test.skip('invalid email subscription with no emails', async ({ gql, dataGenerator }) => {
            const subscription = dataGenerator.createDeadlineSubscriptionInput({
                notificationType: NotificationType.Email,
                emailAddresses: [],
                webhookUrl: "http://webhook:9999"
            });
            
            const res = await gql.subscribeDeadlineCheck({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-subscription-with-no-emails");
        }); 
    });
});

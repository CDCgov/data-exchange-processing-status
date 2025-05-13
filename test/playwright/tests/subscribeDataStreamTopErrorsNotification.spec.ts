import { test, expect } from '@fixtures/gql';
import { NotificationType } from '@gql';
import { GraphQLError } from 'graphql';
import { createSubscriptionInput } from '../fixtures/dataGenerator';

const EMAIL_SERVICE = process.env.EMAILURL || "http://localhost:8025";
const WEBHOOK_SERVICE = process.env.WEBHOOKURL || "http://webhook:80";
const WEBHOOK_SERVICE_UI = process.env.WEBHOOKAPI || "http://localhost:8084";

type GraphQLErrorResponse = { errors: GraphQLError[] };

let subscriptions:string[] = []

test.describe('GraphQL subscribeDataStreamTopErrorsNotification', () => {
    test.setTimeout(90000); 

    test.beforeAll(async ({ request }) => { 
        const mailhogResponse = await request.delete(`${EMAIL_SERVICE}/api/v1/messages`);
        expect(mailhogResponse.status()).toBe(200);
    });

    test.afterEach(async ({ gql }) => { 
        subscriptions.forEach(async (subscriptionId) => {
            const response = await gql.unsubscribeNotificationWorkflow({ subscriptionId: subscriptionId });
            expect(response.unsubscribeNotificationWorkflow.subscriptionId).toBe(subscriptionId);
        });
        subscriptions = [];
    });

    test('subscribing via email with duration cron', async ({ gql, request }) => {
        const subscriptionEmail = `subscribeDataStreamTopErrorsNotification-cron-duration@test.com`;
        const subscription = createSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "@every 10s"
        });

        const res = await gql.subscribeDataStreamTopErrorsNotification({ subscription });
        expect(res.subscribeDataStreamTopErrorsNotification).toBeDefined();
        expect(res.subscribeDataStreamTopErrorsNotification.subscriptionId).toBeDefined();
        
        const subscriptionId = res.subscribeDataStreamTopErrorsNotification.subscriptionId!.toString();
        subscriptions.push(subscriptionId);

        await expect.poll(async () => {
            const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
            const emails = await mailhogResponse.json();
            return emails.total;
        }, {
            message: 'Email should be found',
            timeout: 60000,
        }).toBeGreaterThan(0);

        const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
        const emails = await mailhogResponse.json();
        expect(emails.items[0].Content.Headers.To[0]).toBe(subscriptionEmail);
        expect(emails.items[0].Content.Headers.Subject[0]).toContain("DATA STREAM TOP ERRORS NOTIFICATION");
    });

    test('subscribing via email with classic cron', async ({ gql, request }) => {        
        const subscriptionEmail = `subscribeDataStreamTopErrorsNotification-cron-classic@test.com`;
        const subscription = createSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "* * * * *"
        });

        const res = await gql.subscribeDataStreamTopErrorsNotification({ subscription });
        expect(res.subscribeDataStreamTopErrorsNotification).toBeDefined();
        expect(res.subscribeDataStreamTopErrorsNotification.subscriptionId).toBeDefined();

        const subscriptionId = res.subscribeDataStreamTopErrorsNotification.subscriptionId!.toString();
        subscriptions.push(subscriptionId);

        await expect.poll(async () => {
            const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
            const emails = await mailhogResponse.json();
            return emails.total;
        }, {
            message: 'Email should be found',
            timeout: 60000,
        }).toBeGreaterThan(0);

        const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
        const emails = await mailhogResponse.json();
        expect(emails.items[0].Content.Headers.To[0]).toBe(subscriptionEmail);
        expect(emails.items[0].Content.Headers.Subject[0]).toContain("DATA STREAM TOP ERRORS NOTIFICATION");
    });

    test('subscribing via webhook with duration cron', async ({ gql, request }) => {
        const tokenRequest = await request.post(`${WEBHOOK_SERVICE_UI}/token`);
        const token = await tokenRequest.json();
        const webhookUrl = `${WEBHOOK_SERVICE}/${token.uuid}`;

        const subscription = createSubscriptionInput({
            webhookUrl: webhookUrl,
            cronSchedule: "@every 5s",
            notificationType: NotificationType.Webhook
        });

        const res = await gql.subscribeDataStreamTopErrorsNotification({ subscription });
        expect(res.subscribeDataStreamTopErrorsNotification).toBeDefined();
        expect(res.subscribeDataStreamTopErrorsNotification.subscriptionId).toBeDefined();

        const subscriptionId = res.subscribeDataStreamTopErrorsNotification.subscriptionId!.toString();
        subscriptions.push(subscriptionId);
        
        await expect.poll(async () => {
            const webhooksiteResponse = await request.get(`${WEBHOOK_SERVICE_UI}/token/${token.uuid}/requests`);
            const webhookRequests = await webhooksiteResponse.json();
            return webhookRequests.total
        }, {
            message: "Webhook should be called",
            intervals: [5000],
            timeout: 20000,
        }).toBeGreaterThan(0);
    });

    test('subscribing via webhook with classic cron', async ({ gql, request }) => {
        const tokenRequest = await request.post(`${WEBHOOK_SERVICE_UI}/token`);
        const token = await tokenRequest.json();
        const webhookUrl = `${WEBHOOK_SERVICE}/${token.uuid}`;

        const subscription = createSubscriptionInput({
            webhookUrl: webhookUrl,
            cronSchedule: "* * * * *",
            notificationType: NotificationType.Webhook
        });

        const res = await gql.subscribeDataStreamTopErrorsNotification({ subscription });
        expect(res.subscribeDataStreamTopErrorsNotification).toBeDefined();
        expect(res.subscribeDataStreamTopErrorsNotification.subscriptionId).toBeDefined();

        const subscriptionId = res.subscribeDataStreamTopErrorsNotification.subscriptionId!.toString();
        subscriptions.push(subscriptionId);
        
        await expect.poll(async () => {
            const webhooksiteResponse = await request.get(`${WEBHOOK_SERVICE_UI}/token/${token.uuid}/requests`);
            const webhookRequests = await webhooksiteResponse.json();
            return webhookRequests.total
        }, {
            message: "Webhook should be called",
            intervals: [5000],
            timeout: 600000,
        }).toBeGreaterThan(0);
    });

    test('subscribing to a specific data stream via email', async ({ gql, request }) => {
        const subscriptionEmail = `subscribeDataStreamTopErrorsNotification-datastream@test.com`;
        const subscription = createSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "@every 10s",
            dataStreamIds: ["dextesting"],
            dataStreamRoutes: ["testevent1"],
            jurisdictions: ["jurisdiction"]
        });

        const res = await gql.subscribeDataStreamTopErrorsNotification({ subscription });
        expect(res.subscribeDataStreamTopErrorsNotification).toBeDefined();
        expect(res.subscribeDataStreamTopErrorsNotification.subscriptionId).toBeDefined();

        const subscriptionId = res.subscribeDataStreamTopErrorsNotification.subscriptionId!.toString();
        subscriptions.push(subscriptionId);
        
        await expect.poll(async () => {
            const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
            const emails = await mailhogResponse.json();
            return emails.total;
        }, {
            message: 'Email should be found',
            timeout: 60000,
        }).toBeGreaterThan(0);

        const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
        const emails = await mailhogResponse.json();
        expect(emails.items[0].Content.Headers.To[0]).toBe(subscriptionEmail);
        expect(emails.items[0].Content.Headers.Subject[0]).toContain("DATA STREAM TOP ERRORS NOTIFICATION");
    });

    test.describe('subscribing errors', () => {
        test('invalid chron schedule', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                emailAddresses: [`subscribeDataStreamTopErrorsNotification-error-cron@test.com`],
                cronSchedule: "INVALID"
            });

            const res = await gql.subscribeDataStreamTopErrorsNotification({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-cron");
        });

        test('invalid notification type', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                emailAddresses: [`subscribeDataStreamTopErrorsNotification-error-notification-type@test.com`],
                notificationType: "INVALID" as unknown as NotificationType
            });

            const res = await gql.subscribeDataStreamTopErrorsNotification({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-notification-type");
        });

        test.skip('invalid email format', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                emailAddresses: [`subscribeDataStreamTopErrorsNotification-error-invalid-email`],
                notificationType: NotificationType.Email
            });

            const res = await gql.subscribeDataStreamTopErrorsNotification({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
        });

        test.skip('invalid webhook format', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                webhookUrl: "bad/webhook/url",
                notificationType: NotificationType.Webhook
            });

            const res = await gql.subscribeDataStreamTopErrorsNotification({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
        });
    });
});

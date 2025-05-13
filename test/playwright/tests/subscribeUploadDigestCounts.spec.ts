import { test, expect } from '@fixtures/gql';
import { NotificationType } from '@gql';
import { GraphQLError } from 'graphql';
import { createSubscriptionInput } from '../fixtures/dataGenerator';

const EMAIL_SERVICE = process.env.EMAILURL || "http://localhost:8025";
const WEBHOOK_SERVICE = process.env.WEBHOOKURL || "http://webhook:80";
const WEBHOOK_SERVICE_UI = process.env.WEBHOOKAPI || "http://localhost:8084";

type GraphQLErrorResponse = { errors: GraphQLError[] };

let subscriptions:string[] = []

test.describe('GraphQL subscribeUploadDigestCounts', () => {
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
        const subscriptionEmail = `subscribeUploadDigestCounts-cron-duration@test.com`;
        const subscription = createSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "@every 10s"
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();
        
        const subscriptionId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
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
        expect(emails.items[0].Content.Headers.Subject[0]).toContain("PHDO UPLOAD DIGEST NOTIFICATION");
    });

    test('subscribing via email with classic cron', async ({ gql, request }) => {        
        const subscriptionEmail = `subscribeUploadDigestCounts-cron-classic@test.com`;
        const subscription = createSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "* * * * *"
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();

        const subscriptionId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
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
        expect(emails.items[0].Content.Headers.Subject[0]).toContain("PHDO UPLOAD DIGEST NOTIFICATION");
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

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();

        const subscriptionId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
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

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();

        const subscriptionId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
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
        const subscriptionEmail = `subscribeUploadDigestCounts-datastream@test.com`;
        const subscription = createSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "@every 10s",
            dataStreamIds: ["dextesting"],
            dataStreamRoutes: ["testevent1"],
            jurisdictions: ["jurisdiction"]
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();

        const subscriptionId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
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
        expect(emails.items[0].Content.Headers.Subject[0]).toContain("PHDO UPLOAD DIGEST NOTIFICATION");
    });

    test('subscribing with multiple emails', async ({ gql, request }) => {
        const subscriptionEmail1 = `subscribeUploadDigestCounts-multiple-emails-1@test.com`;
        const subscriptionEmail2 = `subscribeUploadDigestCounts-multiple-emails-2@test.com`;
        const subscription = createSubscriptionInput({
            emailAddresses: [subscriptionEmail1, subscriptionEmail2],
            cronSchedule: "@every 10s"
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.subscriptionId).toBeDefined();
        
        const subscriptionId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
        subscriptions.push(subscriptionId);

        await expect.poll(async () => {
            const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail1);
            const emails = await mailhogResponse.json();
            return emails.total;
        }, {
            message: 'First subscribed email should be found',
            timeout: 60000,
        }).toBeGreaterThan(0);

        await expect.poll(async () => {
            const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail2);
            const emails = await mailhogResponse.json();
            return emails.total;
        }, {
            message: 'Second subscribed email should be found',
            timeout: 60000,
        }).toBeGreaterThan(0);
        
        const mailhogResponse1 = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail1);
        const emails1 = await mailhogResponse1.json();
        expect(emails1.items[0].Content.Headers.Subject[0]).toContain("PHDO UPLOAD DIGEST NOTIFICATION");

        const mailhogResponse2 = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail2);
        const emails2 = await mailhogResponse2.json();
        expect(emails2.items[0].Content.Headers.Subject[0]).toContain("PHDO UPLOAD DIGEST NOTIFICATION");
    });

    test.describe('subscribing errors', () => {
        test('invalid chron schedule', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                emailAddresses: [`subscribeUploadDigestCounts-error-cron@test.com`],
                cronSchedule: "INVALID"
            });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-cron");
        });

        test('invalid notification type', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                emailAddresses: [`subscribeUploadDigestCounts-error-notification-type@test.com`],
                notificationType: "INVALID" as unknown as NotificationType
            });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-notification-type");
        });

        test.skip('invalid email format', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                emailAddresses: [`subscribeUploadDigestCounts-error-invalid-email`],
                notificationType: NotificationType.Email
            });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
        });

        test.skip('invalid webhook format', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                webhookUrl: "bad/webhook/url",
                notificationType: NotificationType.Webhook
            });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
        });
    });
});

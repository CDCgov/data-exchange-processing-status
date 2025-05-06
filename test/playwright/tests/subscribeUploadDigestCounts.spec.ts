import { test, expect } from '@fixtures/gql';
import { NotificationType, WorkflowSubscriptionInput } from '@gql';
import { GraphQLError } from 'graphql';

const EMAIL_SERVICE = process.env.EMAILURL || "http://localhost:8025";
const WEBHOOK_SERVICE = process.env.WEBHOOKURL || "http://webhook:80";
const WEBHOOK_SERVICE_UI = process.env.WEBHOOKAPI || "http://localhost:8084";

type GraphQLErrorResponse = { errors: GraphQLError[] };

function createSubscriptionInput({
    emailAddresses = [""],
    cronSchedule = "0 0 1 12 *",
    dataStreamIds = ["dextesting"],
    dataStreamRoutes = ["testevent1"],
    jurisdictions = ["test"],
    notificationType = NotificationType.Email,
    webhookUrl="",
    sinceDays = 1
}: {
    emailAddresses?: string[];
    cronSchedule?: string;
    dataStreamIds?: string[];
    dataStreamRoutes?: string[];
    jurisdictions?: string[];
    notificationType?: NotificationType;
    webhookUrl?: string;
    sinceDays?: number;
}): WorkflowSubscriptionInput {
    return {
        cronSchedule,
        dataStreamIds,
        dataStreamRoutes,
        jurisdictions,
        emailAddresses,
        notificationType,
        webhookUrl,
        sinceDays,
    };
}

test.describe('GraphQL subscribeUploadDigestCounts', () => {

    test.beforeAll(async ({ request }) => { 
        const mailhogResponse = await request.delete(`${EMAIL_SERVICE}/api/v1/messages`);
        expect(mailhogResponse.status()).toBe(200);
    })

    test('subscribes to uploadDigestCounts via email with duration cron', async ({ gql, request }) => {
        test.setTimeout(60000); 
        
        const subscriptionEmail = "subscribeUploadDigestCounts-cron-duration@test.com"
        const subscription = createSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "@every 10s"
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.emailAddresses).toContain(subscriptionEmail);
        expect(res.subscribeUploadDigestCounts.message).toBe("Successfully subscribed");

        const unsubscribeId = res.subscribeUploadDigestCounts.subscriptionId!.toString();

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
        
        await gql.unsubscribeUploadDigestCounts({ subscriptionId: unsubscribeId });
    });

    test('subscribes to uploadDigestCounts via email with classic cron', async ({ gql, request }) => {
        test.setTimeout(90000); 
        
        const subscriptionEmail = "subscribeUploadDigestCounts-cron-classic@test.com"
        const subscription = createSubscriptionInput({
            emailAddresses: [subscriptionEmail],
            cronSchedule: "* * * * *"
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.emailAddresses).toContain(subscriptionEmail);
        expect(res.subscribeUploadDigestCounts.message).toBe("Successfully subscribed");

        const unsubscribeId = res.subscribeUploadDigestCounts.subscriptionId!.toString();

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
        
        await gql.unsubscribeUploadDigestCounts({ subscriptionId: unsubscribeId });
    });

    test('subscribes to uploadDigestCounts with webhook with duration cron', async ({ gql, request }) => {
        test.setTimeout(60000); 

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
        expect(res.subscribeUploadDigestCounts.webhookUrl).toContain(webhookUrl);
        expect(res.subscribeUploadDigestCounts.message).toBe("Successfully subscribed"); 
        
        await expect.poll(async () => {
            const webhooksiteResponse = await request.get(`${WEBHOOK_SERVICE_UI}/token/${token.uuid}/requests`);
            const webhookRequests = await webhooksiteResponse.json();
            return webhookRequests.total
        }, {
            message: "Webhook should be called",
            intervals: [5000],
            timeout: 20000,
        }).toBeGreaterThan(0);
        
        const unsubscribeId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
        await gql.unsubscribeUploadDigestCounts({ subscriptionId: unsubscribeId });
    });

    test('subscribes to uploadDigestCounts with webhook with classic cron', async ({ gql, request }) => {
        test.setTimeout(90000); 

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
        expect(res.subscribeUploadDigestCounts.webhookUrl).toContain(webhookUrl);
        expect(res.subscribeUploadDigestCounts.message).toBe("Successfully subscribed"); 
        
        await expect.poll(async () => {
            const webhooksiteResponse = await request.get(`${WEBHOOK_SERVICE_UI}/token/${token.uuid}/requests`);
            const webhookRequests = await webhooksiteResponse.json();
            return webhookRequests.total
        }, {
            message: "Webhook should be called",
            intervals: [5000],
            timeout: 600000,
        }).toBeGreaterThan(0);
        
        const unsubscribeId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
        await gql.unsubscribeUploadDigestCounts({ subscriptionId: unsubscribeId });
    });

    test('unsubscribes from uploadDigestCounts email', async ({ gql }) => {
        const subscription = createSubscriptionInput({
            emailAddresses: ["unsubscribeUploadDigestCounts@test.com"],
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();

        const unsubscribeId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
        const unsubscribeRes = await gql.unsubscribeUploadDigestCounts({ subscriptionId: unsubscribeId });
        expect(unsubscribeRes.unsubscribeUploadDigestCounts).toBeDefined();
        expect(unsubscribeRes.unsubscribeUploadDigestCounts.message).toBe("Successfully unsubscribed");
        expect(unsubscribeRes.unsubscribeUploadDigestCounts.emailAddresses).toStrictEqual([]);
        expect(unsubscribeRes.unsubscribeUploadDigestCounts.webhookUrl).toBe("");
    })

    test('unsubscribes from uploadDigestCounts webhook', async ({ gql }) => {
        const subscription = createSubscriptionInput({
            webhookUrl: "http://testwebook:80",
        });

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();

        const unsubscribeId = res.subscribeUploadDigestCounts.subscriptionId!.toString();
        const unsubscribeRes = await gql.unsubscribeUploadDigestCounts({ subscriptionId: unsubscribeId });
        expect(unsubscribeRes.unsubscribeUploadDigestCounts).toBeDefined();
        expect(unsubscribeRes.unsubscribeUploadDigestCounts.message).toBe("Successfully unsubscribed");
        expect(unsubscribeRes.unsubscribeUploadDigestCounts.emailAddresses).toStrictEqual([]);
        expect(unsubscribeRes.unsubscribeUploadDigestCounts.webhookUrl).toBe("");
    })

    test.describe('errors with invalid parameters', () => {
        test('invalid chron schedule', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                emailAddresses: ["test-error-cron@test.com"],
                cronSchedule: "INVALID"
            });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-cron");
        })

        test('invalid notification type', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                emailAddresses: ["test-error-notification-type@test.com"],
                notificationType: "INVALID" as unknown as NotificationType
            });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-notification-type");
        })

        test.skip('invalid email format', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                emailAddresses: ["test-error-invalid-email"],
                notificationType: NotificationType.Email
             });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
        })

        test.skip('invalid webhook format', async ({ gql }) => {
            const subscription = createSubscriptionInput({
                webhookUrl: "bad/webhook/url",
                notificationType: NotificationType.Webhook
             });

            const res = await gql.subscribeUploadDigestCounts({ subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
            expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
        })
    })
});

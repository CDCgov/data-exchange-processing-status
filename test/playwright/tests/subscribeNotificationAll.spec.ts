import { test, expect } from '@fixtures/gql';
import { NotificationType } from '@gql';
import { GraphQLError } from 'graphql';
import { createSubscriptionInput } from '../fixtures/dataGenerator';

const EMAIL_SERVICE = process.env.EMAILURL || "http://localhost:8025";
const WEBHOOK_SERVICE = process.env.WEBHOOKURL || "http://webhook:80";
const WEBHOOK_SERVICE_UI = process.env.WEBHOOKAPI || "http://localhost:8084";

type GraphQLErrorResponse = { errors: GraphQLError[] };

type SubscriptionTestCase = {
    subscribeName: string;
    unsubscribeName: string;
    subscribeFn: (gql: any, args: any, options?: any) => Promise<any>;
    unsubscribeFn: (gql: any, args: any) => Promise<any>;
    expectedEmailSubject: string;
};

const cases: SubscriptionTestCase[] = [
    {
        subscribeName: "subscribeUploadDigestCounts",
        unsubscribeName: "unsubscribeUploadDigestCounts",
        subscribeFn: (gql, args, options) => gql.subscribeUploadDigestCounts(args, options),
        unsubscribeFn: (gql, args) => gql.unsubscribeUploadDigestCounts(args),
        expectedEmailSubject: "PHDO UPLOAD DIGEST NOTIFICATION"
    },
    {
        subscribeName: "subscribeDataStreamTopErrorsNotification",
        unsubscribeName: "unsubscribesDataStreamTopErrorsNotification",
        subscribeFn: (gql, args, options?) => gql.subscribeDataStreamTopErrorsNotification(args, options),
        unsubscribeFn: (gql, args) => gql.unsubscribesDataStreamTopErrorsNotification(args),
        expectedEmailSubject: "DATA STREAM TOP ERRORS NOTIFICATION"
    },
    {
        subscribeName: "subscribeDeadlineCheck",
        unsubscribeName: "unsubscribeDeadlineCheck",
        subscribeFn: (gql, args, options?) => gql.subscribeDeadlineCheck(args, options),
        unsubscribeFn: (gql, args) => gql.unsubscribeDeadlineCheck(args),
        expectedEmailSubject: `UPLOAD DEADLINE CHECK EXPIRED for test on ${new Date().toISOString().split('T')[0]}`
    }
];

test.describe('GraphQL Subscribe/Unsubscribe', () => {
    cases.forEach(testCase => {
        test.describe(testCase.subscribeName, () => {
            test.beforeAll(async ({ request }) => { 
                const mailhogResponse = await request.delete(`${EMAIL_SERVICE}/api/v1/messages`);
                expect(mailhogResponse.status()).toBe(200);
            });

            test.only(`${testCase.subscribeName} - email with duration cron`, async ({ gql, request }) => {
                test.setTimeout(60000); 
                const subscriptionEmail = `${testCase.subscribeName}-cron-duration@test.com`;
                const subscription = createSubscriptionInput({
                    emailAddresses: [subscriptionEmail],
                    cronSchedule: "@every 10s"
                });
        
                const res = await testCase.subscribeFn(gql, { subscription });
                expect(res[`${testCase.subscribeName}`]).toBeDefined();
                expect(res[`${testCase.subscribeName}`].emailAddresses).toContain(subscriptionEmail);
                expect(res[`${testCase.subscribeName}`].message).toBe("Successfully subscribed");
        
                const unsubscribeId = res[`${testCase.subscribeName}`].subscriptionId!.toString();
        
                await expect.poll(async () => {
                    const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
                    const emails = await mailhogResponse.json();
                    return emails.total;
                }, {
                    message: 'Email should be found',
                    timeout: 60000,
                    intervals: [10000],
                }).toBeGreaterThan(0);
        
                const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
                const emails = await mailhogResponse.json();
                expect(emails.items[0].Content.Headers.To[0]).toBe(subscriptionEmail);
                expect(emails.items[0].Content.Headers.Subject[0]).toContain(testCase.expectedEmailSubject);
                
                await testCase.unsubscribeFn(gql, { subscriptionId: unsubscribeId });
            });

            test.skip(`${testCase.subscribeName} - email with classic cron`, async ({ gql, request }) => {
                test.setTimeout(90000); 
                
                const subscriptionEmail = `${testCase.subscribeName}-cron-classic@test.com`;
                const subscription = createSubscriptionInput({
                    emailAddresses: [subscriptionEmail],
                    cronSchedule: "* * * * *"
                });
        
                const res = await testCase.subscribeFn(gql, { subscription });
                expect(res[`${testCase.subscribeName}`]).toBeDefined();
                expect(res[`${testCase.subscribeName}`].emailAddresses).toContain(subscriptionEmail);
                expect(res[`${testCase.subscribeName}`].message).toBe("Successfully subscribed");
        
                const unsubscribeId = res[`${testCase.subscribeName}`].subscriptionId!.toString();
        
                await expect.poll(async () => {
                    const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
                    const emails = await mailhogResponse.json();
                    return emails.total;
                }, {
                    message: 'Email should be found',
                    timeout: 90000,
                    intervals: [10000]
                }).toBeGreaterThan(0);
        
                const mailhogResponse = await request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + subscriptionEmail);
                const emails = await mailhogResponse.json();
                expect(emails.items[0].Content.Headers.To[0]).toBe(subscriptionEmail);
                expect(emails.items[0].Content.Headers.Subject[0]).toContain(testCase.expectedEmailSubject);
                
                await testCase.unsubscribeFn(gql, { subscriptionId: unsubscribeId });
            });

            test.only(`${testCase.subscribeName} - webhook with duration cron`, async ({ gql, request }) => {
                test.setTimeout(60000); 

                const tokenRequest = await request.post(`${WEBHOOK_SERVICE_UI}/token`);
                const token = await tokenRequest.json();
                const webhookUrl = `${WEBHOOK_SERVICE}/${token.uuid}`;

                const subscription = createSubscriptionInput({
                    webhookUrl: webhookUrl,
                    cronSchedule: "@every 5s",
                    notificationType: NotificationType.Webhook
                });

                const res = await testCase.subscribeFn(gql, { subscription });
                expect(res[`${testCase.subscribeName}`]).toBeDefined();
                expect(res[`${testCase.subscribeName}`].webhookUrl).toContain(webhookUrl);
                expect(res[`${testCase.subscribeName}`].message).toBe("Successfully subscribed"); 
                
                await expect.poll(async () => {
                    const webhooksiteResponse = await request.get(`${WEBHOOK_SERVICE_UI}/token/${token.uuid}/requests`);
                    const webhookRequests = await webhooksiteResponse.json();
                    return webhookRequests.total
                }, {
                    message: "Webhook should be called",
                    timeout: 60000,
                    intervals: [10000]
                }).toBeGreaterThan(0);
                
                const unsubscribeId = res[`${testCase.subscribeName}`].subscriptionId!.toString();
                await testCase.unsubscribeFn(gql, { subscriptionId: unsubscribeId });
            });

            test.skip(`${testCase.subscribeName} - webhook with classic cron`, async ({ gql, request }) => {
                test.setTimeout(90000); 

                const tokenRequest = await request.post(`${WEBHOOK_SERVICE_UI}/token`);
                const token = await tokenRequest.json();
                const webhookUrl = `${WEBHOOK_SERVICE}/${token.uuid}`;

                const subscription = createSubscriptionInput({
                    webhookUrl: webhookUrl,
                    cronSchedule: "* * * * *",
                    notificationType: NotificationType.Webhook
                });

                const res = await testCase.subscribeFn(gql, { subscription });
                expect(res[`${testCase.subscribeName}`]).toBeDefined();
                expect(res[`${testCase.subscribeName}`].webhookUrl).toContain(webhookUrl);
                expect(res[`${testCase.subscribeName}`].message).toBe("Successfully subscribed"); 
                
                await expect.poll(async () => {
                    const webhooksiteResponse = await request.get(`${WEBHOOK_SERVICE_UI}/token/${token.uuid}/requests`);
                    const webhookRequests = await webhooksiteResponse.json();
                    return webhookRequests.total
                }, {
                    message: "Webhook should be called",
                    timeout: 900000,
                    intervals: [10000]
                }).toBeGreaterThan(0);
                
                const unsubscribeId = res[`${testCase.subscribeName}`].subscriptionId!.toString();
                await testCase.unsubscribeFn(gql, { subscriptionId: unsubscribeId });
            });

            test(`${testCase.subscribeName} - unsubscribe email`, async ({ gql }) => {
                const subscription = createSubscriptionInput({
                    emailAddresses: [`${testCase.subscribeName}-unsubscribe@test.com`],
                });

                const res = await testCase.subscribeFn(gql, { subscription });
                expect(res[`${testCase.subscribeName}`]).toBeDefined();

                const unsubscribeId = res[`${testCase.subscribeName}`].subscriptionId!.toString();
                const unsubscribeRes = await testCase.unsubscribeFn(gql, { subscriptionId: unsubscribeId });
                expect(unsubscribeRes[`${testCase.unsubscribeName}`]).toBeDefined();
                expect(unsubscribeRes[`${testCase.unsubscribeName}`].message).toBe("Successfully unsubscribed");
                expect(unsubscribeRes[`${testCase.unsubscribeName}`].emailAddresses).toStrictEqual([]);
                expect(unsubscribeRes[`${testCase.unsubscribeName}`].webhookUrl).toBe("");
            });

            test(`${testCase.subscribeName} - unsubscribe webhook`, async ({ gql }) => {
                const subscription = createSubscriptionInput({
                    webhookUrl: "https://testwebook:80",
                });

                const res = await testCase.subscribeFn(gql, { subscription });
                expect(res[`${testCase.subscribeName}`]).toBeDefined();

                const unsubscribeId = res[`${testCase.subscribeName}`].subscriptionId!.toString();
                const unsubscribeRes = await testCase.unsubscribeFn(gql, { subscriptionId: unsubscribeId });
                expect(unsubscribeRes[`${testCase.unsubscribeName}`]).toBeDefined();
                expect(unsubscribeRes[`${testCase.unsubscribeName}`].message).toBe("Successfully unsubscribed");
                expect(unsubscribeRes[`${testCase.unsubscribeName}`].emailAddresses).toStrictEqual([]);
                expect(unsubscribeRes[`${testCase.unsubscribeName}`].webhookUrl).toBe("");
            });

            test.describe('errors', () => {
                test(`${testCase.subscribeName} - invalid chron schedule`, async ({ gql }) => {
                    const subscription = createSubscriptionInput({
                        emailAddresses: [`${testCase.subscribeName}-error-cron@test.com`],
                        cronSchedule: "INVALID"
                    });

                    const res = await testCase.subscribeFn(gql, { subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
                    expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-cron");
                });

                test(`${testCase.subscribeName} - invalid notification type`, async ({ gql }) => {
                    const subscription = createSubscriptionInput({
                        emailAddresses: [`${testCase.subscribeName}-error-notification-type@test.com`],
                        notificationType: "INVALID" as unknown as NotificationType
                    });

                    const res = await testCase.subscribeFn(gql, { subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
                    expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-notification-type");
                });

                test.skip(`${testCase.subscribeName} - invalid email format`, async ({ gql }) => {
                    const subscription = createSubscriptionInput({
                        emailAddresses: [`${testCase.subscribeName}-error-invalid-email`],
                        notificationType: NotificationType.Email
                    });

                    const res = await testCase.subscribeFn(gql, { subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
                    expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
                });

                test.skip(`${testCase.subscribeName} - invalid webhook format`, async ({ gql }) => {
                    const subscription = createSubscriptionInput({
                        webhookUrl: "bad/webhook/url",
                        notificationType: NotificationType.Webhook
                    });

                    const res = await testCase.subscribeFn(gql, { subscription }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
                    expect(JSON.stringify(res.errors)).toMatchSnapshot("invalid-email-format");
                });
            });
        });
    });
});

import { test, expect } from '@fixtures/gql';

let subscriptions: string[] = []

test.describe('GraphQL getSubscription', () => {

    test.afterEach(async ({ notificationHelper }) => { 
       await notificationHelper.subscriptionCleanup(subscriptions);
        subscriptions = [];
    });

    test('gets subscription details forexisting email subscription', async ({ gql, dataGenerator }) => {

        const subscription = dataGenerator.createEmailSubscriptionInput({
            emailAddresses: ["subscribeEmail-create@test.com"],
            dataStreamId: "TestDataStream",
            dataStreamRoute: "TestStreamRoute",
            jurisdiction: "TestJurisdiction",
            ruleDescription: "Generic Rule Description",
            mvelCondition: "true",
        });

        const expectedSubscriptionRule = {
            dataStreamId: subscription.dataStreamId,
            dataStreamRoute: subscription.dataStreamRoute,
            jurisdiction: subscription.jurisdiction,
            mvelRuleCondition: subscription.mvelCondition,
            ruleDescription: subscription.ruleDescription
        }

        const res = await gql.subscribeEmail(subscription);
        expect(res.subscribeEmail).toBeDefined();
        expect(res.subscribeEmail?.subscriptionId).toBeDefined();
        subscriptions.push(res.subscribeEmail.subscriptionId!)

        const getSubscriptionResult = await gql.getSubscription({
            subscriptionId: res.subscribeEmail?.subscriptionId!
        })

        expect(getSubscriptionResult.getSubscription).toBeDefined();
        expect(getSubscriptionResult.getSubscription?.subscriptionId).toBe(res.subscribeEmail?.subscriptionId);
        expect(getSubscriptionResult.getSubscription?.notification?.notificationType).toBe("EMAIL");
        expect(getSubscriptionResult.getSubscription?.subscriptionRule).toEqual(expectedSubscriptionRule);
    });

    test('gets subscription details for existing webhook subscription', async ({ gql, dataGenerator }) => {
        const subscription = dataGenerator.createWebhookSubscriptionInput({
            webhookUrl: "https://webhook.site/123e4567-e89b-12d3-a456-426614174000",
            dataStreamId: "TestDataStream",
            dataStreamRoute: "TestStreamRoute",
            jurisdiction: "TestJurisdiction",
            ruleDescription: "Generic Rule Description",
            mvelCondition: "true",
        });

        const expectedSubscriptionRule = {
            dataStreamId: subscription.dataStreamId,
            dataStreamRoute: subscription.dataStreamRoute,
            jurisdiction: subscription.jurisdiction,
            mvelRuleCondition: subscription.mvelCondition,
            ruleDescription: subscription.ruleDescription
        }

        const res = await gql.subscribeWebhook(subscription);
        expect(res.subscribeWebhook).toBeDefined();
        expect(res.subscribeWebhook?.subscriptionId).toBeDefined();
        subscriptions.push(res.subscribeWebhook.subscriptionId!)

        const getSubscriptionResult = await gql.getSubscription({
            subscriptionId: res.subscribeWebhook?.subscriptionId!
        })

        expect(getSubscriptionResult.getSubscription).toBeDefined();
        expect(getSubscriptionResult.getSubscription?.subscriptionId).toBe(res.subscribeWebhook?.subscriptionId);
        expect(getSubscriptionResult.getSubscription?.notification?.notificationType).toBe("WEBHOOK");
        expect(getSubscriptionResult.getSubscription?.subscriptionRule).toEqual(expectedSubscriptionRule);
    });

    test.describe.skip('returns error bad subscription ID', () => {
        const expectedErrors = [
            {
                name: "non-existing subscription ID",
                subscriptionId: "123e4567-aaaa-bbbb-cccc-426614174000",
                snapshot: "non-existing-subscription"
            },
            {
                name: "invalid subscription ID format",
                subscriptionId: "INVALID-UUID",
                snapshot: "invalid-subscription-id-format"
            },
            {
                name: "blank subscription ID",
                subscriptionId: "",
                snapshot: "blank-subscription-id"
            }
        ]

        expectedErrors.forEach(error => {
            test(`${error.name}`, async ({ gql }) => {
                const getSubscriptionResult = await gql.getSubscription({
                    subscriptionId: error.subscriptionId
                }, { failOnEmptyData: false })

                expect(JSON.stringify(getSubscriptionResult)).toMatchSnapshot(error.snapshot)
            });
        });
    });
});

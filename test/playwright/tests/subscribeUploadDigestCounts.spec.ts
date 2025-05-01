import { test, expect } from '@fixtures/gql';
import { NotificationType, WorkflowSubscriptionInput } from '@gql';

test.describe('GraphQL subscribeUploadDigestCounts', () => {
    test('subscribes to uploadDigestCounts via email', async ({ gql, request }) => {
        test.setTimeout(120000); 
        
        const subscriptionEmail = "subscribeUploadDigestCounts@test.com"
        const expectedEmailAddresses = [ subscriptionEmail ]
        const subscription: WorkflowSubscriptionInput = {
            cronSchedule: "* * * * *",
			dataStreamIds: ["dextesting"],
			dataStreamRoutes: ["testevent1"],
			jurisdictions: ["test"],
			emailAddresses: expectedEmailAddresses,
			notificationType: NotificationType.Email,
			sinceDays: 1,
        }

        const res = await gql.subscribeUploadDigestCounts({ subscription });
        expect(res.subscribeUploadDigestCounts).toBeDefined();
        expect(res.subscribeUploadDigestCounts.emailAddresses).toContain(subscriptionEmail);
        expect(res.subscribeUploadDigestCounts.message).toBe("Successfully subscribed");

        const unsubscribeId = res.subscribeUploadDigestCounts.subscriptionId!.toString();

        await expect.poll(async () => {
            const mailhogResponse = await request.get('http://localhost:8025/api/v2/search?kind=containing&query=' + subscriptionEmail);
            const emails = await mailhogResponse.json();
            return emails.total;
        }, {
            message: 'Email should be found',
            timeout: 60000, // 1 minute timeout
        }).toBeGreaterThan(0);

        const mailhogResponse = await request.get('http://localhost:8025/api/v2/search?kind=containing&query=' + subscriptionEmail);
        const emails = await mailhogResponse.json();
        expect(emails.items[0].Content.Headers.To[0]).toBe(subscriptionEmail);
        expect(emails.items[0].Content.Headers.Subject[0]).toContain("PHDO UPLOAD DIGEST NOTIFICATION");
        
        await gql.unsubscribeUploadDigestCounts({ subscriptionId: unsubscribeId });
    });
});

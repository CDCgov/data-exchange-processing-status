import { APIRequestContext, expect } from '@playwright/test';
import { GqlAPI } from '@gql';
import { UploadReport } from './dataGenerator';

const EMAIL_SERVICE = process.env.EMAILURL || "http://localhost:8025";
const WEBHOOK_SERVICE = process.env.WEBHOOKURL || "http://webhook:80";
const WEBHOOK_SERVICE_UI = process.env.WEBHOOKAPI || "http://localhost:8084";


export class NotificationHelper {
    constructor(private readonly gql: GqlAPI, private readonly request: APIRequestContext) {}

    async upsertCustomReport(report: UploadReport | any) {
        const reportRes = await this.gql.upsertReport({
            action: "replace",
            report: report,
        });
        expect(reportRes.upsertReport).toBeDefined();
        expect(reportRes.upsertReport.reportId).toBeDefined();
    }

    async validateWebhookIsCalledForToken( token: { uuid: string; } ) {
        await expect.poll(async () => {
            const webhooksiteResponse = await this.request.get(`${WEBHOOK_SERVICE_UI}/token/${token.uuid}/requests`);
            const webhookRequests = await webhooksiteResponse.json();
            return webhookRequests.total;
        }, {
            message: `Webhook should be called`,
            intervals: [1000, 2000, 5000],
            timeout: 30000,
        }).toBeGreaterThan(0);
    }

    async validateEmailIsSent(expectedEmail: string, expectedSubject: string ) {
        await expect.poll(async () => {
            const mailhogResponse = await this.request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + expectedEmail);
            const emails = await mailhogResponse.json();
            return emails.total;
        }, {
            message: 'Email should be found',
            timeout: 60000,
        }).toBeGreaterThan(0);

        const mailhogResponse = await this.request.get(`${EMAIL_SERVICE}/api/v2/search?kind=containing&query=` + expectedEmail);
        const emails = await mailhogResponse.json();
        expect(emails.items[0].Content.Headers.To[0]).toContain(expectedEmail);
        expect(emails.items[0].Content.Headers.Subject[0]).toContain(expectedSubject);
    }

    async getNewWebhook() {
        const tokenRequest = await this.request.post(`${WEBHOOK_SERVICE_UI}/token`);
        const token = await tokenRequest.json();
        const webhookUrl = `${WEBHOOK_SERVICE}/${token.uuid}`;
        return { token, webhookUrl };
    }

    async subscriptionNotificationWorkflowCleanup(subscriptions: string[]) {
        subscriptions.forEach(async (subscriptionId) => {
            const response = await this.gql.unsubscribeNotificationWorkflow({ subscriptionId: subscriptionId });
            expect(response.unsubscribeNotificationWorkflow.subscriptionId).toBe(subscriptionId);
        });
    }

    async subscriptionCleanup(subscriptions: string[]) {
        subscriptions.forEach(async (subscriptionId) => {
            const response = await this.gql.unsubscribe({ subscriptionId: subscriptionId });
            expect(response.unsubscribe.subscriptionId).toBe(subscriptionId);
        });
    }

    async validateWorkflowIsNotRunning(subscriptionId: string) {
        await expect(async () => {
            const workflowsResponse = await this.gql.getAllWorkflows();
            const runningWorkflows = workflowsResponse.getAllWorkflows.filter(workflow => 
                workflow.workflowId === subscriptionId && 
                workflow.status === "WORKFLOW_EXECUTION_STATUS_RUNNING"
            )
            await expect(runningWorkflows.length).toBe(0);
        }).toPass({
            intervals: [1000],
            timeout: 5000,
        });
    }
}


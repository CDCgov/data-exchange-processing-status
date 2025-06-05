import { test as setup, expect } from '@fixtures/gql';

const EMAIL_SERVICE = process.env.EMAILURL || "http://localhost:8025";

setup('global setup - clear email queue', async ({ request }) => {
    const mailhogResponse = await request.delete(`${EMAIL_SERVICE}/api/v1/messages`);
    expect(mailhogResponse.status()).toBe(200);
});

setup('global setup - clear schema listing', async ({ gql, request }) => {
    const schemasResponse = await gql.listReportSchemas();
    expect(schemasResponse.listReportSchemas).toBeDefined();

    const baseSchemas = [
        {
            "schemaName": "base",
            "schemaVersion": "1.0.0",
            "description": "Base Report",
            "filename": "base.1.0.0.schema.json"
        },
        {
            "schemaName": "metadata-verify",
            "schemaVersion": "1.0.0",
            "description": "Metadata Verify Report",
            "filename": "metadata-verify.1.0.0.schema.json"
        },
        {
            "schemaName": "blob-file-copy",
            "schemaVersion": "1.0.0",
            "description": "Blob File Copy Report",
            "filename": "blob-file-copy.1.0.0.schema.json"
        },
        {
            "schemaName": "upload-started",
            "schemaVersion": "1.0.0",
            "description": "Upload Started Report",
            "filename": "upload-started.1.0.0.schema.json"
        },
        {
            "schemaName": "base",
            "schemaVersion": "0.0.1",
            "description": "Base Report",
            "filename": "base.0.0.1.schema.json"
        },
        {
            "schemaName": "upload-completed",
            "schemaVersion": "1.0.0",
            "description": "Upload Completed Report",
            "filename": "upload-completed.1.0.0.schema.json"
        },
        {
            "schemaName": "metadata-transform",
            "schemaVersion": "1.0.0",
            "description": "Metadata Transform Report",
            "filename": "metadata-transform.1.0.0.schema.json"
        },
        {
            "schemaName": "upload-status",
            "schemaVersion": "1.0.0",
            "description": "Upload Status Report",
            "filename": "upload-status.1.0.0.schema.json"
        }
    ]

    const filteredSchemas = schemasResponse.listReportSchemas.filter(schema =>
        !baseSchemas.some(base =>
            base.schemaName === schema.schemaName &&
            base.schemaVersion === schema.schemaVersion
        )
    );
    filteredSchemas.forEach(async (schema) => {
        await gql.removeSchema({
            schemaName: schema.schemaName,
            schemaVersion: schema.schemaVersion,
        });
    });
});

setup('global setup - clear subscriptions', async ({ gql }) => {
    const subscriptionsResponse = await gql.getAllSubscriptions();
    expect(subscriptionsResponse.getAllSubscriptions).toBeDefined();
    subscriptionsResponse.getAllSubscriptions.forEach(async (subscription) => {
        await gql.unsubscribe({ subscriptionId: subscription.subscriptionId });
    });
});

setup('global setup - clear workflow subscriptions', async ({ gql }) => {
    const workflowSubscriptionsResponse = await gql.getAllWorkflows();
    workflowSubscriptionsResponse.getAllWorkflows.forEach(async (workflow) => {
        await gql.unsubscribeNotificationWorkflow({subscriptionId: workflow.workflowId});
    });

});


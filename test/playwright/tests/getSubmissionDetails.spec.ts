import { test, expect } from '@fixtures/gql';
import { SortOrder } from '@gql';
test.describe('GraphQL getSubmissionDetails', () => {

    test('returns submission details for a simple upload report', async ({ gql, reportHelper }) => {
        const { completeReport } = await reportHelper.createUploadCompleteReport()

        const submissionDetailsResult = await reportHelper.getSubmissionDetailsAndValidate(
            completeReport.upload_id,
            "timestamp",
            SortOrder.Ascending,
            1
        )

        const submissionDetails = submissionDetailsResult.getSubmissionDetails
        reportHelper.validateSubmissionDetailFields(submissionDetails, completeReport)
        reportHelper.validateSubmissionDetailReport(submissionDetails.reports, completeReport)
    })

    test('returns submission details for an upload report with multiple reports', async ({ gql, reportHelper }) => {
        const { startedReport } = await reportHelper.createUploadStartedReport()
        const { completeReport } = await reportHelper.createUploadCompleteReport(startedReport)

        const submissionDetailsResult = await reportHelper.getSubmissionDetailsAndValidate(
            startedReport.upload_id,
            "timestamp",
            SortOrder.Ascending,
            2
        )

        const submissionDetails = submissionDetailsResult.getSubmissionDetails
        reportHelper.validateSubmissionDetailFields(submissionDetails, completeReport)
        reportHelper.validateSubmissionDetailReport(submissionDetails.reports, startedReport)
        reportHelper.validateSubmissionDetailReport(submissionDetails.reports, completeReport)
    })

    test('returns submission details ordered by timestamp ascending', async ({ gql, reportHelper }) => {
        const { startedReport } = await reportHelper.createUploadStartedReport()
        await reportHelper.createUploadCompleteReport(startedReport)

        const submissionDetailsResult = await reportHelper.getSubmissionDetailsAndValidate(
            startedReport.upload_id,
            "timestamp",
            SortOrder.Ascending,
            2
        )

        const firstReport = submissionDetailsResult.getSubmissionDetails.reports![0]
        const secondReport = submissionDetailsResult.getSubmissionDetails.reports![1]

        expect(Date.parse(firstReport.timestamp)).toBeLessThan(Date.parse(secondReport.timestamp))
    })

    test('returns submission details ordered by timestamp descending', async ({ gql, reportHelper }) => {
        const { startedReport } = await reportHelper.createUploadStartedReport()
        await reportHelper.createUploadCompleteReport(startedReport)

        const submissionDetailsResult = await reportHelper.getSubmissionDetailsAndValidate(
            startedReport.upload_id,
            "timestamp",
            SortOrder.Descending,
            2
        )

        const firstReport = submissionDetailsResult.getSubmissionDetails.reports![0]
        const secondReport = submissionDetailsResult.getSubmissionDetails.reports![1]

        expect(Date.parse(firstReport.timestamp)).toBeGreaterThan(Date.parse(secondReport.timestamp))
    })
});


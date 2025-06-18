import { test, expect } from '@fixtures/gql';
import { GetSubmissionDetailsQuery, SortOrder } from '@gql';
test.describe('GraphQL getSubmissionDetails', () => {

    test('returns submission details for a simple upload report', async ({ gql, reportHelper }) => {
        const { completeReport, createCompleteReportResult } = await reportHelper.createUploadCompleteReport()

        let submissionDetailsResult: GetSubmissionDetailsQuery;
        await expect(async () => {
            submissionDetailsResult = await gql.getSubmissionDetails({
                uploadId: completeReport.upload_id,
                reportsSortedBy: "timestamp",
                sortOrder: SortOrder.Ascending
            })
            expect(submissionDetailsResult.getSubmissionDetails).toBeDefined()
            
        }).toPass({
            intervals: [1_000, 2_000, 5_000],
        })

        const submissionDetails = submissionDetailsResult!.getSubmissionDetails
        reportHelper.validateSubmissionDetailFields(submissionDetails, completeReport)
        reportHelper.validateSubmissionDetailReport(submissionDetails.reports, completeReport)
        
    })

    test('returns submission details for an upload report with multiple reports', async ({ gql, reportHelper }) => {
        const { startedReport, createStartedReportResult } = await reportHelper.createUploadStartedReport()
        const { completeReport, createCompleteReportResult } = await reportHelper.createUploadCompleteReport(startedReport)

        let submissionDetailsResult: GetSubmissionDetailsQuery;
        await expect(async () => {
            submissionDetailsResult = await gql.getSubmissionDetails({
                uploadId: startedReport.upload_id,
                reportsSortedBy: "timestamp",
                sortOrder: SortOrder.Ascending
            })
            expect(submissionDetailsResult.getSubmissionDetails).toBeDefined()
            expect(submissionDetailsResult.getSubmissionDetails.reports).toHaveLength(2)
            
        }).toPass({
            intervals: [1_000, 2_000, 5_000],
        })

        const submissionDetails = submissionDetailsResult!.getSubmissionDetails
        reportHelper.validateSubmissionDetailFields(submissionDetails, completeReport)
        reportHelper.validateSubmissionDetailReport(submissionDetails.reports, startedReport)
        reportHelper.validateSubmissionDetailReport(submissionDetails.reports, completeReport)
        
    })

    test('returns submission details ordered by timestamp ascending', async ({ gql, reportHelper }) => {
        const { startedReport, createStartedReportResult } = await reportHelper.createUploadStartedReport()
        const { completeReport, createCompleteReportResult } = await reportHelper.createUploadCompleteReport(startedReport)

        let submissionDetailsResult: GetSubmissionDetailsQuery;
        await expect(async () => {
            submissionDetailsResult = await gql.getSubmissionDetails({
                uploadId: startedReport.upload_id,
                reportsSortedBy: "timestamp",
                sortOrder: SortOrder.Ascending
            })
            expect(submissionDetailsResult.getSubmissionDetails).toBeDefined()
            expect(submissionDetailsResult.getSubmissionDetails.reports).toHaveLength(2)
            
        }).toPass({
            intervals: [1_000, 2_000, 5_000],
        })

        const firstReport = submissionDetailsResult!.getSubmissionDetails.reports![0]
        const secondReport = submissionDetailsResult!.getSubmissionDetails.reports![1]

        expect(Date.parse(firstReport.timestamp)).toBeLessThan(Date.parse(secondReport.timestamp))

    })

    test('returns submission details ordered by timestamp descending', async ({ gql, reportHelper }) => {
        const { startedReport, createStartedReportResult } = await reportHelper.createUploadStartedReport()
        const { completeReport, createCompleteReportResult } = await reportHelper.createUploadCompleteReport(startedReport)

        let submissionDetailsResult: GetSubmissionDetailsQuery;
        await expect(async () => {
            submissionDetailsResult = await gql.getSubmissionDetails({
                uploadId: startedReport.upload_id,
                reportsSortedBy: "timestamp",
                sortOrder: SortOrder.Descending
            })
            expect(submissionDetailsResult.getSubmissionDetails).toBeDefined()
            expect(submissionDetailsResult.getSubmissionDetails.reports).toHaveLength(2)
            
        }).toPass({
            intervals: [1_000, 2_000, 5_000],
        })

        const firstReport = submissionDetailsResult!.getSubmissionDetails.reports![0]
        const secondReport = submissionDetailsResult!.getSubmissionDetails.reports![1]

        expect(Date.parse(firstReport.timestamp)).toBeGreaterThan(Date.parse(secondReport.timestamp))

    })
});


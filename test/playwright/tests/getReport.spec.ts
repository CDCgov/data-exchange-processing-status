import { test, expect } from '@fixtures/gql';
import { SortOrder, Report } from '@gql';
import dataGenerator, { createUploadReport, UploadReport } from 'fixtures/dataGenerator';

test.describe('GraphQL getReports', () => {

    test('returns a report for a typical upload report', async ({ gql }) => {
        const typicalReport = await dataGenerator.createUploadReport()

        const createReportResult = await gql.upsertReport({
            action: "create",
            report: typicalReport
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: typicalReport.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBe(1)

        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, typicalReport)
            validateNonRequiredFields(report, typicalReport)
            validateContentInfo(report, typicalReport)            
            validateStageInfo(report, typicalReport)
            expect(report.stageInfo?.issues).toBeNull()
        });
    });

    test('returns a report for a minimal upload report', async ({ gql }) => {
        const minimalReport = await dataGenerator.createMinimalReport()

        const createReportResult = await gql.upsertReport({
            action: "create",
            report: minimalReport
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: minimalReport.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBe(1)
        
        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, minimalReport)
            validateContentInfo(report, minimalReport)
            expect(report.stageInfo?.issues).toBeNull()
        });

    });

    test('returns a report for a minimal upload report with error issue', async ({ gql }) => {
        const uploadReport = await dataGenerator.createUploadReport()

        const uploadReportWithError = {
            ...uploadReport,
            stage_info: dataGenerator.createStageInfoWithError()
        }
        const createReportResult = await gql.upsertReport({
            action: "create",
            report: uploadReportWithError
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: uploadReportWithError.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBe(1)

        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, uploadReportWithError)
            validateContentInfo(report, uploadReportWithError)
            validateStageInfo(report, uploadReportWithError)
            expect(report.stageInfo?.issues).toHaveLength(1)
            report.stageInfo?.issues?.forEach((issue, index) => {
                expect(issue.level).toEqual(uploadReportWithError.stage_info.issues[index].level)
                expect(issue.message).toEqual(uploadReportWithError.stage_info.issues[index].message)
            })
        });
        
    });

    test('returns a report for an upload report with warn issue', async ({ gql }) => {
        const uploadReport = await dataGenerator.createUploadReport()

        const uploadReportWithWarnIssue = {
            ...uploadReport,
            stage_info: dataGenerator.createStageInfoWithWarning()
        }
        const createReportResult = await gql.upsertReport({
            action: "create",
            report: uploadReportWithWarnIssue
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: uploadReportWithWarnIssue.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBe(1)

        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, uploadReportWithWarnIssue)
            validateContentInfo(report, uploadReportWithWarnIssue)
            validateStageInfo(report, uploadReportWithWarnIssue)
            expect(report.stageInfo?.issues).toHaveLength(1)
            report.stageInfo?.issues?.forEach((issue, index) => {
                expect(issue.level).toEqual(uploadReportWithWarnIssue.stage_info.issues[index].level)
                expect(issue.message).toEqual(uploadReportWithWarnIssue.stage_info.issues[index].message)
            })
        });
    });

    test('returns a report for an upload report with message metadata', async ({ gql }) => {
        const uploadReport = await dataGenerator.createUploadReport()

        const uploadWithMessageMetadata = {
            ...uploadReport,
            message_metadata: dataGenerator.createMessageMetadata()
        }
        const createReportResult = await gql.upsertReport({
            action: "create",
            report: uploadWithMessageMetadata
        })
        console.log(createReportResult)
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: uploadWithMessageMetadata.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBe(1)

        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, uploadWithMessageMetadata)
            validateContentInfo(report, uploadWithMessageMetadata)
            validateStageInfo(report, uploadWithMessageMetadata)
            validateMessageMetadata(report, uploadWithMessageMetadata)
        });
    
    });

    test('returns all reports for an upload with multiple report in ascending order', async ({ gql }) => {
        const uploadReportStart = await dataGenerator.createUploadReportStarted()
        const uploadReportStatus = await dataGenerator.createUploadReportStatus(uploadReportStart)
        const uploadReportComplete = await dataGenerator.createUploadReportCompleted(uploadReportStart)

        const upsertReportStartResult = await gql.upsertReport({
            action: "create",
            report: uploadReportStart
        })
        expect(upsertReportStartResult.upsertReport.result).toBe("SUCCESS")

        const upsertReportStatusResult = await gql.upsertReport({
            action: "create",
            report: uploadReportStatus
        })
        expect(upsertReportStatusResult.upsertReport.result).toBe("SUCCESS")

        const upsertReportCompleteResult = await gql.upsertReport({
            action: "create",
            report: uploadReportComplete
        })
        expect(upsertReportCompleteResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: uploadReportStart.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBe(3)

        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, uploadReportStart)
        });
        console.log("Ascending report: ", reportResult.getReports)

        validateContentInfo(reportResult.getReports[0], uploadReportStart)
        validateContentInfo(reportResult.getReports[1], uploadReportStatus)
        validateContentInfo(reportResult.getReports[2], uploadReportComplete)
    });

    test('returns all reports for an upload with multiple reports in descending order', async ({ gql }) => {
        const uploadReportStart = await dataGenerator.createUploadReportStarted()
        const uploadReportStatus = await dataGenerator.createUploadReportStatus(uploadReportStart)
        const uploadReportComplete = await dataGenerator.createUploadReportCompleted(uploadReportStart)

        const upsertReportStartResult = await gql.upsertReport({
            action: "create",
            report: uploadReportStart
        })
        expect(upsertReportStartResult.upsertReport.result).toBe("SUCCESS")

        const upsertReportStatusResult = await gql.upsertReport({
            action: "create",
            report: uploadReportStatus
        })
        expect(upsertReportStatusResult.upsertReport.result).toBe("SUCCESS")

        const upsertReportCompleteResult = await gql.upsertReport({
            action: "create",
            report: uploadReportComplete
        })
        expect(upsertReportCompleteResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: uploadReportStart.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Descending
        })
        expect(reportResult.getReports.length).toBe(3)
        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, uploadReportStart)
        });

        console.log("Descending report: ", reportResult.getReports)

        validateContentInfo(reportResult.getReports[0], uploadReportComplete)
        validateContentInfo(reportResult.getReports[2], uploadReportStart)
        validateContentInfo(reportResult.getReports[1], uploadReportStatus)
    });

    test('returns a report that has been updated after initial entry', async ({ gql }) => {
        const typicalReport = await dataGenerator.createUploadReport()

        const createReportResult = await gql.upsertReport({
            action: "create",
            report: typicalReport
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")

        const newReport: UploadReport = createUploadReport()
        newReport.upload_id = typicalReport.upload_id

        const createNewReportResult = await gql.upsertReport({
            action: "replace",
            report: newReport
        })
        expect(createNewReportResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: newReport.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBe(1)

        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, newReport)
            validateNonRequiredFields(report, newReport)
            validateContentInfo(report, newReport)            
            validateStageInfo(report, newReport)
            expect(report.stageInfo?.issues).toBeNull()
        });
    })
});

function validateBasicFields(report: any, uploadReport: any) {
    expect(report.uploadId).toEqual(uploadReport.upload_id)
    expect(report.dataStreamId).toEqual(uploadReport.data_stream_id)
    expect(report.dataStreamRoute).toEqual(uploadReport.data_stream_route)
    expect(report.contentType).toEqual(uploadReport.content_type)
}

function validateNonRequiredFields(report: any, uploadReport: any) {
    expect(report.reportSchemaVersion).toEqual(uploadReport.report_schema_version)
    expect(report.jurisdiction).toEqual(uploadReport.jurisdiction)
    expect(report.senderId).toEqual(uploadReport.sender_id)
    expect(report.dataProducerId).toEqual(uploadReport.data_producer_id)
    expect(report.dataProducerId).toEqual(uploadReport.data_producer_id)
    expect(report.dataProducerId).toEqual(uploadReport.data_producer_id)
    expect(report.contentType).toEqual(uploadReport.content_type)
    expect(report.id).toBeDefined()
    expect(report.timestamp).toBeDefined()
    expect(report.data.dataField1).toEqual(uploadReport.data.data_field1)
    expect(report.tags.tagField1).toEqual(uploadReport.tags.tag_field1)
    //expect(report.dexIngestDateTime).toEqual(uploadReport.dex_ingest_datetime)
}

function validateStageInfo(report: any, uploadReport: any) {
    expect(report.stageInfo?.status).toEqual(uploadReport.stage_info.status)
    // these are currently not able to be set
    // expect(report.stageInfo?.startProcessingTime).toEqual(uploadReport.stage_info.start_processing_time)
    // expect(report.stageInfo?.endProcessingTime).toEqual(uploadReport.end_processing_time)
}

function validateContentInfo(report: any, uploadReport: any) {
    expect(report.content.contentSchemaName).toEqual(uploadReport.content.content_schema_name)
    expect(report.content.contentSchemaVersion).toEqual(uploadReport.content.content_schema_version)
    if (uploadReport.content.content_schema_name === "upload-started" || "upload-completed") {
        expect(report.content.status).toEqual(uploadReport.content.status)
    }
    if (uploadReport.content.content_schema_name === "upload-status") {
        expect(report.content.filename).toEqual(uploadReport.content.filename)
        expect(report.content.offset).toEqual(uploadReport.content.offset)
        expect(report.content.size).toEqual(uploadReport.content.size)
        expect(report.content.tguid).toEqual(uploadReport.content.tguid)
    }
}

function validateMessageMetadata(report: Report, uploadReport: any) {
    console.log(report.messageMetadata)
    expect(report.messageMetadata?.aggregation).toEqual(uploadReport.message_metadata.aggregation)
    // These are currently not able to be set
    // expect(report.messageMetadata?.messageUUID).toEqual(uploadReport.message_metadata.message_uuid)
    // expect(report.messageMetadata?.messageHash).toEqual(uploadReport.message_metadata.message_hash)
    // expect(report.messageMetadata?.messageIndex).toEqual(uploadReport.message_metadata.message_index)
}


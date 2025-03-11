import { test, expect } from '@fixtures/gql';
import { SortOrder } from '@gql';
import { createMinimalReport, createStageInfoWithError, createStageInfoWithWarning, createUploadReport } from 'fixtures/dataGenerator';

test.describe('GraphQL getReports', () => {

    test('returns a report for a typical upload report', async ({ gql }) => {
        const uploadReport = await createUploadReport();
        const createReportResult = await gql.upsertReport({
            action: "create",
            report: uploadReport
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: uploadReport.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBe(1)

        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, uploadReport)
            validateNonRequiredFields(report, uploadReport)
            validateMessageMetadata(report, uploadReport)
            validateContentInfo(report, uploadReport)            
            validateStageInfo(report, uploadReport)
            expect(report.stageInfo?.issues).toBeNull()

        });
    });

    test('returns a report for a minimal upload report', async ({ gql }) => {
        const uploadReport = await createMinimalReport();
        const createReportResult = await gql.upsertReport({
            action: "create",
            report: uploadReport
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: uploadReport.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBe(1)
        
        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, uploadReport)
            validateContentInfo(report, uploadReport)
            expect(report.stageInfo?.issues).toBeNull()

        });

    });

    test('returns a report for a minimal upload report with error issue', async ({ gql }) => {
        const uploadReport = {
            ...createMinimalReport(),
            stage_info: createStageInfoWithError()
        }
        const createReportResult = await gql.upsertReport({
            action: "create",
            report: uploadReport
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: uploadReport.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBe(1)

        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, uploadReport)
            validateContentInfo(report, uploadReport)
            validateStageInfo(report, uploadReport)
            expect(report.stageInfo?.issues).toHaveLength(1)
            report.stageInfo?.issues?.forEach((issue, index) => {
                expect(issue.level).toEqual(uploadReport.stage_info.issues[index].level)
                expect(issue.message).toEqual(uploadReport.stage_info.issues[index].message)
            })
        });
        
    });

    test('returns a report for a minimal upload report with warn issue', async ({ gql }) => {
        const uploadReport = {
            ...createMinimalReport(),
            stage_info: createStageInfoWithWarning()
        }
        const createReportResult = await gql.upsertReport({
            action: "create",
            report: uploadReport
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")

        const reportResult = await gql.getReports({
            uploadId: uploadReport.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBe(1)

        await reportResult.getReports.forEach(report => {
            validateBasicFields(report, uploadReport)
            validateContentInfo(report, uploadReport)
            validateStageInfo(report, uploadReport)
            expect(report.stageInfo?.issues).toHaveLength(1)
            report.stageInfo?.issues?.forEach((issue, index) => {
                expect(issue.level).toEqual(uploadReport.stage_info.issues[index].level)
                expect(issue.message).toEqual(uploadReport.stage_info.issues[index].message)
            })
        });
        
    });

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
    // expect(report.stageInfo?.startProcessingTime).toEqual(uploadReport.stage_info.start_processing_time)
    // expect(report.stageInfo?.endProcessingTime).toEqual(uploadReport.end_processing_time)
}

function validateContentInfo(report: any, uploadReport: any) {
    expect(report.content.contentSchemaName).toEqual(uploadReport.content.content_schema_name)
    expect(report.content.contentSchemaVersion).toEqual(uploadReport.content.content_schema_version)
    expect(report.content.status).toEqual(uploadReport.content.status)
}

function validateMessageMetadata(report: any, uploadReport: any) {
    expect(report.messageMetadata?.aggregation).toEqual(uploadReport.message_metadata.aggregation)
    // expect(report.messageMetadata?.messageUUID).toEqual(uploadReport.message_metadata.message_uuid)
    // expect(report.messageMetadata?.messageHash).toEqual(uploadReport.message_metadata.message_hash)
    // expect(report.messageMetadata?.messageIndex).toEqual(uploadReport.message_metadata.message_index)
}


import { test, expect } from '@fixtures/gql';
import { SortOrder } from '@gql';
import { createUploadReport } from 'fixtures/dataGenerator';
import { report } from 'process';

test.describe('GraphQL getReports', () => {
    let uploadReport: any

    test.beforeAll(async ({ gql }) => {
        uploadReport = await createUploadReport();
        console.log(uploadReport)

        const createReportResult = await gql.upsertReport({
            action: "create",
            report: uploadReport
        })
        console.log(createReportResult)
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")
        
    })
    
    test('returns a report for a known upload id', async ({ gql }) => {
        const reportResult = await gql.getReports({
            uploadId: uploadReport.upload_id,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        console.log(reportResult.getReports[0])
        expect(reportResult.getReports.length).toBeGreaterThanOrEqual(1)

        await reportResult.getReports.forEach(report => {
            expect(report.reportSchemaVersion).toEqual(uploadReport.report_schema_version)
            expect(report.uploadId).toEqual(uploadReport.upload_id)            
            expect(report.dataStreamId).toEqual(uploadReport.data_stream_id)
            expect(report.dataStreamRoute).toEqual(uploadReport.data_stream_route)
            expect(report.jurisdiction).toEqual(uploadReport.jurisdiction)
            expect(report.senderId).toEqual(uploadReport.sender_id)
            expect(report.dataProducerId).toEqual(uploadReport.data_producer_id)
            expect(report.dataProducerId).toEqual(uploadReport.data_producer_id)
            expect(report.dataProducerId).toEqual(uploadReport.data_producer_id)
            expect(report.id).toBeDefined()
            expect(report.timestamp).toBeDefined()
            //expect(report.dexIngestDateTime).toEqual(uploadReport.dex_ingest_datetime)

            // Message Metadata Section
            // expect(report.messageMetadata?.messageUUID).toEqual(uploadReport.message_metadata.message_uuid)
            // expect(report.messageMetadata?.messageHash).toEqual(uploadReport.message_metadata.message_hash)
            // expect(report.messageMetadata?.messageIndex).toEqual(uploadReport.message_metadata.message_index)
            expect(report.messageMetadata?.aggregation).toEqual(uploadReport.message_metadata.aggregation)

            //Content Section
            expect(report.content.contentSchemaName).toEqual(uploadReport.content.content_schema_name)
            expect(report.content.contentSchemaVersion).toEqual(uploadReport.content.content_schema_version)
            expect(report.content.status).toEqual(uploadReport.content.status)

            //data section
            expect(report.data.dataField1).toEqual(uploadReport.data.data_field1)
            
            //stage info section
            expect(report.stageInfo?.status).toEqual(uploadReport.stage_info.status)
            expect(report.stageInfo?.issues).toEqual(uploadReport.stage_info.issues)
            // expect(report.stageInfo?.startProcessingTime).toEqual(uploadReport.stage_info.start_processing_time)
            // expect(report.stageInfo?.endProcessingTime).toEqual(uploadReport.end_processing_time)
            
            //tags section
            expect(report.tags.tagField1).toEqual(uploadReport.tags.tag_field1)
        });
    })

} )

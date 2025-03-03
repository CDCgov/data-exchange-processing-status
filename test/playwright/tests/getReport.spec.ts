import { test, expect } from '@fixtures/gql';
import { SortOrder } from '@gql';
import { report } from 'process';

test.describe('GraphQL getReports', () => {
    test.skip('returns a report for a known upload id', async ({ gql }) => {
        const expectedPageSize = 1
        const expectedPageNumber = 1   
        const expectedDataStreamId = 'dextesting'
        const expectedDataStreamRoute = 'testevent1'

        const uploadResponse = await gql.getUploads({
            dataStreamId: expectedDataStreamId,
            dataStreamRoute: expectedDataStreamRoute,
            pageSize: expectedPageSize,
            pageNumber: expectedPageNumber
        })
        
        expect(uploadResponse.getUploads.items.length).toEqual(1)
        expect(uploadResponse.getUploads.items[0].uploadId).not.toBeNull()
        expect(uploadResponse.getUploads.items[0].uploadId).not.toBeUndefined()
        const expectedUploadId = await uploadResponse.getUploads.items[0].uploadId!
        
        const reportResult = await gql.getReports({
            uploadId: expectedUploadId,
            reportsSortedBy: "timestamp",
            sortOrder: SortOrder.Ascending
        })
        expect(reportResult.getReports.length).toBeGreaterThanOrEqual(1)

        await reportResult.getReports.forEach(report => {
            expect(report.dataStreamId).toEqual(expectedDataStreamId)
            expect(report.dataStreamRoute).toEqual(expectedDataStreamRoute)
            expect(report.uploadId).toEqual(expectedUploadId)            
        });
    })

    test.only('creates an upload report', async ({ gql }) => {

        const report = {
            report_schema_version: "1.0.0",
            upload_id: "e2b3a950-4c2c-4756-868e-f81ec27f27fb",
            user_id: "test-user2",
            data_stream_id: "dex-testing",
            data_stream_route: "test-event1",
            jurisdiction: "SMOKE1",
            sender_id: "APHL",
            data_producer_id: "smoke-test-data-producer",
            dex_ingest_datetime: "2024-07-10T15:40:10.162+00:00",
            message_metadata: {
                message_uuid: "5a1fff57-2ea1-4a64-81de-aa7f3096a1ce",
                message_hash: "38c2cc0dcc05f2b68c4287040cfcf71",
                aggregation: "SINGLE",
                message_index: 1
            },
            stage_info: {
                service: "UPLOAD API",
                action: "upload-completed",
                version: "0.0.49-SNAPSHOT",
                status: "SUCCESS",
                issues: null,
                start_processing_time: "2024-07-10T15:40:10.162+00:00",
                end_processing_time: "2024-07-10T15:40:10.228+00:00"
            },
            tags: { tag_field1: "value1" },
            data: { data_field1: "value1" },
            content_type: "application/json",
            content: {
                content_schema_name: "upload-completed",
                content_schema_version: "1.0.0",
                status: "SUCCESS"
            }
        }

        const reportResult = await gql.upsertReport({
            action: "create",
            report: report
        })

        console.log(reportResult)
    })
} )

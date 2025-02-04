import { test, expect } from '@fixtures/gql';
import { SortOrder } from '@gql';

test.describe('GraphQL getReports', () => {
    test('returns a report for a known upload id', async ({ gql }) => {
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
} )

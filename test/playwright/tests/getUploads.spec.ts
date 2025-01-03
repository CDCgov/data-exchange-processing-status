import { test, expect } from '@fixtures/gql';

test.describe('GraphQL getUploads', () => {
    test('returns empty stats for a non-existing data stream and route', async ({ gql }) => {
        const expectedPageSize = 5
        const expectedPageNumber = 0

        const res = await gql.getUploads({
            dataStreamId: "XXX",
            dataStreamRoute: "XXX",
            pageSize: expectedPageSize,
            pageNumber: expectedPageNumber
        })
        
        expect(res.getUploads.items).toEqual([])
        expect(res.getUploads.summary.jurisdictions).toEqual([])
        expect(res.getUploads.summary.numberOfPages).toEqual(0)
        expect(res.getUploads.summary.pageNumber).toEqual(expectedPageNumber)
        expect(res.getUploads.summary.pageSize).toEqual(expectedPageSize)
        expect(res.getUploads.summary.senderIds).toEqual([])
        expect(res.getUploads.summary.totalItems).toEqual(0)  
    })
} )

import { test, expect, GraphQLErrorResponse } from '@fixtures/gql';
import { GetUploadsQuery } from '@gql';

test.describe('GraphQL getUploads', () => {
    test('returns empty stats for a non-existing data stream and route', async ({ gql }) => {
        const expectedPageSize = 5
        const expectedPageNumber = 0

        const getUploadResponse = await gql.getUploads({
            dataStreamId: "XXX",
            dataStreamRoute: "XXX",
            pageSize: expectedPageSize,
            pageNumber: expectedPageNumber
        })
        
        expectEmptyUploadsResponse(getUploadResponse, expectedPageSize, expectedPageNumber)
    })

    // multiple uploads for the same data stream and route
    test('returns multiple uploads for the same data stream and route and jurisdiction', async ({ gql, reportHelper, dataGenerator }) => {
        const expectedPageSize = 5
        const expectedPageNumber = 1
        const expectedJurisdiction = "TEST"

        const baseReport = await dataGenerator.createUploadReportStarted();

        const { startedReports } = await reportHelper.createFullCompleteUploads({
            data_stream_id: baseReport.data_stream_id,
            data_stream_route: baseReport.data_stream_route,
            jurisdiction: expectedJurisdiction
        }, 3)

      
        const getUploadResponse = await gql.getUploads({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            pageSize: expectedPageSize,
            pageNumber: expectedPageNumber
        })

        expectUploadsCount(getUploadResponse, startedReports.length);
        expectJurisdictions(getUploadResponse, [expectedJurisdiction]);
    })

    test('returns multiple uploads for the same data stream and route and different jurisdictions', async ({ gql, reportHelper, dataGenerator }) => {
        const expectedPageSize = 5
        const expectedPageNumber = 1

        const baseReport = await dataGenerator.createUploadReportStarted();

        const { startedReports } = await reportHelper.createFullCompleteUploads({
            data_stream_id: baseReport.data_stream_id,
            data_stream_route: baseReport.data_stream_route,
        }, 3)

        const expectedJurisdictions = startedReports.map(report => report.jurisdiction).toSorted((a, b) => a.localeCompare(b))
        const getUploadResponse = await gql.getUploads({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            pageSize: expectedPageSize,
            pageNumber: expectedPageNumber
        })
        
        expectUploadsCount(getUploadResponse, startedReports.length);
        expectJurisdictions(getUploadResponse, expectedJurisdictions);
    })  

    test('paginates items when there are more items than the page size', async ({ gql, reportHelper, dataGenerator }) => {
        const expectedPageSize = 3
        const expectedPageNumber = 1
        const totalItems = expectedPageSize + 1

        const baseReport = await dataGenerator.createUploadReportStarted();

        await reportHelper.createFullCompleteUploads({
            data_stream_id: baseReport.data_stream_id,
            data_stream_route: baseReport.data_stream_route,
        }, totalItems)

        const getUploadsResponsePage1 = await gql.getUploads({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            pageSize: expectedPageSize,
            pageNumber: expectedPageNumber
        })
        expect(getUploadsResponsePage1.getUploads.items.length).toEqual(expectedPageSize)
        expect(getUploadsResponsePage1.getUploads.summary.totalItems).toEqual(totalItems)

        const getUploadsResponsePage2 = await gql.getUploads({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            pageSize: expectedPageSize,
            pageNumber: expectedPageNumber + 1
        })
        expect(getUploadsResponsePage2.getUploads.items.length).toEqual(totalItems - expectedPageSize)
        expect(getUploadsResponsePage2.getUploads.summary.totalItems).toEqual(totalItems)
    })

    const sortOrders = [
        { sortOrder: "ASC", expectedFirst: "UploadComplete", expectedSecond: "Uploading" },
        { sortOrder: "DESC", expectedFirst: "Uploading", expectedSecond: "UploadComplete" },
    ];

    sortOrders.forEach(({ sortOrder, expectedFirst, expectedSecond }) => {
        test(`items are sorted by status (${sortOrder})`, async ({ gql, reportHelper, dataGenerator }) => {
            const expectedPageSize = 5
            const expectedPageNumber = 1

            const baseReport = await dataGenerator.createUploadReportStarted();
            await reportHelper.createFullPendingUpload({
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
            })

            await reportHelper.createFullCompleteUpload({
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
            })

            const getUploadsResponse = await gql.getUploads({
                dataStreamId: baseReport.data_stream_id,
                dataStreamRoute: baseReport.data_stream_route,
                pageSize: expectedPageSize,
                pageNumber: expectedPageNumber,
                sortBy: "status",
                sortOrder,
            });
            expect(getUploadsResponse.getUploads.items[0].status).toEqual(expectedFirst);
            expect(getUploadsResponse.getUploads.items[1].status).toEqual(expectedSecond);
        });
    });

    test('items are filtered by filename when specified', async ({ gql, reportHelper, dataGenerator }) => {
        const expectedPageSize = 5
        const expectedPageNumber = 1
        const baseReport = await dataGenerator.createUploadReportStatus();

        const { statusReports } = await reportHelper.createFullCompleteUploads({
            data_stream_id: baseReport.data_stream_id,
            data_stream_route: baseReport.data_stream_route,
        }, 3)

        const expectedFilename = statusReports[0].content.filename
        
        const getUploadsResponse = await gql.getUploads({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            fileName: expectedFilename,
            pageSize: expectedPageSize,
            pageNumber: expectedPageNumber
        })

        expectSingleUploadWithFilename(getUploadsResponse, expectedFilename);
    })

    test('items are filtered by start date range', async ({ gql, reportHelper, dataGenerator }) => {
        const baseDate = new Date();
        const numReports = 4;
        const expectedReports = numReports - 1;
        const expectedPageSize = 5
        const expectedPageNumber = 1
        const baseReport = dataGenerator.createUploadReportStarted()

        const uploadStartedReports = []
        for (let i = 0; i < numReports; i++) {
            const { startedReport } = await reportHelper.createFullCompleteUpload(
                {
                    data_stream_id: baseReport.data_stream_id,
                    data_stream_route: baseReport.data_stream_route,
                    dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, i))
                }
            );
            uploadStartedReports.push(startedReport)
        }
        
        const result = await gql.getUploads({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            dateStart: dataGenerator.formatDateCompactUTC(baseDate),
            pageSize: expectedPageSize,
            pageNumber: expectedPageNumber
        });

        const expectedUploadIds = uploadStartedReports.slice(1).map(report => report.upload_id).sort((a, b) => a.localeCompare(b));

        expectUploadsByIds(result, expectedUploadIds);
    });

    test('items are filtered by end date range', async ({ gql, reportHelper, dataGenerator }) => {
        const baseDate = new Date();
        const numReports = 4;
        const expectedPageSize = 5
        const expectedPageNumber = 1
        const baseReport = dataGenerator.createUploadReportStarted()

        const uploadStartedReports = []
        for (let i = 0; i < numReports; i++) {
            const { startedReport } = await reportHelper.createFullCompleteUpload(
                {
                    data_stream_id: baseReport.data_stream_id,
                    data_stream_route: baseReport.data_stream_route,
                    dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, -i))
                }
            );
            uploadStartedReports.push(startedReport)
        }
        
        const result = await gql.getUploads({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            dateEnd: dataGenerator.formatDateCompactUTC(baseDate),
            pageSize: expectedPageSize,
            pageNumber: expectedPageNumber
        });

        const expectedUploadIds = uploadStartedReports.slice(1).map(report => report.upload_id).sort((a, b) => a.localeCompare(b));
        expectUploadsByIds(result, expectedUploadIds);
    });

    test('items are filtered by start and end date range', async ({ gql, reportHelper, dataGenerator }) => {
        const baseDate = new Date();
        const numReports = 4;
        const expectedPageSize = 5
        const expectedPageNumber = 1
        const baseReport = dataGenerator.createUploadReportStarted()

        const uploadStartedReports = []
        for (let i = 0; i < numReports; i++) {
            const { startedReport } = await reportHelper.createFullCompleteUpload(
                {
                    data_stream_id: baseReport.data_stream_id,
                    data_stream_route: baseReport.data_stream_route,
                    dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, i))
                }
            );
            uploadStartedReports.push(startedReport)
        }
        
        const result = await gql.getUploads({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            dateStart: dataGenerator.formatDateCompactUTC(baseDate),
            dateEnd: dataGenerator.formatDateCompactUTC(dataGenerator.addDays(baseDate, numReports-1)),
            pageSize: expectedPageSize,
            pageNumber: expectedPageNumber
        });

        const expectedUploadIds = uploadStartedReports.slice(1, -1).map(report => report.upload_id).sort((a, b) => a.localeCompare(b));
        expectUploadsByIds(result, expectedUploadIds);
    });

    const errorCases = [
        { name: "invalid dateStart", args: { dateStart: "invalid" }, snapshot: "invalid-date-start" },
        { name: "invalid dateEnd", args: { dateEnd: "invalid" }, snapshot: "invalid-date-end" },
    ];

    errorCases.forEach(({ name, args, snapshot }) => {
        test(`errors when ${name}`, async ({ gql }) => {
            const result = await gql.getUploads({
                dataStreamId: "test-data-stream-id",
                dataStreamRoute: "test-data-stream-route",
                pageSize: 5,
                pageNumber: 1,
                ...args,
            }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;

            expectGraphQLErrorResponse(result, snapshot);
        });
    });

    test('errors when page number is invalid for a real data stream and route', async ({ gql, reportHelper }) => {
        // this should not be necessary, but it's needed to make the page number error happen
        const baseReport = await reportHelper.createFullCompleteUpload()
        const result = await gql.getUploads({
            // actual dataStreamId and dataStreamRoute are required to make the page number error happen
            dataStreamId: baseReport.startedReport.data_stream_id,
            dataStreamRoute: baseReport.startedReport.data_stream_route,
            pageSize: 5,
            pageNumber: 0
        }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;

        expectGraphQLErrorResponse(result, "invalid-page-number")
    })

    test('errors when page number is invalid for nonexistent data stream and route', async ({ gql }) => {
        const result = await gql.getUploads({
            dataStreamId: "test-data-stream-id",
            dataStreamRoute: "test-data-stream-route",
            pageSize: 5,
            pageNumber: 0
        }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;

        expectGraphQLErrorResponse(result, "invalid-page-number-no-datastream-route")
    })
})


function expectEmptyUploadsResponse(response: GetUploadsQuery, expectedPageSize: number, expectedPageNumber: number) {
    expect(response.getUploads.items).toEqual([]);
    expect(response.getUploads.summary.jurisdictions).toEqual([]);
    expect(response.getUploads.summary.numberOfPages).toEqual(0);
    expect(response.getUploads.summary.pageNumber).toEqual(expectedPageNumber);
    expect(response.getUploads.summary.pageSize).toEqual(expectedPageSize);
    expect(response.getUploads.summary.senderIds).toEqual([]);
    expect(response.getUploads.summary.totalItems).toEqual(0);
  }

function expectUploadsCount(response: GetUploadsQuery, expectedCount: number) {
    expect(response.getUploads.summary.totalItems).toEqual(expectedCount);
    expect(response.getUploads.items.length).toEqual(expectedCount);
}

function expectJurisdictions(response: GetUploadsQuery, expectedJurisdictions: string[]) {
    const sortedActualJurisdictions = response.getUploads.summary.jurisdictions.toSorted((a, b) => a.localeCompare(b));
    const sortedExpectedJurisdictions = expectedJurisdictions.toSorted((a, b) => a.localeCompare(b));
    expect(sortedActualJurisdictions).toEqual(sortedExpectedJurisdictions);
}

function expectSingleUploadWithFilename(response: GetUploadsQuery, expectedFilename: string) {
    expect(response.getUploads.items.length).toEqual(1);
    expect(response.getUploads.items[0].fileName).toEqual(expectedFilename);
}

function expectUploadsByIds(response: GetUploadsQuery, expectedIds: string[]) {
    const actualIds = response.getUploads.items.map(item => item.uploadId).sort();
    expect(response.getUploads.items.length).toEqual(expectedIds.length);
    expect(response.getUploads.summary.totalItems).toEqual(expectedIds.length);
    const sortedActualIds = actualIds.toSorted((a, b) => a.localeCompare(b));
    const sortedExpectedIds = expectedIds.toSorted((a, b) => a.localeCompare(b));
    expect(sortedActualIds).toEqual(sortedExpectedIds);
}

function expectGraphQLErrorResponse(result: GraphQLErrorResponse, snapshotName: string) {
    expect(result.errors).toBeDefined();
    expect(JSON.stringify(result)).toMatchSnapshot(snapshotName);
}

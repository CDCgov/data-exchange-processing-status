import dataGenerator from '@fixtures/dataGenerator';
import { test, expect, GraphQLErrorResponse } from '@fixtures/gql';

test.describe('GraphQL getUploadStats', () => {
    test('returns empty stats for a non-existing data stream and route', async ({ gql }) => {
        const result = await gql.getUploadStats({
            dataStreamId: "nonexisting",
            dataStreamRoute: "nonexisting",
            daysInterval: 1
        });
        expect(JSON.stringify(result.getUploadStats)).toMatchSnapshot("empty-stats-non-existing-data-stream-and-route");
    });

    test.skip('returns an error for a request with no date range or interval', async ({ gql }) => {
        const result = await gql.getUploadStats({
            dataStreamId: "nonexisting",
            dataStreamRoute: "nonexisting"
        }, {failOnEmptyData: false}) as unknown as GraphQLErrorResponse;
        expect(JSON.stringify(result.errors)).toMatchSnapshot("error-no-date-range-or-interval");
    });

    test('returns stats for multiple completed upload reports', async ({ gql, reportHelper }) => {
        const { startedReport } = await reportHelper.createUploadStartedReport();
        await reportHelper.createUploadCompleteReport(startedReport);
        await reportHelper.createUploadCompleteReport(startedReport);
        await reportHelper.createUploadCompleteReport(startedReport);

        const result = await gql.getUploadStats({
            dataStreamId: startedReport.data_stream_id,
            dataStreamRoute: startedReport.data_stream_route,
            daysInterval: 1
        })
 
        expect(result.getUploadStats.completedUploadsCount).toBe(3);
        expect(JSON.stringify(result.getUploadStats)).toMatchSnapshot("multiple-completed-upload-reports");
    });

    test('returns stats for multiple unique upload reports for the same data stream and route', async ({ gql, reportHelper }) => {
        const { startedReport } = await reportHelper.createUploadStartedReport();
        await reportHelper.createUploadStartedReport({data_stream_id: startedReport.data_stream_id, data_stream_route: startedReport.data_stream_route});
        await reportHelper.createUploadStartedReport({data_stream_id: startedReport.data_stream_id, data_stream_route: startedReport.data_stream_route});

        const result = await gql.getUploadStats({
            dataStreamId: startedReport.data_stream_id,
            dataStreamRoute: startedReport.data_stream_route,
            daysInterval: 1
        })
        expect(result.getUploadStats.uniqueUploadIdsCount).toBe(3);
        expect(JSON.stringify(result.getUploadStats)).toMatchSnapshot("stats-for-multiple-unique-upload-reports");
    });

    test('returns stats for multiple pending upload reports for the same data stream and route', async ({ gql, reportHelper }) => {
        const numReports = 3;
        const startedReports: any[] = [];
        const statusReports: any[] = [];
        const metadataVerifyReports: any[] = [];

        const baseReport = dataGenerator.createUploadReportStarted()
        
        for (let i = 0; i < numReports; i++) {
            const { startedReport, statusReport, metadataVerifyReport } = await reportHelper.createFullPendingUpload({
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
            });

            startedReports.push(startedReport);
            statusReports.push(statusReport);
            metadataVerifyReports.push(metadataVerifyReport);
        }
        const startedReport1 = startedReports[0]; // Keep reference to first report for later use

        const result = await gql.getUploadStats({
            dataStreamId: startedReport1.data_stream_id,
            dataStreamRoute: startedReport1.data_stream_route,
            daysInterval: 1
        })
        expect(result.getUploadStats.uniqueUploadIdsCount).toBe(3);
        expect(result.getUploadStats.pendingUploads.totalCount).toBe(3);
        expect(result.getUploadStats.pendingUploads.pendingUploads.length).toBe(3);

        await reportHelper.validatePendingUploadsBlock(result.getUploadStats.pendingUploads, metadataVerifyReports)

        const cleanedResult = {
            ...result.getUploadStats,
            pendingUploads: {
                ...result.getUploadStats.pendingUploads,
                pendingUploads: []
            }
        };
        expect(JSON.stringify(cleanedResult)).toMatchSnapshot("stats-for-multiple-pending-upload-reports");
    });

    test('returns stats with start date range filtering', async ({ gql, reportHelper, dataGenerator }) => {
        const baseDate = new Date();
        const numReports = 3;
        const expectedReports = numReports - 1;
        const startedReports: any[] = [];

        const baseReport = dataGenerator.createUploadReportStarted()
        for (let i = 0; i < numReports; i++) {
            const { startedReport } = await reportHelper.createFullCompleteUpload(
                {
                    data_stream_id: baseReport.data_stream_id,
                    data_stream_route: baseReport.data_stream_route,
                    dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, i))
                }
            );
            startedReports.push(startedReport);
        }
        const result = await gql.getUploadStats({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            dateStart: dataGenerator.formatDateCompactUTC(dataGenerator.addDays(baseDate, 1))
        });

        expect(result.getUploadStats.uniqueUploadIdsCount).toBe(expectedReports);
        expect(result.getUploadStats.completedUploadsCount).toBe(expectedReports);
        expect(result.getUploadStats.uploadsWithStatusCount).toBe(expectedReports);
        expect(JSON.stringify(result)).toMatchSnapshot("stats-with-start-date-range-filtering");
    });

    test('returns stats with start and end date range filtering', async ({ gql, reportHelper, dataGenerator }) => {
        // create 4 reports, but expect 2 to be returned
        const baseDate = new Date();
        const numReports = 4;
        const expectedReports = numReports - 2;
        const startedReports: any[] = [];

        const baseReport = dataGenerator.createUploadReportStarted()
        for (let i = 0; i < numReports; i++) {
            const { startedReport } = await reportHelper.createFullCompleteUpload(
                {
                    data_stream_id: baseReport.data_stream_id,
                    data_stream_route: baseReport.data_stream_route,
                    dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, i))
                }
            );
            startedReports.push(startedReport);
        }
        const result = await gql.getUploadStats({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            // take off the first day and last day to get the middle two reports
            dateStart: dataGenerator.formatDateCompactUTC(dataGenerator.addDays(baseDate, 1)),
            dateEnd: dataGenerator.formatDateCompactUTC(dataGenerator.addDays(baseDate, numReports-1))
        });
        
        expect(result.getUploadStats.uniqueUploadIdsCount).toBe(expectedReports);
        expect(result.getUploadStats.completedUploadsCount).toBe(expectedReports);
        expect(result.getUploadStats.uploadsWithStatusCount).toBe(expectedReports);
        expect(JSON.stringify(result)).toMatchSnapshot("stats-with-start-and-end-date-range-filtering");
        
    });

    test('returns stats with interval date range filtering', async ({ gql, reportHelper, dataGenerator }) => {
        const baseDate = new Date();
        const numReports = 3;
        const startedReports: any[] = [];

        const baseReport = dataGenerator.createUploadReportStarted()
        
        for (let i = 0; i < numReports+1; i++) {
            const { startedReport } = await reportHelper.createFullCompleteUpload({
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
                dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, i-1))
            });

            startedReports.push(startedReport);
        }

        const initialReport = startedReports[0];

        const result = await gql.getUploadStats({
            dataStreamId: initialReport.data_stream_id,
            dataStreamRoute: initialReport.data_stream_route,
            daysInterval: 0
        });
        
        expect(result.getUploadStats.uniqueUploadIdsCount).toBe(numReports);
        expect(result.getUploadStats.completedUploadsCount).toBe(numReports);
        expect(JSON.stringify(result)).toMatchSnapshot("stats-with-interval-date-range-filtering");
    });

    test('returns stats for bad metadata count', async ({ gql, reportHelper }) => {
        const numReports = 3;
        const metadataVerifyReports: any[] = [];

        const baseReport = dataGenerator.createUploadReportStarted()

        for (let i = 0; i < numReports; i++) {
            const { metadataVerifyReport } = await reportHelper.createUploadMetadataVerifyReportWithIssues({
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
            });
            metadataVerifyReports.push(metadataVerifyReport);
        }

        const result = await gql.getUploadStats({
            dataStreamId: metadataVerifyReports[0].data_stream_id,
            dataStreamRoute: metadataVerifyReports[0].data_stream_route,
            daysInterval: 0 
        });

        expect(result.getUploadStats.badMetadataCount).toBe(numReports);
        await reportHelper.validatePendingUploadsBlock(result.getUploadStats.pendingUploads, metadataVerifyReports)

        const cleanedResult = {
            ...result.getUploadStats,
            pendingUploads: {
                ...result.getUploadStats.pendingUploads,
                pendingUploads: []
            }
        };
        expect(JSON.stringify(cleanedResult)).toMatchSnapshot("stats-for-bad-metadata-count");
    });

    // TODO: This test is not working as expected. Filename is not returned correctly and is always null.
    test('returns stats for duplicate filenames ', async ({ gql, reportHelper }) => {
        const numReports = 3;
        const metadataVerifyReports: any[] = [];
        const baseReport: any = dataGenerator.createUploadReportStarted()
        const expectedFilename = "test.txt"
        const expectedFilename2 = "test2.txt"

        for (let i = 0; i < numReports; i++) {
            const { metadataVerifyReport } = await reportHelper.createUploadMetadataVerifyReport({
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
                content: {
                    ...baseReport.content,
                    filename: expectedFilename,
                    metadata: {
                        ...baseReport.content.metadata,
                        meta_ext_filename: expectedFilename,
                        received_filename: expectedFilename
                    }
                }
            });
            metadataVerifyReports.push(metadataVerifyReport);
            console.log(JSON.stringify(metadataVerifyReport, null, 2))
        }


        // for (let i = 0; i < numReports; i++) {
        //     const { metadataVerifyReport } = await reportHelper.createUploadMetadataVerifyReport({
        //         data_stream_id: baseReport.data_stream_id,
        //         data_stream_route: baseReport.data_stream_route,
        //         content: {
        //             ...baseReport.content,
        //             metadata: {
        //                 ...baseReport.content.metadata,
        //                 received_filename: expectedFilename2
        //             }
        //         }
        //     });
        //     metadataVerifyReports.push(metadataVerifyReport);
        // }

        const initialReport = metadataVerifyReports[0];

        const result = await gql.getUploadStats({
            dataStreamId: initialReport.data_stream_id,
            dataStreamRoute: initialReport.data_stream_route,
            daysInterval: 3
        });

        console.log(JSON.stringify(result, null, 2))
        expect(result.getUploadStats.duplicateFilenames.length).toBe(1);
        expect(result.getUploadStats.duplicateFilenames[0].totalCount).toBe(numReports);
        expect(result.getUploadStats.duplicateFilenames[0].filename).toBe(expectedFilename);
        //expect(JSON.stringify(result)).toMatchSnapshot("stats-for-duplicate-filenames");
    });

    test('returns stats for undelivered uploads', async ({ gql, reportHelper }) => {
        const numReports = 3;
        const startedReports: any[] = [];
        const metadataVerifyReports: any[] = [];
        const baseReport = dataGenerator.createUploadReportStarted()
        
        for (let i = 0; i < numReports; i++) {
            const { startedReport, metadataVerifyReport } = await reportHelper.createFullUndeliveredUpload({
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
            });

            startedReports.push(startedReport);
            metadataVerifyReports.push(metadataVerifyReport);
        }

        const initialReport = startedReports[0];

        const result = await gql.getUploadStats({
            dataStreamId: initialReport.data_stream_id,
            dataStreamRoute: initialReport.data_stream_route,
            daysInterval: 0
        });

        expect (result.getUploadStats.completedUploadsCount).toBe(numReports);
        expect (result.getUploadStats.undeliveredUploads.totalCount).toBe(numReports);
        expect (result.getUploadStats.undeliveredUploads.undeliveredUploads.length).toBe(numReports);
        expect(result.getUploadStats.uniqueUploadIdsCount).toBe(numReports);
        expect(result.getUploadStats.uploadsWithStatusCount).toBe(numReports);

        await reportHelper.validateUndeliveredUploadsBlock(result.getUploadStats.undeliveredUploads, metadataVerifyReports)

        const cleanedResult = {
            ...result.getUploadStats,
            undeliveredUploads: {
                ...result.getUploadStats.undeliveredUploads,
                undeliveredUploads: []
            }
        };
        expect(JSON.stringify(cleanedResult)).toMatchSnapshot("stats-for-multiple-undelivered-upload-reports");
    });

    test('handles invalid date start parameter gracefully', async ({ gql }) => {
        const result = await gql.getUploadStats({
            dataStreamId: "test-data-stream-id",
            dataStreamRoute: "test-data-stream-route",
            dateStart: "2024-01-01",
        }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
        
        await expect(JSON.stringify(result.errors)).toMatchSnapshot("error-invalid-date-start-parameter");
    });

    test('handles invalid date end parameter gracefully', async ({ gql }) => {
        const result = await gql.getUploadStats({
            dataStreamId: "test-data-stream-id",
            dataStreamRoute: "test-data-stream-route",
            dateStart: "20240101T000000Z",
            dateEnd: "2024-01-01",
        }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
        
        await expect(JSON.stringify(result.errors)).toMatchSnapshot("error-invalid-date-end-parameter");
    });


});

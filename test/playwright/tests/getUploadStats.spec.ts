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
        
        for (let i = 0; i < numReports; i++) {
            const { startedReport } = await reportHelper.createUploadStartedReport(
                i === 0 ? undefined : {
                    data_stream_id: startedReports[0].data_stream_id,
                    data_stream_route: startedReports[0].data_stream_route
                }
            );
            startedReports.push(startedReport);
            const { statusReport } = await reportHelper.createUploadStatusReport(startedReport);
            statusReports.push(statusReport);
            const { metadataVerifyReport } = await reportHelper.createUploadMetadataVerifyReport(startedReport);
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

        result.getUploadStats.pendingUploads.pendingUploads.forEach(upload => {
            expect(upload).toHaveProperty("uploadId");
            expect(upload).toHaveProperty("filename");
        });

        const actualPendingUploadFiles = result.getUploadStats.pendingUploads.pendingUploads.map(upload => ({
            uploadId: upload.uploadId,
            filename: upload.filename
        }));
        actualPendingUploadFiles.sort((a, b) => (a.uploadId || '').localeCompare(b.uploadId || ''));

        const originalUploadFiles = metadataVerifyReports.map(report => ({
            uploadId: report.upload_id,
            filename: report.content.filename
        }));
        originalUploadFiles.sort((a, b) => (a.uploadId || '').localeCompare(b.uploadId || ''));

        expect(actualPendingUploadFiles.length).toBe(originalUploadFiles.length);
        expect(actualPendingUploadFiles).toEqual(originalUploadFiles);

        const cleanedResult = {
            ...result.getUploadStats,
            pendingUploads: {
                ...result.getUploadStats.pendingUploads,
                pendingUploads: []
            }
        };
        expect(JSON.stringify(cleanedResult)).toMatchSnapshot("stats-for-multiple-pending-upload-reports");
    });

    // TODO: This test is not working as expected. The stats are not being returned correctly with the date range filter.
    test.skip('returns stats with start date range filtering', async ({ gql, reportHelper, dataGenerator }) => {
        const baseDate = new Date();
        const numReports = 3;
        const startedReports: any[] = [];

        const { startedReport: initialReport } = await reportHelper.createUploadStartedReport({
            dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, -1)) // 2024-01-02
        });
        await reportHelper.createUploadCompleteReport(initialReport);

        for (let i = 0; i < numReports; i++) {
            const { startedReport } = await reportHelper.createUploadStartedReport(
                {
                    data_stream_id: initialReport.data_stream_id,
                    data_stream_route: initialReport.data_stream_route,
                    dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, i))
                }
            );
            startedReports.push(startedReport);
        }
        const result = await gql.getUploadStats({
            dataStreamId: initialReport.data_stream_id,
            dataStreamRoute: initialReport.data_stream_route,
            dateStart: dataGenerator.formatDateCompactUTC(baseDate)
        });
        
        expect(result.getUploadStats.uniqueUploadIdsCount).toBe(numReports);
        
    });

    test.skip('returns stats with end date range filtering', async ({ gql }) => {
        // Test that getUploadStats properly filters by startDate and endDate parameters
        // and returns only stats for uploads within the specified date range
    });

    test('returns stats with interval date range filtering', async ({ gql, reportHelper, dataGenerator }) => {
        const baseDate = new Date();
        const numReports = 3;
        const startedReports: any[] = [];

        const { startedReport: initialReport } = await reportHelper.createUploadStartedReport({
            dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, -1)) // 2024-01-02
        });
        await reportHelper.createUploadCompleteReport(initialReport);

        for (let i = 0; i < numReports; i++) {
            const { startedReport } = await reportHelper.createUploadStartedReport(
                {
                    data_stream_id: initialReport.data_stream_id,
                    data_stream_route: initialReport.data_stream_route,
                    dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, i))
                }
            );
            startedReports.push(startedReport);
        }
        const result = await gql.getUploadStats({
            dataStreamId: initialReport.data_stream_id,
            dataStreamRoute: initialReport.data_stream_route,
            daysInterval: 0
        });
        
        expect(result.getUploadStats.uniqueUploadIdsCount).toBe(numReports);
    });

    test.skip('returns aggregated stats correctly', async ({ gql }) => {
        // Test that getUploadStats returns correct aggregated statistics
        // such as total uploads, successful uploads, failed uploads, etc.
    });

    test.skip('handles invalid date parameters gracefully', async ({ gql }) => {
        // Test that getUploadStats handles invalid date formats or ranges
        // and returns appropriate error responses
    });

});

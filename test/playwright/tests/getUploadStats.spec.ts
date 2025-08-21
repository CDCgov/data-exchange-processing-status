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

    test('returns an error for a request with no date range or interval', async ({ gql }) => {
        const result = await gql.getUploadStats({
            dataStreamId: "nonexisting",
            dataStreamRoute: "nonexisting"
        }, { failOnEmptyData: false }) as unknown as GraphQLErrorResponse;
        expect(result.errors[0].message).toContain("Parsing of the input failed");
        expect(result.errors[0].extensions.classification).toBe("ParsingFailureException");
        expect(result.errors[0].path).toStrictEqual(["getUploadStats"]);
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

    test('returns stats for completed upload reports with replace', async ({ gql, reportHelper }) => {
        const { startedReport } = await reportHelper.createUploadStartedReport();
        await reportHelper.replaceUploadCompleteReport(startedReport);
        await reportHelper.replaceUploadCompleteReport(startedReport);
        await reportHelper.replaceUploadCompleteReport(startedReport);

        const result = await gql.getUploadStats({
            dataStreamId: startedReport.data_stream_id,
            dataStreamRoute: startedReport.data_stream_route,
            daysInterval: 1
        })
 
        expect(result.getUploadStats.completedUploadsCount).toBe(1);
        expect(JSON.stringify(result.getUploadStats)).toMatchSnapshot("completed-upload-reports-with-replace");
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

    test('returns stats for multiple pending upload reports for the same data stream and route', async ({ gql, reportHelper, dataGenerator }) => {
        const numReports = 3;
        const baseReport = dataGenerator.createUploadReportStarted()
        
        const { metadataVerifyReports } = await reportHelper.createFullPendingUploads(
            {
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
            }, numReports)

        const result = await gql.getUploadStats({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            daysInterval: 1
        })
        expect(result.getUploadStats.uniqueUploadIdsCount).toBe(3);
        expect(result.getUploadStats.pendingUploads.totalCount).toBe(3);
        expect(result.getUploadStats.pendingUploads.pendingUploads.length).toBe(3);

        await reportHelper.validatePendingUploadsBlock(result.getUploadStats.pendingUploads, metadataVerifyReports)

        const cleanedResult = reportHelper.cleanPendingUploads(result.getUploadStats);
        expect(JSON.stringify(cleanedResult)).toMatchSnapshot("stats-for-multiple-pending-upload-reports");
    });

    test('returns stats with start date range filtering', async ({ gql, reportHelper, dataGenerator }) => {
        const baseDate = new Date();
        const numReports = 3;
        const expectedReports = numReports + 1;

        const baseReport = dataGenerator.createUploadReportStarted()
        await reportHelper.createFullCompleteUpload(
            {
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
                dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, -1))
            })

        await reportHelper.createFullCompleteUploads(
            {
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
                dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(baseDate)
            }, numReports)
    
        await reportHelper.createFullCompleteUpload(
            {
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
                dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, 1))
            })
        

        const result = await gql.getUploadStats({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            dateStart: dataGenerator.formatDateCompactUTC(baseDate)
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
        const baseReport = dataGenerator.createUploadReportStarted()

        for (let i = 0; i < numReports; i++) {
            await reportHelper.createFullCompleteUpload(
                {
                    data_stream_id: baseReport.data_stream_id,
                    data_stream_route: baseReport.data_stream_route,
                    dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, i))
                }
            );
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
        const baseReport = dataGenerator.createUploadReportStarted()
        
        await reportHelper.createFullCompleteUpload(
            {
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
                dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(dataGenerator.addDays(baseDate, -1))
            })

        await reportHelper.createFullCompleteUploads(
            {
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
                dex_ingest_datetime: dataGenerator.getFormattedDexIngestDateTime(baseDate)
            }, numReports)

        const result = await gql.getUploadStats({
            dataStreamId: baseReport.data_stream_id,
            dataStreamRoute: baseReport.data_stream_route,
            daysInterval: 0
        });
        
        expect(result.getUploadStats.uniqueUploadIdsCount).toBe(numReports);
        expect(result.getUploadStats.completedUploadsCount).toBe(numReports);
        expect(JSON.stringify(result)).toMatchSnapshot("stats-with-interval-date-range-filtering");
    });

    test('returns stats for bad metadata count', async ({ gql, reportHelper, dataGenerator }) => {
        const numReports = 3;

        const baseReport = dataGenerator.createUploadReportStarted()

        const { metadataVerifyReports } = await reportHelper.createUploadMetadataVerifyReportsWithIssues(
            {
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
            }, numReports)

        const result = await gql.getUploadStats({
            dataStreamId: metadataVerifyReports[0].data_stream_id,
            dataStreamRoute: metadataVerifyReports[0].data_stream_route,
            daysInterval: 0 
        });

        expect(result.getUploadStats.badMetadataCount).toBe(numReports);
        await reportHelper.validatePendingUploadsBlock(result.getUploadStats.pendingUploads, metadataVerifyReports)
        
        const cleanedResult = reportHelper.cleanPendingUploads(result.getUploadStats);
        expect(JSON.stringify(cleanedResult)).toMatchSnapshot("stats-for-bad-metadata-count");
    });

    test('returns stats for duplicate filenames ', async ({ gql, reportHelper, dataGenerator }) => {
        const numReports = 3;
        const allMetadataVerifyReports: any[] = [];
        const baseReport: any = dataGenerator.createUploadReportStarted()
        const expectedFilename = "test.txt"
        const expectedFilename2 = "test2.txt"

        const { metadataVerifyReports } = await reportHelper.createUploadMetadataVerifyReports(
            {
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
                content: {
                    ...baseReport.content,
                    metadata: {
                        ...baseReport.content.metadata,
                        received_filename: expectedFilename
                    }
                }
            }, numReports)
        allMetadataVerifyReports.push(...metadataVerifyReports)

        const { metadataVerifyReports: metadataVerifyReports2 } = await reportHelper.createUploadMetadataVerifyReports(
            {
                data_stream_id: baseReport.data_stream_id,
                data_stream_route: baseReport.data_stream_route,
                content: {
                    ...baseReport.content,
                    metadata: {
                        ...baseReport.content.metadata,
                        received_filename: expectedFilename2
                    }
                }
            }, numReports)
        allMetadataVerifyReports.push(...metadataVerifyReports2)

        const initialReport = metadataVerifyReports[0];
        const result = await gql.getUploadStats({
            dataStreamId: initialReport.data_stream_id,
            dataStreamRoute: initialReport.data_stream_route,
            daysInterval: 3
        });

        await reportHelper.validateDuplicateFilenamesBlock(result.getUploadStats.duplicateFilenames, allMetadataVerifyReports)
        await reportHelper.validatePendingUploadsBlock(result.getUploadStats.pendingUploads, allMetadataVerifyReports)
     
    });

    test('returns stats for undelivered uploads', async ({ gql, reportHelper, dataGenerator }) => {
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

        const cleanedResult = reportHelper.cleanUndeliveredUploads(result.getUploadStats);
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

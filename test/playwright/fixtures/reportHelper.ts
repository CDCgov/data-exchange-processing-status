import { expect } from '@playwright/test';
import dataGenerator, { UploadReport } from './dataGenerator';
import { GetSubmissionDetailsQuery, SortOrder } from '@gql';

export class ReportHelper {
    constructor(private readonly gql: any) {}
  
    async createUploadCompleteReport(baseReport?: UploadReport) {
        const report = dataGenerator.createUploadReportCompleted(baseReport)
        const createReportResult = await this.gql.upsertReport({
            action: "create",
            report: report
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")
        return { completeReport: report, createCompleteReportResult: createReportResult }
    }

    async replaceUploadCompleteReport(baseReport?: UploadReport) {
        const report = dataGenerator.createUploadReportCompleted(baseReport)
        const createReportResult = await this.gql.upsertReport({
            action: "replace",
            report: report
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")
        return { completeReport: report, createCompleteReportResult: createReportResult }
    }

    async createUploadStartedReport(baseReport?: Partial<UploadReport>) {
        const report = dataGenerator.createUploadReportStarted(baseReport ? dataGenerator.createUploadReport(baseReport) : undefined)
        const createReportResult = await this.gql.upsertReport({
            action: "create",
            report: report
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")
        return { startedReport: report, createStartedReportResult: createReportResult }
    }

    async createUploadStatusReport(baseReport?: UploadReport) {
        const report = dataGenerator.createUploadReportStatus(baseReport)
        const createReportResult = await this.gql.upsertReport({
            action: "create",
            report: report
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")
        return { statusReport: report, createStatusReportResult: createReportResult }
    }

    async createUploadMetadataVerifyReport(baseReport?: Partial<UploadReport>) {
        const report = dataGenerator.createUploadMetadataVerifyReport(baseReport ? dataGenerator.createUploadMetadataVerifyReport(baseReport) : undefined)
        const createReportResult = await this.gql.upsertReport({
            action: "create",
            report: report
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")
        return { metadataVerifyReport: report, createMetadataVerifyReportResult: createReportResult }
    }

    async createUploadMetadataVerifyReports(baseReport?: Partial<UploadReport>, numReports: number = 1) {
        const reports = []
        const createReportResults = []
        for (let i = 0; i < numReports; i++) {
            const { metadataVerifyReport, createMetadataVerifyReportResult }  = await this.createUploadMetadataVerifyReport(baseReport)
            reports.push(metadataVerifyReport)
            createReportResults.push(createMetadataVerifyReportResult)
        }
        return { metadataVerifyReports: reports, createMetadataVerifyReportResults: createReportResults }
    }

    async createUploadMetadataVerifyReportWithIssues(baseReport?: Partial<UploadReport>) {
        const report = dataGenerator.createUploadMetadataVerifyReportWithIssue(baseReport ? dataGenerator.createUploadReport(baseReport) : undefined)
        const createReportResult = await this.gql.upsertReport({
            action: "create",
            report: report
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")
        return { metadataVerifyReport: report, createMetadataVerifyReportResult: createReportResult }
    }

    async createUploadMetadataVerifyReportsWithIssues(baseReport?: Partial<UploadReport>, numReports: number = 1) {
        const reports = []
        const createReportResults = []
        for (let i = 0; i < numReports; i++) {
            const { metadataVerifyReport, createMetadataVerifyReportResult }  = await this.createUploadMetadataVerifyReportWithIssues(baseReport)
            reports.push(metadataVerifyReport)
            createReportResults.push(createMetadataVerifyReportResult)
        }   
        return { metadataVerifyReports: reports, createMetadataVerifyReportResults: createReportResults }
    }

    async createBlobFileCopyReport(baseReport?: UploadReport) {
        const report = dataGenerator.createBlobFileCopyReport(baseReport)
        const createReportResult = await this.gql.upsertReport({
            action: "create",
            report: report
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")
        return { blobFileCopyReport: report, createBlobFileCopyReportResult: createReportResult }
    }

    async createFullPendingUpload(baseReport?: Partial<UploadReport>) { 
        const { startedReport, createStartedReportResult } = await this.createUploadStartedReport(baseReport)
        const { statusReport, createStatusReportResult } = await this.createUploadStatusReport(startedReport)
        const { metadataVerifyReport, createMetadataVerifyReportResult } = await this.createUploadMetadataVerifyReport(startedReport)
        return {
            startedReport,
            statusReport,
            metadataVerifyReport,
            createStartedReportResult,
            createStatusReportResult,
            createMetadataVerifyReportResult,
        }
    }

    async createFullPendingUploads(baseReport?: Partial<UploadReport>, numReports: number = 1) {
        const startedReports: any[] = []
        const statusReports: any[] = []
        const metadataVerifyReports: any[] = []
        const createStartedReportResults: any[] = []
        const createStatusReportResults: any[] = []
        const createMetadataVerifyReportResults: any[] = []
        for (let i = 0; i < numReports; i++) {
            const { startedReport, createStartedReportResult } = await this.createUploadStartedReport(baseReport)
            const { statusReport, createStatusReportResult } = await this.createUploadStatusReport(startedReport)
            const { metadataVerifyReport, createMetadataVerifyReportResult } = await this.createUploadMetadataVerifyReport(startedReport)
            startedReports.push(startedReport)
            statusReports.push(statusReport)
            metadataVerifyReports.push(metadataVerifyReport)
            createStartedReportResults.push(createStartedReportResult)
            createStatusReportResults.push(createStatusReportResult)
            createMetadataVerifyReportResults.push(createMetadataVerifyReportResult)
        }
        return {
            startedReports,
            statusReports,
            metadataVerifyReports,
            createStartedReportResults,
            createStatusReportResults,
            createMetadataVerifyReportResults,
        }
    }

    async createFullCompleteUpload(baseReport?: Partial<UploadReport>) {
        const { startedReport, createStartedReportResult } = await this.createUploadStartedReport(baseReport)
        const { statusReport, createStatusReportResult } = await this.createUploadStatusReport(startedReport)
        const { metadataVerifyReport, createMetadataVerifyReportResult } = await this.createUploadMetadataVerifyReport(startedReport)
        const { completeReport, createCompleteReportResult } = await this.createUploadCompleteReport(startedReport)
        const { blobFileCopyReport, createBlobFileCopyReportResult } = await this.createBlobFileCopyReport(startedReport)
        return {
            startedReport,
            statusReport,
            metadataVerifyReport,
            completeReport,
            blobFileCopyReport,
            createStartedReportResult,
            createStatusReportResult,
            createMetadataVerifyReportResult,
            createCompleteReportResult,
            createBlobFileCopyReportResult,
        }
    }
    async createFullCompleteUploads(baseReport?: Partial<UploadReport>, numReports: number = 1) {
        const startedReports: any[] = []
        const statusReports: any[] = []
        const metadataVerifyReports: any[] = []
        const completeReports: any[] = []
        const blobFileCopyReports: any[] = []
        const createStartedReportResults: any[] = []
        const createStatusReportResults: any[] = []
        const createMetadataVerifyReportResults: any[] = []
        const createCompleteReportResults: any[] = []
        const createBlobFileCopyReportResults: any[] = []

        for (let i = 0; i < numReports; i++) {
            const { startedReport, createStartedReportResult } = await this.createUploadStartedReport(baseReport)
            const { statusReport, createStatusReportResult } = await this.createUploadStatusReport(startedReport)
            const { metadataVerifyReport, createMetadataVerifyReportResult } = await this.createUploadMetadataVerifyReport(startedReport)
            const { completeReport, createCompleteReportResult } = await this.createUploadCompleteReport(startedReport)
            const { blobFileCopyReport, createBlobFileCopyReportResult } = await this.createBlobFileCopyReport(startedReport)
            startedReports.push(startedReport)
            statusReports.push(statusReport)
            metadataVerifyReports.push(metadataVerifyReport)
            completeReports.push(completeReport)
            blobFileCopyReports.push(blobFileCopyReport)
            createStartedReportResults.push(createStartedReportResult)
            createStatusReportResults.push(createStatusReportResult)
            createMetadataVerifyReportResults.push(createMetadataVerifyReportResult)
            createCompleteReportResults.push(createCompleteReportResult)
            createBlobFileCopyReportResults.push(createBlobFileCopyReportResult)
        }
        return {
            startedReports,
            statusReports,
            metadataVerifyReports,
            completeReports,
            blobFileCopyReports,
            createStartedReportResults,
            createStatusReportResults,
            createMetadataVerifyReportResults,
            createCompleteReportResults,
            createBlobFileCopyReportResults,
        }
    }

    async createFullUndeliveredUpload(baseReport?: Partial<UploadReport>) {
        const { startedReport, createStartedReportResult } = await this.createUploadStartedReport(baseReport)
        const { statusReport, createStatusReportResult } = await this.createUploadStatusReport(startedReport)
        const { metadataVerifyReport, createMetadataVerifyReportResult } = await this.createUploadMetadataVerifyReport(startedReport)
        const { completeReport, createCompleteReportResult } = await this.createUploadCompleteReport(startedReport)
        return {
            startedReport,
            statusReport,
            metadataVerifyReport,
            completeReport,
            createStartedReportResult,
            createStatusReportResult,
            createMetadataVerifyReportResult,
            createCompleteReportResult,
        }
    }

    validateSubmissionDetailFields(submissionDetails: GetSubmissionDetailsQuery['getSubmissionDetails'], report: any) {
        expect.soft(submissionDetails.dataProducerId).toBe(report.data_producer_id)
        expect.soft(submissionDetails.dataStreamId).toBe(report.data_stream_id)
        expect.soft(submissionDetails.dataStreamRoute).toBe(report.data_stream_route)
        expect.soft(submissionDetails.filename).toBe(null)
        expect.soft(submissionDetails.jurisdiction).toBe(report.jurisdiction)
        expect.soft(submissionDetails.lastService).toBe(report.stage_info.service)
        expect.soft(submissionDetails.senderId).toBe(report.sender_id)
        expect.soft(submissionDetails.status).toBe("DELIVERED")
        expect.soft(submissionDetails.uploadId).toBe(report.upload_id)
        const expectedStageInfoAction = report.stage_info.action.toUpperCase().replace(/-/g, "_")
        expect.soft(submissionDetails.lastAction).toBe(expectedStageInfoAction)
        expect.soft(submissionDetails.dexIngestDateTime).toBeRecentInSeconds(5)
    }

    validateSubmissionDetailReport(submissionDetailsReports: GetSubmissionDetailsQuery['getSubmissionDetails']['reports'], originalReport: any) {
        expect(submissionDetailsReports).toBeDefined()

        const originalReportAction = originalReport.stage_info.action.toUpperCase().replace(/-/g, '_')

        const foundReport = submissionDetailsReports!.filter(report => report.stageInfo?.action === originalReportAction)
        expect(foundReport.length).toEqual(1)

        const responseReport = foundReport[0]
        expect.soft(new Date(responseReport.timestamp).getTime()).toBeRecentInSeconds(10)
        expect.soft(responseReport.id).toBeDefined()
        expect.soft(responseReport.reportId).toBeDefined()
        expect.soft(responseReport.id).toBe(responseReport.reportId)

        const fieldMappings: FieldMapping[] = [
            { response: 'uploadId', report: 'upload_id' },
            { response: 'dataStreamId', report: 'data_stream_id' },
            { response: 'dataStreamRoute', report: 'data_stream_route' },
            { response: 'jurisdiction', report: 'jurisdiction' },
            { response: 'senderId', report: 'sender_id' },
            { response: 'dataProducerId', report: 'data_producer_id' },
            { response: 'contentType', report: 'content_type' },
            { response: 'dexIngestDateTime', report: 'dex_ingest_datetime',
                transform: (value: string) => new Date(value).toISOString()
            },
            { response: 'messageMetadata.messageUUID', report: 'message_metadata.message_uuid',
                transform: (value: string) => null  // does not populate in response
            },
            { response: 'messageMetadata.messageHash', report: 'message_metadata.message_hash', 
                transform: (value: string) => null  // does not populate in response
            },
            { response: 'messageMetadata.aggregation', report: 'message_metadata.aggregation' },
            { response: 'messageMetadata.messageIndex', report: 'message_metadata.message_index', 
                transform: (value: string) => null  // does not populate in response
            },
            { response: 'stageInfo.service', report: 'stage_info.service' },
            { response: 'stageInfo.action', report: 'stage_info.action',
                transform: (value: string) => value.toUpperCase().replace(/-/g, '_')
            },
            { response: 'stageInfo.service', report: 'stage_info.service' },
            { response: 'stageInfo.status', report: 'stage_info.status' },
            { response: 'stageInfo.version', report: 'stage_info.version' },
            //{ response: 'stageInfo.issues', report: 'stage_info.issues' },
            { response: 'stageInfo.startProcessingTime', report: 'stage_info.start_processing_time',
                transform: (value: string) => new Date(value).toISOString()
            },
            { response: 'stageInfo.endProcessingTime', report: 'stage_info.end_processing_time',
                transform: (value: string) => new Date(value).toISOString()
            },
            { response: 'content.content_schema_name', report: 'content.content_schema_name' },
            { response: 'content.content_schema_version', report: 'content.content_schema_version' },
            { response: 'content.status', report: 'content.status' },
            { response: 'data.dataField1', report: 'data.data_field1' },
            { response: 'tags.tagField1', report: 'tags.tag_field1' },
        ]

        // Validate each mapped field
        fieldMappings.forEach(mapping => {
            const responseValue = getNestedValue(responseReport, mapping.response)
            const originalReportValue = getNestedValue(originalReport, mapping.report)
            const finalTestValue = mapping.transform ? mapping.transform(originalReportValue) : originalReportValue
            expect.soft(responseReport).toHaveProperty(mapping.response)
            expect.soft(responseValue, `Field ${mapping.response} does not match ${mapping.report}`).toBe(finalTestValue)
        })
    }

    async validatePendingUploadsBlock(pendingUploads: any, metadataVerifyReports: any[]) {

        this.validateUploadListBlock(pendingUploads.pendingUploads)

        const actualPendingUploadFiles = this.getUploadFilesFromListBlock(pendingUploads.pendingUploads)      
        const originalUploadFiles = this.getUploadFilesFromMetadataVerifyReports(metadataVerifyReports)
        
        expect(actualPendingUploadFiles.length).toBe(originalUploadFiles.length);
        expect(actualPendingUploadFiles).toEqual(originalUploadFiles);
    }

    async validateUndeliveredUploadsBlock(undeliveredUploads: any, metadataVerifyReports: any[]) {  
        this.validateUploadListBlock(undeliveredUploads.undeliveredUploads)
        const actualUndeliveredUploadFiles = this.getUploadFilesFromListBlock(undeliveredUploads.undeliveredUploads)
        const originalUploadFiles = this.getUploadFilesFromMetadataVerifyReports(metadataVerifyReports)
        
        expect(actualUndeliveredUploadFiles.length).toBe(originalUploadFiles.length);
        expect(actualUndeliveredUploadFiles).toEqual(originalUploadFiles);
    }

    async validateDuplicateFilenamesBlock(actualDuplicateFilenames: any, metadataVerifyReports: any[]) {
        this.validateDuplicateFilesBlock(actualDuplicateFilenames)
        const actualDuplicateUploadFilesSorted = actualDuplicateFilenames.sort((a: any, b: any) => a.filename.localeCompare(b.filename))
        const originalDuplicateFiles = this.getReceivedFilenamesWithCounts(metadataVerifyReports)
        
        expect(actualDuplicateUploadFilesSorted.length).toBe(originalDuplicateFiles.length);
        expect(actualDuplicateUploadFilesSorted).toEqual(originalDuplicateFiles);
    }

    async validateUploadListBlock(uploadList: any) {
        uploadList.forEach((upload: any) => {
            expect(upload).toHaveProperty("uploadId");
            expect(upload).toHaveProperty("filename");
        });
    }

    async validateDuplicateFilesBlock(duplicateFiles: any) {
        duplicateFiles.forEach((duplicate: any) => {
            expect(duplicate).toHaveProperty("filename");
            expect(duplicate).toHaveProperty("totalCount");
        });
    }

    private getUploadFilesFromListBlock(uploadList: any): { uploadId: string; filename: string; }[] {
        const uploadFiles = uploadList.map((upload: { uploadId: any; filename: any; }) => ({
            uploadId: upload.uploadId,
            filename: upload.filename
         }));
        uploadFiles.sort((a: { uploadId: any; }, b: { uploadId: any; }) => (a.uploadId || '').localeCompare(b.uploadId || ''));
        return uploadFiles;
    }

    private getUploadFilesFromMetadataVerifyReports(metadataVerifyReports: any): { uploadId: string; filename: string; }[] {
        const uploadFiles =  metadataVerifyReports.map((report: { upload_id: any; content: { filename: any; }; }) => ({
            uploadId: report.upload_id,
            filename: report.content.filename
        }));
        
        uploadFiles.sort((a: { uploadId: any; }, b: { uploadId: any; }) => (a.uploadId || '').localeCompare(b.uploadId || ''));
        return uploadFiles;
    }

    private getReceivedFilenamesWithCounts(metadataVerifyReports: any[]): { filename: string; totalCount: number }[] {
        const counts: Record<string, number> = {};
        metadataVerifyReports.forEach(report => {
            const filename = report.content?.metadata?.received_filename;
            if (filename) {
                counts[filename] = (counts[filename] || 0) + 1;
            }
        });
        return Object.entries(counts)
            .map(([filename, totalCount]) => ({ filename, totalCount }))
            .sort((a, b) => a.filename.localeCompare(b.filename));
    }

    async getSubmissionDetailsAndValidate(
        uploadId: string,
        sortedBy: string,
        sortOrder: SortOrder,
        expectedReportCount: number = 1
    ): Promise<GetSubmissionDetailsQuery> {
        let submissionDetailsResult: GetSubmissionDetailsQuery;
        await expect(async () => {
            submissionDetailsResult = await this.gql.getSubmissionDetails({
                uploadId,
                reportsSortedBy: sortedBy,
                sortOrder
            })
            expect(submissionDetailsResult.getSubmissionDetails).toBeDefined()
            expect(submissionDetailsResult.getSubmissionDetails.reports).toHaveLength(expectedReportCount)
            
        }).toPass({
            intervals: [1_000, 2_000, 5_000],
        })

        return submissionDetailsResult!;
    }

    /**
     * Cleans only the pendingUploads block by emptying its pendingUploads array.
     * @param {any} stats - The stats object
     * @returns {any} - A copy with cleaned pendingUploads
     */
    cleanPendingUploads(stats: any) {
        if (!stats || typeof stats !== 'object') return stats;
        return {
            ...stats,
            pendingUploads: stats.pendingUploads
                ? { ...stats.pendingUploads, pendingUploads: [] }
                : stats.pendingUploads,
        };
    }

    /**
     * Cleans only the undeliveredUploads block by emptying its undeliveredUploads array.
     * @param {any} stats - The stats object
     * @returns {any} - A copy with cleaned undeliveredUploads
     */
    cleanUndeliveredUploads(stats: any) {
        if (!stats || typeof stats !== 'object') return stats;
        return {
            ...stats,
            undeliveredUploads: stats.undeliveredUploads
                ? { ...stats.undeliveredUploads, undeliveredUploads: [] }
                : stats.undeliveredUploads,
        };
    }

    /**
     * Cleans only the duplicateFilenames block by emptying the array.
     * @param {any} stats - The stats object
     * @returns {any} - A copy with cleaned duplicateFilenames
     */
    cleanDuplicateFilenames(stats: any) {
        if (!stats || typeof stats !== 'object') return stats;
        return {
            ...stats,
            duplicateFilenames: Array.isArray(stats.duplicateFilenames)
                ? []
                : stats.duplicateFilenames,
        };
    }

    /**
     * Cleans the getUploadStats result for snapshotting by emptying the arrays in
     * pendingUploads, undeliveredUploads, and duplicateFilenames blocks.
     *
     * @param {any} stats - The getUploadStats object to clean
     * @returns {any} - A cleaned copy suitable for snapshot comparison
     */
    cleanUploadStatsForSnapshot(stats: any) {
        if (!stats || typeof stats !== 'object') return stats;
        let cleaned = this.cleanPendingUploads(stats);
        cleaned = this.cleanUndeliveredUploads(cleaned);
        cleaned = this.cleanDuplicateFilenames(cleaned);
        return cleaned;
    }
}

interface FieldMapping {
    response: string;
    report: string;
    transform?: (value: any) => any;
}

const getNestedValue = (obj: any, path: string) => {
    return path.split('.').reduce((acc, part) => acc?.[part], obj)
}


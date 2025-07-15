import { expect } from '@playwright/test';
import dataGenerator, { UploadReport } from './dataGenerator';
import { GetSubmissionDetailsQuery, SortOrder } from '@gql';
import { base } from '@faker-js/faker/.';

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

    async createUploadMetadataVerifyReport(baseReport?: UploadReport) {
        const report = dataGenerator.createUploadMetadataVerifyReport(baseReport)
        const createReportResult = await this.gql.upsertReport({
            action: "create",
            report: report
        })
        expect(createReportResult.upsertReport.result).toBe("SUCCESS")
        return { metadataVerifyReport: report, createMetadataVerifyReportResult: createReportResult }
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
            { response: 'content.contentSchemaName', report: 'content.content_schema_name' },
            { response: 'content.contentSchemaVersion', report: 'content.content_schema_version' },
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
}

interface FieldMapping {
    response: string;
    report: string;
    transform?: (value: any) => any;
}

const getNestedValue = (obj: any, path: string) => {
    return path.split('.').reduce((acc, part) => acc?.[part], obj)
}


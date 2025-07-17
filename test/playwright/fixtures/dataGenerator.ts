import { faker } from "@faker-js/faker"
import { NotificationType, WorkflowSubscriptionDeadlineCheckInput, WorkflowSubscriptionForDataStreamsInput, SubscribeEmailMutationVariables, SubscribeWebhookMutationVariables} from '@gql';

export type UploadReport = {
    report_schema_version: string,
    upload_id: string,
    user_id: string,
    data_stream_id: string,
    data_stream_route: string,
    dex_ingest_datetime: string,
    sender_id: string,
    stage_info: StageInfo
    content_type: string,
    message_metadata?: MessageMetadata,
    tags?: object,
    data?: object,
    jurisdiction?: string
    data_producer_id?: string,
    content: ContentUploadCompleted|ContentUploadStarted|ContentUploadStatus|ContentUploadMetadataVerify|ContentBlobFileCopy

}

export type ContentUploadCompleted = {
    content_schema_name: string
    content_schema_version: string
    status: Status
}

export type ContentUploadStarted = {
    content_schema_name: string
    content_schema_version: string
    status: Status
}

export type ContentUploadStatus = {
    content_schema_name: string
    content_schema_version: string
    tguid: string
    offset: number
    size: number
    filename: string
}

export type ContentUploadMetadataVerify = {
    content_schema_name: string
    content_schema_version: string
    filename: string
    metadata: object
}

export type ContentBlobFileCopy = {
    content_schema_name: string
    content_schema_version: string
    file_source_blob_url: string
    file_destination_blob_url: string
    timestamp?: string
}

enum Aggregation {SINGLE="SINGLE", BATCH="BATCH"}
enum Status {SUCCESS="SUCCESS", FAILURE="FAILURE"}
type MessageMetadata = {
    message_uuid: string,
    message_hash: string,
    aggregation: Aggregation,
    message_index: number
}

type StageInfo = {
    service: string
    action: string
    status: Status
    start_processing_time: string
    end_processing_time: string
    version?: string
    issues?: [any]
}

const minimalReport = {
    upload_id: "uuid",
    data_stream_id: "dextesting",
    data_stream_route: "testevent1",
    content_type: "application/json",
    content: {
        content_schema_name: "upload-started",
        content_schema_version: "1.0.0",
        status: "SUCCESS"
    }
}

export function createMinimalReport() {
    let newReport = { ...minimalReport }
    newReport.upload_id = faker.string.uuid()
    return newReport
}

export function createUploadReport(overrides?: Partial<UploadReport>): UploadReport {
    const dexIngestDateTime = randomTime(new Date())

    const defaultReport: UploadReport = {
        report_schema_version: "1.0.0",
        upload_id: faker.string.uuid(),
        user_id: faker.internet.username(),
        data_stream_id: `${faker.word.noun()}-${faker.word.verb()}`,
        data_stream_route: `${faker.word.adjective()}-${faker.word.noun()}`,
        jurisdiction: faker.string.alpha({length: 3, casing: 'upper' }),
        sender_id: `${faker.word.adjective()}-${faker.word.noun()}`,
        data_producer_id: `${faker.word.adjective()}-${faker.word.noun()}`,
        dex_ingest_datetime: getFormattedDexIngestDateTime(dexIngestDateTime),
        tags: { tag_field1: `${faker.word.noun()}.${faker.string.nanoid()}` },
        data: { data_field1: `${faker.word.noun()}.${faker.string.nanoid()}` },
        content_type: "application/json",
        message_metadata: createMessageMetadata(),
        content: createContentUploadStarted(),
        stage_info: createStageInfo(dexIngestDateTime),
    }

    return { ...defaultReport, ...overrides }
}

export function createUploadReportStarted(report?: UploadReport): UploadReport {
    report = report || createUploadReport()
    const newReport: UploadReport = {
        ...report,
        content: {
            ...createContentUploadStarted(),
            ...report.content
        },
        stage_info: {
            ...createStageInfoStarted(),
            ...report.stage_info
        }
    }
    return newReport
}

export function createUploadReportStatus(report?: UploadReport): UploadReport {
    report = report || createUploadReport()
    const newReport: UploadReport = {
        ...report,
        content: createContentUploadStatus(),
        stage_info: createStageInfoStatus()
    }
    return newReport
}

export function createUploadReportCompleted(report?: UploadReport): UploadReport {
    report = report || createUploadReport()
    const newReport: UploadReport = {
        ...report,
        content: createContentUploadCompleted(),
        stage_info: createStageInfoCompleted()
    }
    return newReport
}

export function createUploadMetadataVerifyReport(report?: Partial<UploadReport>): UploadReport {
    const baseReport = report ? { ...createUploadReport(), ...report } : createUploadReport()
    const newReport: UploadReport = {
        ...baseReport,
        content: {
            ...createContentUploadMetadataVerify(baseReport),
        },
        stage_info: {
            ...createStageInfoMetadataVerify(),
        }
            
    }
    return newReport
}

export function createUploadMetadataVerifyReportWithIssue(report?: UploadReport): UploadReport {
    report = report || createUploadReport()
    const newReport: UploadReport = {
        ...report,
        content: createContentUploadMetadataVerify(report),
        stage_info: createStageInfoMetadataVerifyWithIssue()
    }
    return newReport
}
export function createMessageMetadata() : MessageMetadata {    
    const messageMetadata: MessageMetadata = {
        message_uuid: faker.string.uuid(),
        message_hash: faker.string.hexadecimal({ length: 32, prefix: "", casing: "lower" }),
        aggregation: Aggregation.SINGLE,
        message_index: 1
    }
    return messageMetadata
}

export function createStageInfo(date: Date = new Date(), overrides?: Partial<StageInfo>) {
    const defaultStageInfo = createStageInfoStarted(date)
    return { ...defaultStageInfo, ...overrides }
}

export function createStageInfoStarted(date: Date = new Date(), overrides?: Partial<StageInfo>) {
    const defaultStageInfo = {
        service: "UPLOAD API",
        action: "upload-started",
        version: "0.0.49-SNAPSHOT",
        status: Status.SUCCESS,
        start_processing_time: getFormattedDate(addSeconds(date, 10)),
        end_processing_time: getFormattedDate(addSeconds(date, 20))
    }

    return { ...defaultStageInfo, ...overrides }
}

export function createStageInfoStatus(date: Date = new Date(), overrides?: Partial<StageInfo>) {
    const defaultStageInfo = {
        service: "UPLOAD API",
        action: "upload-status",
        version: "0.0.49-SNAPSHOT",
        status: Status.SUCCESS,
        start_processing_time: getFormattedDate(addSeconds(date, 10)),
        end_processing_time: getFormattedDate(addSeconds(date, 20))
    }

    return { ...defaultStageInfo, ...overrides }
}

export function createStageInfoCompleted(date: Date = new Date(), overrides?: Partial<StageInfo>) {
    const defaultStageInfo = {
        service: "UPLOAD API",
        action: "upload-completed",
        version: "0.0.49-SNAPSHOT",
        status: Status.SUCCESS,
        start_processing_time: getFormattedDate(addSeconds(date, 10)),
        end_processing_time: getFormattedDate(addSeconds(date, 20))
    }

    return { ...defaultStageInfo, ...overrides }
}

export function createStageInfoMetadataVerify(date: Date = new Date(), overrides?: Partial<StageInfo>) {
    const defaultStageInfo = {
        service: "UPLOAD API",
        action: "metadata-verify",
        version: "0.0.49-SNAPSHOT",
        status: Status.SUCCESS,
        start_processing_time: getFormattedDate(addSeconds(date, 10)),
        end_processing_time: getFormattedDate(addSeconds(date, 20))
    }

    return { ...defaultStageInfo, ...overrides }
}

export function createStageInfoMetadataVerifyWithIssue(date: Date = new Date(), overrides?: Partial<StageInfo>): StageInfo {
    const defaultStageInfo: StageInfo = {
        service: "UPLOAD API",
        action: "metadata-verify",
        version: "0.0.49-SNAPSHOT",
        status: Status.SUCCESS,
        start_processing_time: getFormattedDate(addSeconds(date, 10)),
        end_processing_time: getFormattedDate(addSeconds(date, 20)),
        issues: [
            {
                level: "ERROR",
                message: "Error message"
            }
        ]
    }

    return { ...defaultStageInfo, ...overrides }
}

export function createStageInfoBlobFileCopy(date: Date = new Date(), overrides?: Partial<StageInfo>) {
    const defaultStageInfo = {
        service: "UPLOAD API",
        action: "blob-file-copy",
        version: "0.0.49-SNAPSHOT",
        status: Status.SUCCESS,
        start_processing_time: getFormattedDate(addSeconds(date, 10)),
        end_processing_time: getFormattedDate(addSeconds(date, 20))
    }

    return { ...defaultStageInfo, ...overrides }
}


export function createStageInfoWithWarning(date: Date = new Date(), overrides?: Partial<StageInfo>) {
    const defaultStageInfo = {
        ...createStageInfo(date),
        issues: [
            {
                level: "WARNING",
                message: "Warning message"
            }
        ]
    }
    return { ...defaultStageInfo, ...overrides }
}

export function createStageInfoWithError(date: Date = new Date(), overrides?: Partial<StageInfo>) {
    const defaultStageInfo = {
        ...createStageInfo(date),
        issues: [
            {
                level: "ERROR",
                message: "Error message"
            }
        ]
    }
    return { ...defaultStageInfo, ...overrides }
}

export function createContentUploadStarted(): ContentUploadStarted {
    const content: ContentUploadStarted = {
        content_schema_name: "upload-started",
        content_schema_version: "1.0.0",
        status: Status.SUCCESS
    }

    return content
}

export function createContentUploadStatus(): ContentUploadStatus {
    const content:ContentUploadStatus = {
        content_schema_name: "upload-status",
        content_schema_version: "1.0.0",
        tguid: faker.string.uuid(),
        offset: 0,
        size: 1024,
        filename: "playwright-test-file"
    }
    return content
}

export function createContentUploadCompleted():ContentUploadCompleted {
    const content:ContentUploadCompleted = {
        content_schema_name: "upload-completed",
        content_schema_version: "1.0.0",
        status: Status.SUCCESS
    }
    return content
}

export function createContentUploadMetadataVerify(report?: UploadReport): ContentUploadMetadataVerify {
    const filename = faker.system.commonFileName('csv')
    const content: ContentUploadMetadataVerify = {
        content_schema_name: "metadata-verify",
        content_schema_version: "1.0.0",
        filename: filename,
        metadata: {
            received_filename: filename,
            ...(report?.content && 'metadata' in report.content ? report.content.metadata : {})
        }
    }
    return content
}

export function createContentBlobFileCopy(): ContentBlobFileCopy {
    const content: ContentBlobFileCopy = {
        content_schema_name: "blob-file-copy",
        content_schema_version: "1.0.0",
        file_source_blob_url: faker.system.filePath(),
        file_destination_blob_url: faker.system.filePath()
    }
    return content
}

export function createBlobFileCopyReport(report?: UploadReport): UploadReport {
    report = report || createUploadReport()
    const newReport: UploadReport = {
        ...report,
        content: createContentBlobFileCopy(),
        stage_info: createStageInfoBlobFileCopy()
    }
    return newReport
}


export function  getFormattedDate(date: Date = new Date()): string {
    // Get timezone offset in minutes and convert to hours:minutes format
    const offset = -date.getTimezoneOffset();
    const sign = offset >= 0 ? "+" : "-";
    const pad = (num: number) => String(Math.floor(Math.abs(num))).padStart(2, "0");
  
    // Format the date as required
    return date.toISOString().replace("Z", "") + `${sign}${pad(offset / 60)}:${pad(offset % 60)}`;
}

export function getFormattedDexIngestDateTime(date: Date): string {
    return date.toISOString().replace(/\.\d{3}Z$/, 'Z')
};
  
function randomTime(date: Date) {
    let randomTime = faker.date.anytime()
    randomTime.setFullYear(date.getFullYear())
    randomTime.setMonth(date.getMonth())
    randomTime.setDate(date.getDate())
    return randomTime
}

function addSeconds(date: Date, seconds: number) {
    const newDate = new Date(date)
    newDate.setSeconds(date.getSeconds() + seconds)
    return newDate
}

export function addDays(date: Date, days: number) {
    const newDate = new Date(date)
    newDate.setDate(date.getDate() + days)
    return newDate
}

export function createSubscriptionInput({
    emailAddresses = [],
    cronSchedule = "0 0 1 12 *",
    dataStreamIds = ["dextesting"],
    dataStreamRoutes = ["testevent1"],
    jurisdictions = ["jurisdiction"],
    notificationType = NotificationType.Email,
    webhookUrl = "",
    sinceDays = 1
}: Partial<WorkflowSubscriptionForDataStreamsInput>
): WorkflowSubscriptionForDataStreamsInput {
    return {
        cronSchedule,
        dataStreamIds,
        dataStreamRoutes,
        jurisdictions,
        emailAddresses,
        notificationType,
        webhookUrl,
        sinceDays,
    };
}

export function createEmailSubscriptionInput({
    dataStreamId = "dextesting",
    dataStreamRoute = "testevent1", 
    jurisdiction = "jurisdiction",
    ruleDescription = "New Rule Description",
    mvelCondition = "true",
    emailAddresses = []
}: Partial<SubscribeEmailMutationVariables>
): SubscribeEmailMutationVariables {
    return {
        dataStreamId,
        dataStreamRoute,
        jurisdiction, 
        ruleDescription,
        mvelCondition,
        emailAddresses,
    };
}

export function createWebhookSubscriptionInput({
    dataStreamId = "dextesting",
    dataStreamRoute = "testevent1",
    jurisdiction = "jurisdiction",
    ruleDescription = "New Rule Description (webhook)",
    mvelCondition = "true",
    webhookUrl = "",
}: Partial<SubscribeWebhookMutationVariables>
): SubscribeWebhookMutationVariables {
    return {
        dataStreamId,
        dataStreamRoute,
        jurisdiction,
        ruleDescription,
        mvelCondition,
        webhookUrl,
    };
}

export function createDeadlineSubscriptionInput({
    emailAddresses = [],
    cronSchedule = "0 0 1 12 *",
    dataStreamId = "dextesting",
    dataStreamRoute = "testevent1",
    deadlineTime = "06:00:00",
    expectedJurisdictions = [],
    notificationType = NotificationType.Email,
    webhookUrl = "",
}: Partial<WorkflowSubscriptionDeadlineCheckInput>
): WorkflowSubscriptionDeadlineCheckInput {  
    return {
        emailAddresses,
        webhookUrl,
        cronSchedule,
        dataStreamId,
        dataStreamRoute,
        expectedJurisdictions,
        deadlineTime,
        notificationType,
    };
}

export function createRandomSchema() { 
 return {
    schemaName: `${faker.word.noun()}-${faker.word.verb()}`,
    schemaVersion: "1.0.0", 
    content: {
        schema: "https://json-schema.org/draft-07/schema",
        id: "https://github.com/cdcent/data-exchange-messages/reports/matt",
        title: `${faker.word.adjective()} ${faker.word.noun()} ${faker.word.verb()}ing ${faker.word.noun()}`,
        type: "object",
        required: [
            "property1",
            "property2"
        ],
        properties: {
            property1: {
                type: "string"
            },
            property2: {
                type: "string",
                enum: [
                    "SUCCESS",
                    "FAILURE"
                ]
            }
        },
        defs: {}
    }
 }
}

export function formatDateCompactUTC(date: Date): string {
    const year = date.getUTCFullYear();
    const month = String(date.getUTCMonth() + 1).padStart(2, '0');
    const day = String(date.getUTCDate()).padStart(2, '0');
    const hours = String(date.getUTCHours()).padStart(2, '0');
    const minutes = String(date.getUTCMinutes()).padStart(2, '0');
    const seconds = String(date.getUTCSeconds()).padStart(2, '0');
    return `${year}${month}${day}T${hours}${minutes}${seconds}Z`;
}

const dataGenerator = {
    addSeconds,
    addDays,
    createMinimalReport,
    randomTime,
    getFormattedDate,
    getFormattedDexIngestDateTime,
    formatDateCompactUTC,
    createUploadReport,
    createStageInfo,   
    createStageInfoWithWarning,
    createStageInfoWithError,
    createMessageMetadata,
    createContentUploadStarted,
    createContentUploadCompleted,
    createContentUploadStatus,
    createContentUploadMetadataVerify,
    createContentBlobFileCopy,
    createUploadReportStarted,
    createUploadReportStatus,
    createUploadReportCompleted,
    createUploadMetadataVerifyReport,
    createUploadMetadataVerifyReportWithIssue,
    createBlobFileCopyReport,
    createStageInfoBlobFileCopy,
    createSubscriptionInput,
    createEmailSubscriptionInput,
    createDeadlineSubscriptionInput,
    createWebhookSubscriptionInput,
    createRandomSchema,
}

export default dataGenerator;

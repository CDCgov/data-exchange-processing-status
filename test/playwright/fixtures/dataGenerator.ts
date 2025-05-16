import { faker } from "@faker-js/faker"
import { NotificationType, WorkflowSubscriptionDeadlineCheckInput, WorkflowSubscriptionForDataStreamsInput} from '@gql';

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
    content: ContentUploadCompleted|ContentUploadStarted|ContentUploadStatus

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

export function createUploadReport(): UploadReport {
    const dexIngestDateTime = randomTime(new Date())

    let newReport: UploadReport = {
        report_schema_version: "1.0.0",
        upload_id: faker.string.uuid(),
        user_id: faker.internet.username(),
        data_stream_id: `${faker.word.noun()}-${faker.word.verb()}`,
        data_stream_route: `${faker.word.adjective()}-${faker.word.noun()}`,
        jurisdiction: faker.string.alpha({length: 3, casing: 'upper' }),
        sender_id: `${faker.word.adjective()}-${faker.word.noun()}`,
        data_producer_id: `${faker.word.adjective()}-${faker.word.noun()}`,
        dex_ingest_datetime: getFormattedDate(dexIngestDateTime),
        message_metadata: createMessageMetadata(),
        tags: { tag_field1: `${faker.word.noun()}.${faker.string.nanoid()}` },
        data: { data_field1: `${faker.word.noun()}.${faker.string.nanoid()}` },
        content_type: "application/json",
        content: createContentUploadStarted(),
        stage_info: createStageInfo(dexIngestDateTime),
    }
    return newReport
}

export function createUploadReportStarted(report?: UploadReport): UploadReport {
    report = report || createUploadReport()

    
    const newReport: UploadReport = {
        ...report,
        content: createContentUploadStarted()
    }
    return newReport
}

export function createUploadReportStatus(report?: UploadReport): UploadReport {
    report = report || createUploadReport()
    const newReport: UploadReport = {
        ...report,
        content: createContentUploadStatus()
    }
    return newReport
}

export function createUploadReportCompleted(report?: UploadReport): UploadReport {
    report = report || createUploadReport()
    const newReport: UploadReport = {
        ...report,
        content: createContentUploadCompleted()
    }
    return newReport
}

export function createMessageMetadata() : MessageMetadata {    
    const messageMetadata: MessageMetadata = {
        message_uuid: faker.string.uuid(),
        message_hash: 'messagehash',
        aggregation: Aggregation.SINGLE,
        message_index: 1
    }
    return messageMetadata
}

export function createStageInfo(date: Date = new Date()) {
    const stage_info =  {
        service: "UPLOAD API",
        action: "upload-completed",
        version: "0.0.49-SNAPSHOT",
        status: Status.SUCCESS,
        start_processing_time: getFormattedDate(addSeconds(date, 10)),
        end_processing_time: getFormattedDate(addSeconds(date, 20))
    }

    return stage_info
}

export function createStageInfoWithWarning(date: Date = new Date()) {
    const stage_info_warn = {
        ...createStageInfo(date),
        issues: [
            {
                level: "WARNING",
                message: "Warning message"
            }
        ]
    }
    return stage_info_warn
}

export function createStageInfoWithError(date: Date = new Date()) {
    const stage_info_error = {
        ...createStageInfo(date),
        issues: [
            {
                level: "ERROR",
                message: "Error message"
            }
        ]
    }
    return stage_info_error
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

function getFormattedDate(date: Date = new Date()): string {
    // Get timezone offset in minutes and convert to hours:minutes format
    const offset = -date.getTimezoneOffset();
    const sign = offset >= 0 ? "+" : "-";
    const pad = (num: number) => String(Math.floor(Math.abs(num))).padStart(2, "0");
  
    // Format the date as required
    return date.toISOString().replace("Z", "") + `${sign}${pad(offset / 60)}:${pad(offset % 60)}`;
}
  
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

export function createSubscriptionInput({
    emailAddresses = [],
    cronSchedule = "0 0 1 12 *",
    dataStreamIds = [],
    dataStreamRoutes = [],
    jurisdictions = [],
    notificationType = NotificationType.Email,
    webhookUrl = "",
    sinceDays = 1
}: {
    emailAddresses?: string[];
    cronSchedule?: string;
    dataStreamIds?: string[];
    dataStreamRoutes?: string[];
    jurisdictions?: string[];
    notificationType?: NotificationType;
    webhookUrl?: string;
    sinceDays?: number;
}): WorkflowSubscriptionForDataStreamsInput {
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

export function createDeadlineSubscriptionInput({
    emailAddresses = [],
    cronSchedule = "0 0 1 12 *",
    dataStreamId = "",
    dataStreamRoute = "",
    deadlineTime = "06:00:00",
    expectedJurisdictions = [],
    notificationType = NotificationType.Email,
    webhookUrl = "",
}: {
    emailAddresses?: string[];
    cronSchedule?: string;
    dataStreamId?: string;
    dataStreamRoute?: string;
    deadlineTime?: string;
    expectedJurisdictions?: string[];
    notificationType?: NotificationType;
    webhookUrl?: string;
}): WorkflowSubscriptionDeadlineCheckInput {
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



const dataGenerator = {
    addSeconds,
    createMinimalReport,
    randomTime,
    getFormattedDate,
    createUploadReport,
    createStageInfo,   
    createStageInfoWithWarning,
    createStageInfoWithError,
    createMessageMetadata,
    createContentUploadStarted,
    createContentUploadCompleted,
    createUploadReportStarted,
    createUploadReportStatus,
    createUploadReportCompleted,
    createSubscriptionInput
}

export default dataGenerator;

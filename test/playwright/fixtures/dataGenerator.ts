import { faker } from "@faker-js/faker"

const baseReport = {
    report_schema_version: "1.0.0",
    upload_id: "uploadID",
    user_id: "test-user",
    data_stream_id: "dextesting",
    data_stream_route: "testevent1",
    jurisdiction: "jurisdiction",
    sender_id: "sender",
    data_producer_id: "dataproducer",
    dex_ingest_datetime: "2025-03-06T14:21:28Z",
    message_metadata: {
        message_uuid: "uuid",
        message_hash: 'messagehash',
        aggregation: "SINGLE",
        message_index: 1
    },
    tags: { tag_field1: "value1" },
    data: { data_field1: "value1" },
    content_type: "application/json",
    content: {
        content_schema_name: "upload-completed",
        content_schema_version: "1.0.0",
        status: "SUCCESS"
    }
}
    

const minimalReport = {
    upload_id: "uuid",
    data_stream_id: "dextesting",
    data_stream_route: "testevent1",
    content_type: "application/json",
    content: {
        content_schema_name: "upload-completed",
        content_schema_version: "1.0.0",
        status: "SUCCESS"
    }
}


export function createMinimalReport() {
    let newReport = { ...minimalReport }
    newReport.upload_id = faker.string.uuid()
    return newReport
}


export function createUploadReport() {
    const dexIngestDateTime = randomTime(new Date())

    let newReport = {
        ...baseReport,
        stage_info: createStageInfo(dexIngestDateTime)
    }
    newReport.upload_id = faker.string.uuid()
    newReport.message_metadata.message_uuid = faker.string.uuid()
    newReport.dex_ingest_datetime = getFormattedDate(dexIngestDateTime)
    newReport.stage_info.start_processing_time = getFormattedDate(addSeconds(dexIngestDateTime, 10))
    newReport.stage_info.end_processing_time = getFormattedDate(addSeconds(dexIngestDateTime, 20))
    return newReport
}

export function createStageInfo(date: Date = new Date()) {
    const stage_info =  {
        service: "UPLOAD API",
        action: "upload-completed",
        version: "0.0.49-SNAPSHOT",
        status: "SUCCESS",
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

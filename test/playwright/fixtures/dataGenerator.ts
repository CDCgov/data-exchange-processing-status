import { faker } from "@faker-js/faker"
import { start } from "repl"

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
    stage_info: {
        service: "UPLOAD API",
        action: "upload-completed",
        version: "0.0.49-SNAPSHOT",
        status: "SUCCESS",
        issues: null,
        start_processing_time: "2024-07-10T15:40:10.162+00:00",
        end_processing_time: "2024-07-10T15:40:10.228+00:00"
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

    let newReport = { ...baseReport }
    newReport.upload_id = faker.string.uuid()
    newReport.message_metadata.message_uuid = faker.string.uuid()
    newReport.dex_ingest_datetime = getFormattedDate(dexIngestDateTime)
    newReport.stage_info.start_processing_time = getFormattedDate(addSeconds(dexIngestDateTime, 10))
    newReport.stage_info.end_processing_time = getFormattedDate(addSeconds(dexIngestDateTime, 20))
    return newReport
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

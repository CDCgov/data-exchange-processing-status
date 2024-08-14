from datetime import datetime

def create_report_msg_from_content(upload_id, dex_ingest_datetime, replace, with_issues, content):
    disposition_type = "ADD"
    if replace:
        disposition_type = "REPLACE"
    status = "SUCCESS"
    issues = "null"
    if with_issues:
        status = "FAILURE"
        issues = '[{"level":"ERROR","message":"First issue"}, {"level":"ERROR","message":"Second issue"}]'
    message = """
{
    "report_schema_version": "1.0.0",
    "upload_id": "%s",
    "user_id": "test-event1",
    "data_stream_id": "dex-testing",
    "data_stream_route": "test-event1",
    "jurisdiction": "SMOKE",
    "sender_id": "APHL",
    "data_producer_id": "smoke-test-data-producer",
    "dex_ingest_datetime": "%s",
    "status": "SUCCESS",
    "disposition_type": "%s",
    "messageMetadata": {
       "type": "object",
       "properties": {
          "message_uuid": {
             "type": "UUID",
             "description": "Unique identifier for the message associated with this report.  Null if not applicable."
          },
          "message_hash": {
             "type": "string",
             "description": "MD5 hash of the message content."
          },
          "aggregation": {
             "type": "string",
             "enum": ["SINGLE", "BATCH"],
             "description": "Enumeration: [SINGLE, BATCH]."
          },
          "message_index": {
             "type": "integer",
             "description": "Index of the message; e.g. row if csv."
          }

       }
    },
    "stage_info": {
       "service": "HL7v2 Pipeline",
       "action": "RECEIVER",
       "version": "0.0.49-SNAPSHOT",
       "status": "%s",
       "issues": %s,
       "start_processing_time": "2024-07-10T15:40:10.162+00:00",
       "end_processing_time": "2024-07-10T15:40:10.228+00:00"
    },
    "tags": {
       "$ref": "test-ref",
       "description": "Optional tag(s) associated with this report."
    },
    "data": {
       "$ref": "keyValueMap",
       "description": "Optional data associated with this report."
    },
    "content_type": "application/json",
    "content": %s
}
""" % (upload_id, dex_ingest_datetime, disposition_type, status, issues, content)
    return message

def create_upload(upload_id, dex_ingest_datetime, offset, size):
    content = """
{
    "content_schema_name":"upload-status",
    "content_schema_version":"1.0.0",
    "tguid":"%s",
    "offset":%d,
    "size":%d,
    "filename":"some_upload1.csv",
    "meta_destination_id":"dex-testing",
    "meta_ext_event":"test-event1",
    "end_time_epoch_millis":1700009141546,
    "start_time_epoch_millis":1700009137234,
    "metadata":{
        "filename":"10MB-test-file",
        "filetype":"text/plain",
        "meta_destination_id":"dex-testing",
        "meta_ext_event":"test-event1",
        "meta_ext_source":"IZGW",
        "meta_ext_sourceversion":"V2022-12-31",
        "meta_ext_entity":"DD2",
        "meta_username":"ygj6@cdc.gov",
        "meta_ext_objectkey":"2b18d70c-8559-11ee-b9d1-0242ac120002",
        "meta_ext_filename":"10MB-test-file",
        "meta_ext_submissionperiod":"1"
    }
}
""" % (upload_id, offset, size)

    return create_report_msg_from_content(upload_id, dex_ingest_datetime, True, False, content)

def create_routing(upload_id, dex_ingest_datetime):
    content = """
{
    "content_schema_name": "blob-file-copy",
    "content_schema_version": "1.0.0",
    "file_source_blob_url": "",
    "file_destination_blob_url": "",
    "timestamp": "%s",
    "result": "success"
}
""" % (datetime.utcnow().replace(microsecond=0).isoformat() + 'Z')

    return create_report_msg_from_content(upload_id, dex_ingest_datetime, False, False, content)

def create_hl7_debatch_report(upload_id, dex_ingest_datetime):
    content = """
{
   "content_schema_name": "hl7v2-debatch",
   "content_schema_version": "1.0.0",
   "report": {
      "ingested_file_path": "https://ocioedemessagesatst.blob.core.windows.net/hl7ingress/dex-routing/dex-smoke-test_319101ba2fd7835983f3257713819f7b",
      "ingested_file_timestamp": "2024-07-10T15:40:09+00:00",
      "ingested_file_size": 10240,
      "received_filename": "dex-smoke-test",
      "supporting_metadata": {
         "meta_ext_source": "test-src",
         "meta_ext_filestatus": "test-file-status",
         "meta_ext_file_timestamp": "test-timestamp",
         "system_provider": "DEX-ROUTING",
         "meta_ext_uploadid": "test-upload-id",
         "meta_ext_objectkey": "test-obj-key",
         "reporting_jurisdiction": "unknown"
      },
      "aggregation": "SINGLE",
      "number_of_messages": 1,
      "number_of_messages_not_propagated": 1,
      "error_messages": [
         {
            "message_uuid": "378d6660-903f-40b8-b48b-80f9d77aa69c",
            "message_index": 1,
            "error_message": "No valid message found."
         }
      ]
   }
}
"""

    return create_report_msg_from_content(upload_id, dex_ingest_datetime, False, False, content)

def create_hl7_validation(upload_id, dex_ingest_datetime, line):
    content = """
{
    "content_schema_name": "hl7v2-structure-validation",
    "content_schema_version": "1.0.0",
    "message_uuid": "",
    "entries": {
        "structure": [
            {
                "line": %d,
                "column": 56,
                "path": "OBX[10]-5[1].2",
                "description": "test is not a valid Number. The format should be: [+|-]digits[.digits]",
                "category": "Format",
                "classification": "Error"
            }
        ],
        "content": [
            {
                "line":%d,
                "column": 99,
                "path": "OBR[1]-7[1].1",
                "description": "DateTimeOrAll0s - If TS.1 (Time) is valued then TS.1 (Time) shall follow the date/time pattern 'YYYYMMDDHHMMSS[.S[S[S[S]]]][+/-ZZZZ]]'.",
                "category": "Constraint Failure",
                "classification": "Error"
            }
        ],
        "value-set": []
    },
    "error-count": {
        "structure": 1,
        "value-set": 0,
        "content": 1
    },
    "warning-count": {
        "structure": 0,
        "value-set": 0,
        "content": 0
    },
    "status": "STRUCTURE_ERRORS"
}
""" % (line, line)

    return create_report_msg_from_content(upload_id, dex_ingest_datetime, False, True, content)


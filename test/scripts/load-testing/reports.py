from datetime import datetime

def create_report_msg_from_content(upload_id,
                                   service,
                                   action,
                                   dex_ingest_datetime,
                                   start_processing_time,
                                   end_processing_time,
                                   replace,
                                   with_issues,
                                   content):
    disposition_type = "ADD"
    if replace:
        disposition_type = "REPLACE"
    status = "SUCCESS"
    issues = "null"
    if with_issues:
        status = "FAILURE"
        issues = '[{"level":"ERROR","message":"First issue"}, {"level":"ERROR","message":"Second issue"}]'
    start_processing_time = start_processing_time.isoformat(timespec='milliseconds')
    end_processing_time = end_processing_time.isoformat(timespec='milliseconds')
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
    "message_metadata": {
       "message_uuid": "5a1fff57-2ea1-4a64-81de-aa7f3096a1ce",
       "message_hash": "38c2cc0dcc05f2b68c4287040cfcf71",
       "aggregation": "SINGLE",
       "message_index": 1
    },
    "stage_info": {
       "service": "%s",
       "action": "%s",
       "version": "0.0.49-SNAPSHOT",
       "status": "%s",
       "issues": %s,
       "start_processing_time": "%s",
       "end_processing_time": "%s"
    },
    "tags": {
       "tag_field1": "value1"
    },
    "data": {
       "data_field1": "value1"
    },
    "content_type": "application/json",
    "content": %s
}
""" % (upload_id, dex_ingest_datetime, disposition_type, service, action, status, issues, start_processing_time, end_processing_time, content)
    return message

def create_metadata_verify(upload_id, dex_ingest_datetime, start_processing_time, end_processing_time):
    content = """
{
    "content_schema_name": "metadata-verify",
    "content_schema_version": "1.0.0",
    "filename": "InterPartner~CELR~WI~AIMSPlatform~Prod~Prod~20240820192844878~STOP~WALLACLB2024082007233923.OBX",
    "metadata": {
        "data_producer_id": "WI",
        "data_stream_id": "dex-testing",
        "data_stream_route": "test-event1",
        "dex_ingest_datetime": "2024-08-21T00:50:06Z",
        "jurisdiction": "SMOKE",
        "meta_data_stream_id": "dex-testing",
        "meta_ext_event": "test-event1",
        "meta_ext_file_timestamp": "1724201406184",
        "meta_ext_filename": "InterPartner~CELR~WI~AIMSPlatform~Prod~Prod~20240820192844878~STOP~WALLACLB2024082007233923.OBX",
        "meta_ext_filestatus": "Succeeded",
        "meta_ext_objectkey": "0b35c128-4838-40f2-868f-55535f2f15f3",
        "meta_ext_source": "AIMS_TRANSPORT",
        "meta_ext_uploadid": "34716790",
        "meta_organization": "WI",
        "meta_username": "APHL",
        "received_filename": "InterPartner~CELR~WI~AIMSPlatform~Prod~Prod~20240820192844878~STOP~WALLACLB2024082007233923.OBX",
        "sender_id": "APHL",
        "upload_id": "%s",
        "version": "2.0"
    }
}
""" % (upload_id)

    return create_report_msg_from_content(
        upload_id,
        "UPLOAD API",
        "metadata-verify",
        dex_ingest_datetime,
        start_processing_time,
        end_processing_time,
        False,
        False,
        content)

def create_upload_started(upload_id, dex_ingest_datetime, start_processing_time, end_processing_time):
    content = """
{
    "content_schema_name": "upload-started",
    "content_schema_version": "1.0.0",
    "status": "SUCCESS"
}
"""
    return create_report_msg_from_content(
        upload_id,
        "UPLOAD API",
        "upload-started",
        dex_ingest_datetime,
        start_processing_time,
        end_processing_time,
        False,
        False,
        content)

def create_upload_completed(upload_id, dex_ingest_datetime, start_processing_time, end_processing_time):
    content = """
{
    "content_schema_name": "upload-completed",
    "content_schema_version": "1.0.0",
    "status": "SUCCESS"
}
"""
    return create_report_msg_from_content(
        upload_id,
        "UPLOAD API",
        "upload-completed",
        dex_ingest_datetime,
        start_processing_time,
        end_processing_time,
        False,
        False,
        content)

def create_upload_status(upload_id, dex_ingest_datetime, start_processing_time, end_processing_time, offset, size):
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

    return create_report_msg_from_content(
        upload_id,
        "UPLOAD API",
        "upload-status",
        dex_ingest_datetime,
        start_processing_time,
        end_processing_time,
        True,
        False,
        content)

def create_routing(upload_id, dex_ingest_datetime, start_processing_time, end_processing_time):
    content = """
{
    "content_schema_name": "blob-file-copy",
    "content_schema_version": "1.0.0",
    "file_source_blob_url": "",
    "file_destination_blob_url": ""
}
"""

    return create_report_msg_from_content(
        upload_id,
        "UPLOAD API",
        "blob-file-copy",
        dex_ingest_datetime,
        start_processing_time,
        end_processing_time,
        False,
        False,
        content)

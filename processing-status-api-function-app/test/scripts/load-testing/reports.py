from datetime import datetime

def create_upload(upload_id, offset, size):    
    content = """
{
    "schema_name":"upload",
    "schema_version":"1.0",
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

    message = """
{
    "upload_id": "%s",
    "destination_id": "dex-testing",
    "event_type": "test-event1",
    "stage_name": "dex-upload",
    "content_type": "json",
    "content": %s,
    "disposition_type": "replace"
}
""" % (upload_id, content)
    return message

def create_routing(upload_id):
    content = """
{
    "schema_name": "dex-routing",
    "schema_version": "1.0",
    "route_source_blob_url": "",
    "route_destination_blob_url": "",
    "timestamp": "%s",
    "result": "success"
}
""" % (datetime.now())

    message = """
{
    "upload_id": "%s",
    "destination_id": "dex-testing",
    "event_type": "test-event1",
    "stage_name": "dex-routing",
    "content_type": "json",
    "content": %s
}
""" % (upload_id, content)
    return message

def create_hl7_validation(upload_id, line):
    content = """
{
    "schema_name": "hl7-validation",
    "schema_version": "1.0",
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
    
    message = """
{
    "upload_id": "%s",
    "destination_id": "dex-testing",
    "event_type": "test-event1",
    "stage_name": "dex-hl7-validation",
    "content_type": "json",
    "content": %s
}
""" % (upload_id, content)
    return message


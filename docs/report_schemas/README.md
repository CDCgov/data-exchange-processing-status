# DEX Report Schemas
The following report schemas are reusable JSON schemas used as reports to the processing status API.  They are sent
either through the processing status service bus or through HTTP calls into the processing status API.

## dex-file-copy
```json
{
  "schema_name": "dex-file-copy",
  "schema_version": "0.0.1",
  "file_source_blob_url": "",
  "file_destination_blob_url": "",
  "timestamp": "",
  "result": "success|failed",
  "error_description": ""
}
```

### Usages

- DEX routing stage
- DEX upload API copy stage - used when bypassing DEX Routing for blob file copying files straight to the EDAV storage account

## dex-hl7v2

### Debatching Report:
```json
{
  "schema_name": "DEX HL7v2 RECEIVER",
  "schema_version": "2.0.0",
  "routing_metadata": {
    "ingested_file_path": "https://ocioedemessagesatst.blob.core.windows.net/hl7ingress/dex-routing/demoDaart.txt",
    "ingested_file_timestamp": "2024-04-24T13:47:58+00:00",
    "ingested_file_size": "8140",
    "data_producer_id": "",
    "jurisdiction": "UNKNOWN",
    "upload_id": "b76f1539-afd3-4729-bad0-bf5d5eed70cd",
    "data_stream_id": "daart",
    "data_stream_route": "hl7_out_recdeb",
    "sender_id": "",
    "received_filename": "",
    "supporting_metadata": {
      "route": "daart",
      "reporting_jurisdiction": "unknown",
      "system_provider": "DEX-ROUTING",
      "message_type": "ELR"
    }
  },
  "stage": {
    "stage_name": "RECEIVER",
    "stage_version": "0.0.44-SNAPSHOT",
    "event_timestamp": "2024-04-24T13:47:58.2119184Z",
    "start_processing_time": "2024-04-24T13:48:13.667+00:00",
    "end_processing_time": "2024-04-24T13:48:15.26+00:00",
    "report": {
      "single_or_batch": "SINGLE",
      "number_of_messages": 1,
      "number_of_messages_not_propagated": 0,
      "error_messages": [<Strings>]
    }
  }
}
```

### Redaction

```json
{
  "schema_name": "DEX HL7v2 - REDACTOR",
  "schema_version": "2.0.0",
  "message_metadata": {
    "message_uuid": "9b15b373-754f-4bf7-82b5-3c4bdf0f4f37",
    "single_or_batch": "SINGLE",
    "message_index": 1,
    "message_hash": "d8dc2b8e477f0083567d9fdd042450cb"
  },
  "routing_metadata": {
    "ingested_file_path": "https://ocioedemessagesatst.blob.core.windows.net/hl7ingress/dex-routing/demoDaart.txt",
    "ingested_file_timestamp": "2024-04-24T14:28:00+00:00",
    "ingested_file_size": "8140",
    "data_producer_id": "",
    "jurisdiction": "UNKNOWN",
    "upload_id": "8273d12b-bb22-4c37-b53b-33858aca2257",
    "data_stream_id": "daart",
    "data_stream_route": "hl7_out_redacted",
    "sender_id": "",
    "received_filename": "",
    "supporting_metadata": {
      "route": "daart",
      "reporting_jurisdiction": "unknown",
      "system_provider": "DEX-ROUTING",
      "message_type": "ELR"
    }
  },
  "stage": {
    "report": {
      "entries": [
         {
            "path": "PID-3.1",
            "rule": "Redacted PID-3.1 with value 'REDACTED' when PID-3.5 !IN (PT;PI;MB;PN;SR;PHC;PH;AN)",
            "lineNumber": 2
        }
      ],
      "status": "SUCCESS"
    },
    "stage_name": "REDACTOR",
    "stage_version": "0.0.43-SNAPSHOT",
    "status": "SUCCESS",
    "configs": [
      "DEFAULT-config.txt"
    ],
    "event_timestamp": "2024-04-24T14:28:24.144",
    "eventhub_offset": 287762808832,
    "eventhub_sequence_number": 63757,
    "start_processing_time": "2024-04-24T14:37:42.143+00:00",
    "end_processing_time": "2024-04-24T14:37:42.42+00:00"
  },
  "summary": {
    "current_status": "REDACTED",
    "problem": null
  }
}
```

### Validation Report
```json
{
  "schema_name": "DEX HL7v2 - STRUCTURE_VALIDATOR",
  "schema_version": "2.0.0",
  "message_metadata": {
    "message_uuid": "ea512d49-68ee-4a80-a3b6-f73acb75b0f1",
    "single_or_batch": "SINGLE",
    "message_index": 1,
    "message_hash": "48b31c99340261603f0641c32a70466b"
  },
  "routing_metadata": {
    "ingested_file_path": "https://ocioedemessagesatst.blob.core.windows.net/hl7ingress/demo/AL_COVID19_test15.txt",
    "ingested_file_timestamp": "2024-05-13T15:28:37+00:00",
    "ingested_file_size": "5521",
    "data_producer_id": "",
    "jurisdiction": "UNKNOWN",
    "upload_id": "20240216-01-75803f57-5fbc-4f99-baea-b5cf82b81d45",
    "data_stream_id": "celr",
    "data_stream_route": "hl7_out_validation_report",
    "sender_id": "",
    "received_filename": "",
    "supporting_metadata": null
  },
  "stage": {
    "report": {
      "entries": {
        "structure": [
          {
            "line": 6,
            "column": 1,
            "path": "OBX[1]",
            "description": "Segment OBX (Observation/Result) has extra children",
            "category": "Extra",
            "classification": "Warning",
            "stackTrace": null,
            "metaData": null
          }
        ],
        "content": [],
        "value-set": []
      },
      "error-count": {
        "structure": 0,
        "value-set": 0,
        "content": 0
      },
      "warning-count": {
        "structure": 14,
        "value-set": 0,
        "content": 0
      },
      "status": "VALID_MESSAGE"
    },
    "stage_name": "STRUCTURE-VALIDATOR",
    "stage_version": "0.0.42-SNAPSHOT",
    "status": "SUCCESS",
    "configs": [
      "CELR-2.5.1"
    ],
    "event_timestamp": "2024-05-13T15:28:41.467",
    "eventhub_offset": 678604832768,
    "eventhub_sequence_number": 250623,
    "start_processing_time": "2024-05-13T15:28:43.382+00:00",
    "end_processing_time": "2024-05-13T15:28:45.089+00:00"
  },
  "summary": {
    "current_status": "VALID_MESSAGE",
    "problem": null
  },
  "content": "TVNIfF5+XCYjfFNUQVJMS..."
}
```

### hl7-json

```json
{
  "schema_name": "DEX HL7v2 - HL7-JSON-LAKE-TRANSFORMER",
  "schema_version": "2.0.0",
  "message_metadata": {
    "message_uuid": "3210bbb6-2deb-40fa-bc99-18b23e9c91ed",
    "single_or_batch": "SINGLE",
    "message_index": 1,
    "message_hash": "9f95ade323b824a458d470130d26444e"
  },
  "routing_metadata": {
    "ingested_file_path": "https://ocioedemessagesatst.blob.core.windows.net/hl7ingress/AR_CELR_testdata.txt",
    "ingested_file_timestamp": "2024-04-22T15:58:59+00:00",
    "ingested_file_size": "1987",
    "data_producer_id": "",
    "jurisdiction": "05",
    "upload_id": "2024034-{{GUID}}",
    "data_stream_id": "celr",
    "data_stream_route": "hl7_out_json",
    "trace_id": "UNKNOWN",
    "span_id": "TracingDisabled",
    "supporting_metadata": {
      "route": "COVID19-ELR",
      "system_provider": "POSTMAN",
      "message_type": "ELR",
      "original_file_timestamp": "2023-12-22T09:40:04.074",
      "orginal_file_name": "localfile.txt"
    }
  },
  "stage": {
    "output": {
      "MSH": {
        "field_separator": "|",
        "encoding_characters": "^~\\&",
        "sending_application": {
          "namespace_id": "CERNER"
        },
        "sending_facility": {
          "namespace_id": "WDL",
          "universal_id": "52D0391886",
          "universal_id_type": "CLIA"
        },
        "receiving_application": {
          "namespace_id": "vCMR"
        },
        "receiving_facility": {
          "namespace_id": "WEDSS"
        },
        "date_time_of_message": "20201230094810",
        "message_type": {
          "message_code": "ORU",
          "trigger_event": "R01"
        },
        "message_control_id": "1112117",
        "processing_id": {
          "processing_id": "P"
        },
        "version_id": {
          "version_id": "2.3.z"
        },
        "children": [
          {
            "PID": {
              "set_id": "1",
              "patient_identifier_list": [
                {
                  "id_number": "Raymond1",
                  "assigning_authority": {
                    "namespace_id": "WDL",
                    "universal_id": "52D0391886",
                    "universal_id_type": "CLIA"
                  },
                  "identifier_type_code": "PI",
                  "assigning_facility": {
                    "namespace_id": "WDL",
                    "universal_id": "52D0391886",
                    "universal_id_type": "CLIA"
                  }
                }
              ]
            }
          },
         
          {
            "OBR": {
              "set_id": "1",
              "place_order_number": {
                "entity_identifier": "Test23z"
              },
              "filler_order_number": {
                "entity_identifier": "23zMultOTHER",
                "namespace_id": "WDL",
                "universal_id": "52D0391886",
                "universal_id_type": "CLIA"
              }
            }
          }
        ]
      }
    },
    "stage_name": "HL7-JSON-LAKE-TRANSFORMER",
    "stage_version": "0.0.40-SNAPSHOT",
    "status": "SUCCESS",
    "configs": [
      "PhinGuideProfile_v2.json"
    ],
    "event_timestamp": "2024-04-22T15:59:29.458",
    "eventhub_offset": 695784701952,
    "eventhub_sequence_number": 303579,
    "start_processing_time": "2024-04-22T15:59:30.172+00:00",
    "end_processing_time": "2024-04-22T15:59:30.767+00:00"
  },
  "summary": {
    "current_status": "HL7-JSON-LAKE-TRANSFORMED",
    "problem": null
  }
}
```

### Usages

- DEX HL7v2 structure validation stage

## dex-metadata-verify

```json
{
  "schema_version": "0.0.1",
  "schema_name": "dex-metadata-verify",
  "filename": "10MB-test-file",
  "timestamp": "",
  "metadata": {
    "filename": "10MB-test-file",
    "filetype": "text/plain",
    "meta_destination_id": "ndlp",
    "meta_ext_event": "routineImmunization",
    "meta_ext_source": "IZGW",
    "meta_ext_sourceversion": "V2022-12-31",
    "meta_ext_entity": "DD2",
    "meta_username": "ygj6@cdc.gov",
    "meta_ext_objectkey": "2b18d70c-8559-11ee-b9d1-0242ac120002",
    "meta_ext_filename": "10MB-test-file",
    "meta_ext_submissionperiod": "1",
    "meta_field2": "value3"
  },
  "issues": [
    "Missing required metadata field, 'meta_field1'.",
    "Metadata field, 'meta_field2' is set to 'value3' and does not contain one of the allowed values: [ 'value1', value2' ]"
  ]
}
```


### Usages

- DEX upload metadata verify stage

## dex-upload-status

```json
{
  "schema_name": "upload",
  "schema_version": "1.0",
  "tguid": "{{uploadId}}",
  "offset": 0,
  "size": 27472691,
  "filename": "some_upload1.csv",
  "meta_destination_id": "ndlp",
  "meta_ext_event": "routineImmunization",
  "end_time_epoch_millis": 1700009141546,
  "start_time_epoch_millis": 1700009137234,
  "metadata": {
    "filename": "10MB-test-file",
    "filetype": "text/plain",
    "meta_destination_id": "ndlp",
    "meta_ext_event": "routineImmunization",
    "meta_ext_source": "IZGW",
    "meta_ext_sourceversion": "V2022-12-31",
    "meta_ext_entity": "DD2",
    "meta_username": "ygj6@cdc.gov",
    "meta_ext_objectkey": "2b18d70c-8559-11ee-b9d1-0242ac120002",
    "meta_ext_filename": "10MB-test-file",
    "meta_ext_submissionperiod": "1"
  }
}
```

### Usages

- DEX upload status stage

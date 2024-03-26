package gov.cdc.ocio.processingstatusapi.integration


data class DexMetadataVersionOne (
    val filename: String,
    val received_filename: String,
    val original_filname: String,
    val meta_destination_id: String,
    val meta_ext_event: String,
    val meta_ext_source: String,
    val reporting_jurisdiction: String
)
data class DexMetadataVersionTwo (
    val version: String,
    val received_filename: String,
    val filetype: String,
    val data_stream_id: String,
    val data_stream_route: String,
    val data_producer_id: String,
    val sender_id: String,
    val jurisdiction: String,
    val supporting_metadata: Any
)
data class DexFileCopy(
    val schema_name: String,
    val schema_version: String,
    val file_source_blob_url: String,
    val file_destination_blob_url: String,
    val timestamp: String,
    val result: String,
    val error_description: String
)

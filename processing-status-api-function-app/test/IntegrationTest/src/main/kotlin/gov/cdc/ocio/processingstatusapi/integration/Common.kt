package gov.cdc.ocio.processingstatusapi.integration

import com.google.gson.annotations.SerializedName


data class DexMetadataVersionOne(
    @SerializedName("filename") val fileName: String,
    @SerializedName("received_filename") val receivedFileName: String,
    @SerializedName("original_filename") val originalFileName: String,
    @SerializedName("meta_destination_id") val metaDestinationId: String,
    @SerializedName("meta_ext_event") val metaExtEvent: String,
    @SerializedName("meta_ext_source") val metaExtSource: String,
    @SerializedName("reporting_jurisdiction") val reportingJurisdiction: String
)

data class DexMetadataVersionTwo(
    val version: String,
    @SerializedName("received_filename") val receivedFilename: String,
    val filetype: String,
    @SerializedName("data_stream_id") val dataStreamId: String,
    @SerializedName("data_stream_route") val dataStreamRoute: String,
    @SerializedName("data_producer_id") val dataProducerId: String,
    @SerializedName("sender_id") val senderId: String,
    val jurisdiction: String,
    @SerializedName("supporting_metadata") val supportingMetadata: Any
)

data class DexFileCopy(
    @SerializedName("schema_name") val schemaName: String,
    @SerializedName("schema_version") val schemaVersion: String,
    @SerializedName("file_source_blob_url") val fileSourceBlobUrl: String,
    @SerializedName("file_destination_blob_url") val fileDestinationBlobUrl: String,
    val timestamp: String,
    val result: String,
    @SerializedName("error_description") val errorDescription: String
)
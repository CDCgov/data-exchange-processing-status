package gov.cdc.ocio.processingstatusapi.models.reports

data class StageCounts(

    var content_schema_name: String? = null,

    var content_schema_version: String? = null,

    var stageName: String? = null,

    var counts: Int? = null,
)

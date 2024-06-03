package gov.cdc.ocio.processingstatusapi.models.reports

data class StageCounts(

    var schema_name: String? = null,

    var schema_version: String? = null,

    var stageName: String? = null,

    var counts: Int? = null,
)

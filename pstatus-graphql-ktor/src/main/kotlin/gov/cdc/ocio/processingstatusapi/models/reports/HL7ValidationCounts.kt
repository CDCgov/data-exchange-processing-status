package gov.cdc.ocio.processingstatusapi.models.reports

data class HL7ValidationCounts(

    var uploadId: String? = null,

    var stageName: String? = null,

    var valid: Double = 0.0,

    var invalid: Double = 0.0
)
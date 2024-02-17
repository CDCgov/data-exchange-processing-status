package gov.cdc.ocio.processingstatusapi.model.reports

data class HL7DebatchCounts(

    var numberOfMessages: Double = 0.0,

    var numberOfMessagesNotPropagated: Double = 0.0
)
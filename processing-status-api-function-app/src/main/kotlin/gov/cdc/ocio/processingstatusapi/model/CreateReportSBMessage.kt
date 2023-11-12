package gov.cdc.ocio.processingstatusapi.model

class CreateReportSBMessage: ServiceBusMessage() {

    val uploadId: String? = null

    val destinationId: String? = null

    val eventType: String? = null
}
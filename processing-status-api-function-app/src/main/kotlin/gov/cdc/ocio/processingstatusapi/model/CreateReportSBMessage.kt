package gov.cdc.ocio.processingstatusapi.model

/**
 * Create a report service bus message.
 *
 * @property uploadId String?
 * @property destinationId String?
 * @property eventType String?
 */
class CreateReportSBMessage: ServiceBusMessage() {

    val uploadId: String? = null

    val destinationId: String? = null

    val eventType: String? = null
}
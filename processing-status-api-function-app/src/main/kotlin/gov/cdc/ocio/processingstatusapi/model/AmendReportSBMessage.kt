package gov.cdc.ocio.processingstatusapi.model

/**
 * Amend an existing report service bus message.
 *
 * @property uploadId String?
 * @property stageName String?
 * @property contentType String?
 * @property content String?
 */
class AmendReportSBMessage: ServiceBusMessage() {

    val uploadId: String? = null

    val stageName: String? = null

    val contentType: String? = null

    val content: String? = null
}
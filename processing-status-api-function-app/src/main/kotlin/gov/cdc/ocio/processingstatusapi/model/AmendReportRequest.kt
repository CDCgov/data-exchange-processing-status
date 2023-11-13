package gov.cdc.ocio.processingstatusapi.model

/**
 * Amend report message that comes in as the JSON body of HTTP requests.
 *
 * @property contentType String?
 * @property content String?
 * @property dispositionType DispositionType
 */
class AmendReportRequest {

    val contentType: String? = null

    val content: String? = null

    val dispositionType: DispositionType = DispositionType.APPEND
}
package gov.cdc.ocio.processingstatusapi.model

import gov.cdc.ocio.processingstatusapi.model.stagereports.SchemaDefinition

/**
 * Amend report message that comes in as the JSON body of HTTP requests.
 *
 * @property contentType String?
 * @property content String?
 * @property schemaDefinition String?
 * @property dispositionType DispositionType
 */
class AmendReportRequest {

    val contentType: String? = null

    val content: String? = null

    val schemaDefinition: SchemaDefinition? = null

    val dispositionType: DispositionType = DispositionType.APPEND
}
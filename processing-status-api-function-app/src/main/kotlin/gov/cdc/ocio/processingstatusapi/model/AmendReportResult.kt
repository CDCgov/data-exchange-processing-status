package gov.cdc.ocio.processingstatusapi.model

/**
 * Contains all the properties for the return response when a report has been successfully amended.
 *
 * @property reportId String
 * @property stageName String
 * @constructor
 */
data class AmendReportResult(val reportId: String, val stageName: String)
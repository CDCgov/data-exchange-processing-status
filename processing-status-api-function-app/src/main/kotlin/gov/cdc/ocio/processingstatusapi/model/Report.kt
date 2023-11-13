package gov.cdc.ocio.processingstatusapi.model

/**
 * Report definition.  A report is an aggregate of smaller reports from each of the stages that make up a service
 * line or processing pipeline.
 *
 * @property id String?
 * @property reportId String?
 * @property uploadId String?
 * @property destinationId String?
 * @property eventType String?
 * @property reports List<StageReport>?
 */
class Report {

    var id : String? = null

    var reportId: String? = null

    var uploadId: String? = null

    var destinationId: String? = null

    var eventType: String? = null

    var reports: List<StageReport>? = null
}

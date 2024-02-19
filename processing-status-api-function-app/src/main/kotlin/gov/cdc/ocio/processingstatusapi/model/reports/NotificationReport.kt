package gov.cdc.ocio.processingstatusapi.model.reports

/**
 * NotificationReport to be sent for Notifications Queue
 *
 * @property uploadId String?
 * @property reportId String?
 * @property destinationId String?
 * @property eventType String?
 * @property stageName String?
 * @property contentType String?
 * @property content String?
 * @property report_origin Source
 */
data class NotificationReport(

    var uploadId: String? = null,

    var reportId: String? = null,

    var destinationId: String? = null,

    var eventType: String? = null,

    var stageName: String? = null,

    var contentType : String? = null,

    var content: String? = null,

    var report_origin: Source
)

enum class Source {

    HTTP,
    SERVICEBUS
}


package gov.cdc.ocio.processingstatusapi.models

/**
 * NotificationReport to be sent for Notifications Queue
 *
 * @property uploadId String?
 * @property reportId String?
 * @property dataStreamId String?
 * @property dataStreamRoute String?
 * @property stageName String?
 * @property contentType String?
 * @property content String?
 * @property report_origin Source
 */
data class NotificationReport(

    var uploadId: String? = null,

    var reportId: String? = null,

    var dataStreamId: String? = null,

    var dataStreamRoute: String? = null,

    var stageName: String? = null,

    var contentType : String? = null,

    var content: String? = null,

    var messageId: String? = null,

    var status: String? = null,

    var report_origin: Source
)

enum class Source {

    HTTP,
    SERVICEBUS
}

